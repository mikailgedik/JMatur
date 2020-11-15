package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.TimerTask;

public class SocketAdapter {
    private static final int RESULT = 0x0;
    private static final int GET = 0x1;
    private static final int DONE = 0x2;
    private static final int ABORT = 0x4;
    private static final int CALCULABLE = 0x8;
    private static final int CONFIGURE = 0x10;
    private static final int INIT = 0x20;

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    private final Thread outPutThread;
    private final LinkedList<Signal> signals;

    public SocketAdapter(Socket socket) throws IOException {
        this.socket = socket;

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        signals = new LinkedList<>();

        outPutThread = new Thread(() -> {
            try {
                while(true) {
                    Signal s;
                    synchronized (signals) {
                        if(signals.isEmpty()) {
                            signals.wait();
                        }
                        s = signals.pop();
                    }
                    out.writeObject(s);
                    out.flush();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        outPutThread.setDaemon(true);

        outPutThread.start();
    }

    private void sendInitInter(Signal.SignalInit init) throws IOException {
        out.writeInt(INIT);
        out.writeUTF(init.getInit().getClKernelSource());
    }

    private void sendSignalAbortInter(int calcId) throws IOException {
        out.writeInt(ABORT);
        out.writeInt(calcId);
    }

    private void sendConfigurationInter(CalculatorUnit.CalculatorConfiguration configuration) throws IOException {
        out.writeInt(CONFIGURE);
        out.writeInt(configuration.getLogicClusterWidth());
        out.writeInt(configuration.getLogicClusterHeight());
        //Do not write CalculatorMandelbrot field!
    }

    private void sendCalculableInter(Calculable[] calculable) throws IOException {
        out.writeInt(CALCULABLE);
        out.writeInt(calculable.length);
        for (Calculable c : calculable) {
            if (c != null) {
                out.writeInt(c.getCalculatorId());
                out.writeDouble(c.getStartX());
                out.writeDouble(c.getStartY());
                out.writeInt(c.getMaxIterations());
                out.writeDouble(c.getPrecision());
            } else {
                out.writeInt(-1);
            }
        }
    }

    private void sendResultInter(Calculable calculable, int[] data) throws IOException {
        out.writeInt(RESULT);
        out.writeInt(calculable.getCalculatorId());
        out.writeDouble(calculable.getStartX());
        out.writeDouble(calculable.getStartY());
        out.writeInt(calculable.getMaxIterations());
        out.writeDouble(calculable.getPrecision());

        out.writeInt(data.length);
        for (int datum : data) {
            out.writeInt(datum);
        }
    }

    private void sendDoneInter() throws IOException {
        out.writeInt(DONE);
    }

    private void requestNextInter(int amount) throws IOException {
        out.writeInt(GET);
        out.writeInt(amount);
    }

    public void sendSignalInit(CalculatorUnit.Init init) {
        synchronized (signals) {
            signals.add(new Signal.SignalInit(init));
            signals.notify();
        }
    }

    public void sendSignalAbort(int calcId) {
        synchronized (signals) {
            signals.add(new Signal.SignalAbort(calcId));
            signals.notify();
        }
    }

    public void sendConfiguration(CalculatorUnit.CalculatorConfiguration configuration) {
        synchronized (signals) {
            signals.add(new Signal.SignalConfigure(configuration));
            signals.notify();
        }
    }

    public void sendCalculable(Calculable[] calculable) {
        synchronized (signals) {
            signals.add(new Signal.SignalCalculable(calculable));
            signals.notify();
        }
    }

    public void sendResult(Calculable calculable, int[] data) {
        synchronized (signals) {
            signals.add(new Signal.SignalResult(new Calculable.CalculableResult(
                    calculable.getCalculatorId(),
                    calculable.getStartX(),
                    calculable.getStartY(),
                    calculable.getMaxIterations(),
                    calculable.getPrecision(),
                    data)));
            signals.notify();
        }
    }

    public void sendDone() {
        synchronized (signals) {
            signals.add(new Signal.SignalDone());
            signals.notify();
        }
    }

    public void requestNext(int amount) {
        synchronized (signals) {
            signals.add(new Signal.SignalGet(amount));
            signals.notify();
        }
    }

    public Signal readSignal() throws IOException {
        synchronized (in) {
            try {
                return (Signal) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getHostName() {
        return socket.getInetAddress().getHostName();
    }
}
