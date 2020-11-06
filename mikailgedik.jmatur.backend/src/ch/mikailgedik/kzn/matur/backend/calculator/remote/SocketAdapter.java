package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnit;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Stack;

public class SocketAdapter {
    private static final int RESULT = 0x0;
    private static final int GET = 0x1;
    private static final int DONE = 0x2;
    private static final int ABORT = 0x4;
    private static final int CALCULABLE = 0x8;
    private static final int CONFIGURE = 0x10;

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;


    private final Thread outPutThread;
    private final LinkedList<Signal> signals;


    public SocketAdapter(Socket socket) throws IOException {
        this.socket = socket;

        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

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
                    if(s instanceof Signal.SignalGet) {
                        requestNextInter(((Signal.SignalGet) s).amount);
                    } else if(s instanceof Signal.SignalAbort) {
                        sendSignalAbortInter(((Signal.SignalAbort) s).calcId);
                    } else if(s instanceof Signal.SignalCalculable) {
                        sendCalculableInter(((Signal.SignalCalculable) s).calculable);
                    } else if(s instanceof Signal.SignalConfigure) {
                        sendConfigurationInter(((Signal.SignalConfigure) s).configuration);
                    } else if(s instanceof Signal.SignalDone) {
                        sendDoneInter();
                    } else if(s instanceof Signal.SignalResult) {
                        sendResultInter(((Signal.SignalResult) s).result,
                                ((Signal.SignalResult) s).result.getData());
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        outPutThread.setDaemon(true);
        outPutThread.start();

    }

    public void sendSignalAbortInter(int calcId) throws IOException {
        synchronized (out) {
            out.writeInt(ABORT);
            out.writeInt(calcId);
            out.flush();
            //System.out.println("Sent abort");
        }
    }

    public void sendConfigurationInter(CalculatorUnit.CalculatorConfiguration configuration) throws IOException {
        synchronized (out) {
            out.writeInt(CONFIGURE);
            out.writeInt(configuration.getLogicClusterWidth());
            out.writeInt(configuration.getLogicClusterHeight());
            out.writeInt(configuration.getMaxIterations());
            out.writeDouble(configuration.getPrecision());
            //Do not write CalculatorMandelbrot field!

            out.flush();
            //System.out.println("Sent conf");
        }
    }

    public void sendCalculableInter(Calculable[] calculable) throws IOException {
        synchronized (out) {
            out.writeInt(CALCULABLE);
            out.writeInt(calculable.length);
            for(Calculable c: calculable) {
                if(c != null) {
                    out.writeInt(c.getCalculatorId());
                    out.writeDouble(c.getStartX());
                    out.writeDouble(c.getStartY());
                } else {
                    out.writeInt(-1);
                }
            }
            out.flush();
            //System.out.println("Sent calcs:" + calculable.length);
        }
    }

    public void sendResultInter(Calculable calculable, int[] data) throws IOException {
        synchronized (out) {
            out.writeInt(RESULT);
            out.writeInt(calculable.getCalculatorId());
            out.writeDouble(calculable.getStartX());
            out.writeDouble(calculable.getStartY());
            out.writeInt(data.length);
            for (int datum : data) {
                out.writeInt(datum);
            }

            out.flush();
            //System.out.println("Sent resu");
        }
    }

    public void sendDoneInter() throws IOException {
        synchronized (out) {
            out.writeInt(DONE);
            out.flush();
            //System.out.println("Sent done");
        }

    }

    public void requestNextInter(int amount) throws IOException {
        synchronized (out) {
            out.writeInt(GET);
            out.writeInt(amount);
            out.flush();
            //System.out.println("Sent next");
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
            int type = in.readInt();
            //System.out.println("Read sig: " + type);

            return switch (type) {
                case RESULT -> getResult();
                case GET -> getGet();
                case DONE -> new Signal.SignalDone();
                case ABORT -> getAbort();
                case CALCULABLE -> getCalculable();
                case CONFIGURE -> getConfigure();
                default -> throw new IllegalStateException("Unknown type: " + type);
            };
        }
    }

    private Signal.SignalGet getGet() throws IOException {
        int amount = in.readInt();
        return new Signal.SignalGet(amount);
    }

    private Signal.SignalResult getResult() throws IOException {
        int id = in.readInt();
        double startX = in.readDouble();
        double startY = in.readDouble();
        int[] data = new int[in.readInt()];
        for(int i = 0; i < data.length; i++) {
            data[i] = in.readInt();
        }

        return new Signal.SignalResult(new Calculable.CalculableResult(id, startX, startY, data));
    }

    private Signal.SignalAbort getAbort() throws IOException {
        int id = in.readInt();
        return new Signal.SignalAbort(id);
    }

    private Signal.SignalCalculable getCalculable()  throws IOException {
        int amount = in.readInt();
        Calculable[] calculables = new Calculable[amount];
        for(int i = 0; i < calculables.length; i++) {
            int id = in.readInt();
            if(id != -1) {
                double startX = in.readDouble();
                double startY = in.readDouble();

                //System.out.println("Read calc: " + id + " " + startX + " " + startY);
                calculables[i] = new Calculable(id, startX, startY);
            } else {
                //System.out.println("Read calc: null");
                calculables[i] = null;
            }
        }

        return new Signal.SignalCalculable(calculables);
    }

    private Signal.SignalConfigure getConfigure()  throws IOException {
        int cW = in.readInt();
        int cH = in.readInt();
        int iter = in.readInt();
        double precision = in.readDouble();
        return new Signal.SignalConfigure(
                new CalculatorUnit.CalculatorConfiguration(cW, cH, iter, precision, null));
    }
}
