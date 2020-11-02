package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnit;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketAdapter {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public SocketAdapter(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void sendSignalAbort(int calcId) throws IOException {
        synchronized (out) {
            out.writeObject(new Signal.SignalAbort(calcId));
            out.flush();
            System.out.println("Sent abort");
        }
    }

    public void sendConfiguration(CalculatorUnit.CalculatorConfiguration configuration) throws IOException {
        synchronized (out) {
            out.writeObject(new Signal.SignalConfigure(configuration));
            out.flush();
            System.out.println("Sent conf");
        }
    }

    public void sendCalculable(Calculable calculable) throws IOException {
        synchronized (out) {
            out.writeObject(new Signal.SignalCalculable(calculable));
            out.flush();
            System.out.println("Sent calc");
        }
    }

    public void sendResult(Calculable calc, int[] data) throws IOException {
        synchronized (out) {
            out.writeObject(new Signal.SignalResult(new Calculable.CalculableResult(
                    calc.getCalculatorId(), calc.getStartX(), calc.getStartY(), data)));
            out.flush();
            System.out.println("Sent resu");
        }
    }

    public void sendDone() throws IOException {
        synchronized (out) {
        out.writeObject(new Signal.SignalDone());
        out.flush();
        System.out.println("Sent done");
        }

    }

    public void requestNext() throws IOException {
        synchronized (out) {
            out.writeObject(new Signal.SignalGet());
            out.flush();
            System.out.println("Sent next");
        }
    }

    public Signal readSignal() throws IOException {
        synchronized (in) {
            try {
                Signal s = (Signal) in.readObject();
                System.out.println("Read sig: " + s.getClass().getName());
                return s;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
