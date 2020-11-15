package ch.mikailgedik.kzn.matur.frontend.console;

import ch.mikailgedik.kzn.matur.backend.connector.Connector;
import ch.mikailgedik.kzn.matur.backend.connector.Constants;

import javax.swing.JFileChooser;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;

@Deprecated
public class ConsoleFrontEnd {
    private Connector connector;
    private DecimalFormat format;

    public ConsoleFrontEnd() {
        this.connector = new Connector();
        format = new DecimalFormat("00000");
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        String line;
        System.out.println("Java version: " + System.getProperty("java.version"));
        //System.out.println("Program version: " + connector.getSetting(Constants.VALUE_PROGRAM_VERSION));
        System.out.println("Input commands");
        ArrayList<String> commands = new ArrayList<>();

        while(!(line = scanner.nextLine()).equals("")) {
            commands.add(line);
        }

        System.out.println("Registered " + commands.size() + " command(s)");
        commands.forEach(this::execute);
        scanner.close();
    }

    private void execute(String command) {
        command = command.trim();
        long t = System.currentTimeMillis();
        System.out.println("#########################\nExecuting: " + command);
        int indexOfSpace = command.indexOf(' ');
        String para;
        if(indexOfSpace > 0) {
            para = command.substring(indexOfSpace + 1);
            command = command.substring(0, indexOfSpace);
        } else {
            para = null;
        }
        /*
        switch (command) {
            case "calculate" -> connector.calculate();
            case "render" -> connector.createImage();
            case "saveto" -> {
                if (para == null) {
                    //throw new RuntimeException("No file");
                    JFileChooser chooser = new JFileChooser();
                    int value = chooser.showOpenDialog(null);
                    if (value == JFileChooser.APPROVE_OPTION) {
                        para = chooser.getSelectedFile().getAbsolutePath();
                    }
                } else if (para.equals("default")) {
                    para = connector.getSettingS(Constants.FILE_DEFAULT_OUTPUT);
                    if (para == null || para.equals("")) {
                        throw new RuntimeException("No default file provided");
                    }
                } else if (!para.startsWith("\"") || !para.endsWith("\"")) {
                    throw new RuntimeException("File must be in double quotation marks");
                } else {
                    para = para.substring(1, para.length() - 1);
                }
                connector.saveImage(para);
            }
            case "setsetting" -> {
                if (para == null) {
                    throw new RuntimeException("No setting");
                }
                int indexOfEq = para.indexOf('=');
                if (indexOfEq < 0) {
                    throw new RuntimeException("Missing equals");
                }
                String setName = para.substring(0, indexOfEq);
                String setValS = para.substring(indexOfEq + 1);
                Object val = setValS;
                if (setName.startsWith("double")) {
                    val = Double.valueOf(setValS);
                } else if (setName.startsWith("int")) {
                    val = Integer.valueOf(setValS);
                } else if (!setName.startsWith("string")) {
                    throw new RuntimeException("Unknown setting type; cannot cast");
                }
                connector.setSetting(setName, val);
            }
            default -> throw new RuntimeException("Unknown command: " + command);
        }
         */

        System.out.println("Time: " + format.format((System.currentTimeMillis()-t)) + " ms");
    }

    public static void main(String... args) {
        new ConsoleFrontEnd().run();
    }
}
