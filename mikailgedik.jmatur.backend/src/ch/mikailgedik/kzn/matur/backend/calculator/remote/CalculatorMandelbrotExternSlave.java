package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.*;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class CalculatorMandelbrotExternSlave implements CalculatorMandelbrot {
    private final SocketAdapter socket;
    private ArrayList<CalculatorUnit> units;
    private final LinkedList<Calculable> calculables;
    private CalculatorUnit.CalculatorConfiguration configuration;
    private Thread socketReader, terminationAwaiting;

    public CalculatorMandelbrotExternSlave(String host, int port) throws IOException {
        socket = new SocketAdapter(new Socket(host, port));
        units = new ArrayList<>();
        calculables = new LinkedList<>();
        setDefaultUnits();

        socketReader = new Thread(() -> {
            try {
                while(true) {
                    Signal signal = socket.readSignal();
                    if(signal instanceof Signal.SignalConfigure) {
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
    }

    private void sigConf(Signal.SignalConfigure conf) {
        this.configuration = conf.configuration;
        this.configuration.setCalculatorMandelbrot(this);
        units.forEach(u -> u.configureAndStart(configuration));

        this.terminationAwaiting = new Thread(() -> {
            try {
                for(CalculatorUnit unit: units) {
                    //TODO
                    unit.awaitTerminationAndCleanup(100000);
                }
                socket.sendDone();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        });
        this.terminationAwaiting.start();
    }

    private void sigCalculable(Signal.SignalCalculable signalCalculable) {
        synchronized (calculables) {
            calculables.add(signalCalculable.calculable);
            calculables.notify();
        }
    }

    @Override
    public boolean accept(Calculable cal, int[] clusterData) {
        try {
            socket.sendResult(cal, clusterData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;//Delete local copy
    }

    @Override
    public boolean accept(Calculable cal, CLDevice device, long address) {
        Cluster c = new Cluster(null, 0);
        c.setDevice(device, address);
        MemMan.copyToRAM(c, configuration.getLogicClusterHeight() * configuration.getLogicClusterWidth());
        try {
            socket.sendResult(cal, c.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;//Delete local copy
    }

    @Override
    public Calculable get() {
        synchronized (calculables) {
            if(calculables.isEmpty()) {
                try {
                    socket.requestNext();
                    calculables.wait();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                return calculables.removeFirst();
            }
        }
        return null;
    }

    private void setDefaultUnits() {
        for(long device: OpenCLHelper.getAllAvailableDevices()) {
            units.add(new CalculatorUnitGPU(device));
        }

        units.add(new CalculatorUnitCPU());
    }
}
