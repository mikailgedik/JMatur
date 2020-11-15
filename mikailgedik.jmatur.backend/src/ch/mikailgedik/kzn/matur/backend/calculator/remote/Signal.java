package ch.mikailgedik.kzn.matur.backend.calculator.remote;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;

import java.io.Serializable;

public interface Signal extends Serializable {
    //Signals sent by slave
    class SignalResult implements Signal {
        public final Calculable.CalculableResult result;

        public SignalResult(Calculable.CalculableResult result) {
            this.result = result;
        }
    }

    class SignalGet implements Signal {
        public final int amount;

        public SignalGet(int amount) {
            this.amount = amount;
        }

        //Empty
    }

    class SignalDone implements Signal {
        //Empty
    }

    //Signals sent by master
    class SignalInit implements Signal {
        private final CalculatorUnit.Init init;

        public SignalInit(CalculatorUnit.Init init) {
            this.init = init;
        }

        public CalculatorUnit.Init getInit() {
            return init;
        }
    }

    class SignalAbort implements Signal {
        public final int calcId;

        public SignalAbort(int calcId) {
            this.calcId = calcId;
        }
    }

    class SignalCalculable implements Signal {
        public final Calculable[] calculable;

        public SignalCalculable(Calculable[] calculable) {
            this.calculable = calculable;
        }
    }

    class SignalConfigure implements Signal {
        public final CalculatorUnit.CalculatorConfiguration configuration;

        public SignalConfigure(CalculatorUnit.CalculatorConfiguration configuration) {
            this.configuration = configuration;
        }
    }
}
