package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnit;
import org.lwjgl.system.CallbackI;

import java.io.*;
import java.net.Socket;

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

    public SocketAdapter(Socket socket) throws IOException {
        this.socket = socket;

        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void sendSignalAbort(int calcId) throws IOException {
        synchronized (out) {
            out.writeInt(ABORT);
            out.writeInt(calcId);
            out.flush();
            System.out.println("Sent abort");
        }
    }

    public void sendConfiguration(CalculatorUnit.CalculatorConfiguration configuration) throws IOException {
        synchronized (out) {
            out.writeInt(CONFIGURE);
            out.writeInt(configuration.getLogicClusterWidth());
            out.writeInt(configuration.getLogicClusterHeight());
            out.writeInt(configuration.getMaxIterations());
            out.writeDouble(configuration.getPrecision());
            //Do not write CalculatorMandelbrot field!

            out.flush();
            System.out.println("Sent conf");
        }
    }

    public void sendCalculable(Calculable calculable) throws IOException {
        synchronized (out) {
            out.writeInt(CALCULABLE);
            if(calculable != null) {
                out.writeInt(calculable.getCalculatorId());
                out.writeDouble(calculable.getStartX());
                out.writeDouble(calculable.getStartY());
            } else {
                out.writeInt(-1);
            }
            out.flush();
            System.out.println("Sent calc");
        }
    }

    public void sendResult(Calculable calculable, int[] data) throws IOException {
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
            System.out.println("Sent resu");
        }
    }

    public void sendDone() throws IOException {
        synchronized (out) {
            out.writeInt(DONE);
            out.flush();
            System.out.println("Sent done");
        }

    }

    public void requestNext() throws IOException {
        synchronized (out) {
            out.writeInt(GET);
            out.flush();
            System.out.println("Sent next");
        }
    }

    public Signal readSignal() throws IOException {
        synchronized (in) {
            int type = in.readInt();
            System.out.println("Read sig: " + type);

            return switch (type) {
                case RESULT -> getResult();
                case GET -> new Signal.SignalGet();
                case DONE -> new Signal.SignalDone();
                case ABORT -> getAbort();
                case CALCULABLE -> getCalculable();
                case CONFIGURE -> getConfigure();
                default -> throw new IllegalStateException("Unknown type: " + type);
            };
        }
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
        int id = in.readInt();
        if(id != -1) {
            double startX = in.readDouble();
            double startY = in.readDouble();

            System.out.println("Read calc: " + id + " " + startX + " " + startY);

            return new Signal.SignalCalculable(new Calculable(id, startX, startY));
        } else {
            System.out.println("Read calc: null");

            return new Signal.SignalCalculable(null);
        }

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
