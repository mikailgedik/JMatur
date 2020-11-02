package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorMandelbrotExternSlave;

import java.io.IOException;

public class SlaveConnector {
    private CalculatorMandelbrotExternSlave calculatorMandelbrotExternSlave;

    public SlaveConnector(String host, int port) throws IOException {
        calculatorMandelbrotExternSlave = new CalculatorMandelbrotExternSlave(host, port);
    }
}
