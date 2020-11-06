package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnit;

import java.io.IOException;

public class CalculatorUnitExternMaster implements CalculatorUnit {
    private final SocketAdapter socket;
    private Thread thread;
    private CalculatorConfiguration configuration;

    public CalculatorUnitExternMaster(SocketAdapter socket) throws IOException {
        this.socket = socket;
    }

    @Override
    public synchronized void configureAndStart(CalculatorConfiguration configuration) {
        this.configuration = configuration;
        try {
            socket.sendConfiguration(configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        thread = new Thread(() -> {
            Signal o;
            try {
                boolean shouldRun = true;
                while(shouldRun) {
                    o = socket.readSignal();
                    if(o instanceof Signal.SignalGet) {
                        sendNext(((Signal.SignalGet) o).amount);
                    } else if (o instanceof Signal.SignalResult) {
                        boolean accepted = configuration.getCalculatorMandelbrot().accept(((Signal.SignalResult) o).result,
                                ((Signal.SignalResult) o).result.getData());
                        System.out.println("Received result " + accepted);
                    } else if (o instanceof Signal.SignalDone) {
                        shouldRun = false;
                    } else {
                        throw new RuntimeException("Unexpected signal + " + o.getClass());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void sendNext(int amount) throws IOException {
        Calculable[] calculables = new Calculable[amount];
        for(int i = 0; i < calculables.length; i++) {
            calculables[i] = configuration.getCalculatorMandelbrot().get();
        }
        socket.sendCalculable(calculables);
    }

    @Override
    public synchronized void awaitTerminationAndCleanup(long maxWaitingTime) throws InterruptedException {
        thread.join(maxWaitingTime);
        if(thread.isAlive()) {
            throw new RuntimeException("Socket has not stopped!");
        }
        thread = null;
        configuration = null;
    }

    @Override
    public void abort(int calcId) {
        try {
            socket.sendSignalAbort(calcId);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
