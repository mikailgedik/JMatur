package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.*;
import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculatorMandelbrotExternSlave implements CalculatorMandelbrot {
    private static final int BUFFER_LOWER_THRESHOLD = 30, BUFFER_UPPER_THRESHOLD = 50;

    private final SocketAdapter socket;
    private ArrayList<CalculatorUnit> units;
    private final LinkedList<Calculable> calculables;
    private CalculatorUnit.CalculatorConfiguration configuration;
    private Thread socketReader, terminationAwaiting;

    private AtomicInteger pendingRequests;

    public CalculatorMandelbrotExternSlave(String host, int port, ArrayList<CalculatorUnit> units) throws IOException {
        socket = new SocketAdapter(new Socket(host, port));
        this.units = units;
        calculables = new LinkedList<>();
        pendingRequests = new AtomicInteger();

        socketReader = new Thread(() -> {
            try {
                while(true) {
                    Signal signal = socket.readSignal();
                    if(signal instanceof Signal.SignalInit) {
                        units.forEach(u -> u.init(((Signal.SignalInit) signal).getInit()));
                    } else if(signal instanceof Signal.SignalConfigure) {
                        sigConf((Signal.SignalConfigure) signal);
                    } else if (signal instanceof Signal.SignalAbort) {
                        sigAbort((Signal.SignalAbort) signal);
                    } else if (signal instanceof Signal.SignalCalculable) {
                        sigCalculable((Signal.SignalCalculable) signal);
                    } else {
                        throw new RuntimeException("Unexpected signal " + signal.getClass().toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketReader.start();
    }

    private void sigAbort(Signal.SignalAbort signalAbort) {
        units.forEach(u -> u.abort(signalAbort.calcId));
        synchronized (this.calculables) {
            this.calculables.removeIf(u -> (u != null && u.getCalculatorId() == signalAbort.calcId));
        }
    }

    private void sigConf(Signal.SignalConfigure conf) {
        this.configuration = conf.configuration;
        this.configuration.setCalculatorMandelbrot(this);
        pendingRequests.set(0);

        units.forEach(u -> u.configureAndStart(configuration));

        this.terminationAwaiting = new Thread(() -> {
            try {
                for(CalculatorUnit unit: units) {
                    //TODO
                    unit.awaitTerminationAndCleanup(100000);
                    System.out.println("Done");
                }
                socket.sendDone();
                calculables.clear();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });
        this.terminationAwaiting.start();
    }

    private void sigCalculable(Signal.SignalCalculable signalCalculable) {
        synchronized (calculables) {
            pendingRequests.addAndGet(-signalCalculable.calculable.length);
            System.out.println("Got new, pending: " + pendingRequests.get());
            assert pendingRequests.get() >= 0;
            calculables.addAll(Arrays.asList(signalCalculable.calculable));
            calculables.notify();
        }
    }

    @Override
    public boolean accept(Calculable cal, int[] clusterData) {
        socket.sendResult(cal, clusterData);
        return false;//Delete local copy
    }

    @Override
    public boolean accept(Calculable cal, CLDevice device, long address) {
        //Create dummy cluster
        Cluster c = new Cluster(null, 0, 0,0);
        c.setDevice(device, address);
        MemMan.copyToRAM(c, configuration.getLogicClusterHeight() * configuration.getLogicClusterWidth());
        socket.sendResult(cal, c.getValue());
        return false;//Delete local copy
    }

    @Override
    public synchronized Calculable get() {
        synchronized (calculables) {
            if(calculables.size() < BUFFER_LOWER_THRESHOLD) {
                try {
                    //Refill buffer TODO
                    int req = BUFFER_UPPER_THRESHOLD - calculables.size();
                    req -= pendingRequests.get();

                    if(req > 0) {
                        socket.requestNext(req);
                        pendingRequests.addAndGet(req);
                    }
                    calculables.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return calculables.removeFirst();
        }
    }
}
