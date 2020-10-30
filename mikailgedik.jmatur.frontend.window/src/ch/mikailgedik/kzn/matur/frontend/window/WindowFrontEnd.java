package ch.mikailgedik.kzn.matur.frontend.window;

import ch.mikailgedik.kzn.matur.backend.connector.Connector;
import ch.mikailgedik.kzn.matur.backend.connector.Constants;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class WindowFrontEnd extends JFrame {
    private FractalCanvas canvas;
    private Point mousePoint;

    private JTextArea textPanel;
    private JScrollPane scrollPane;
    private JSplitPane splitPane;
    private JButton refreshButton;

    private final Connector connector;

    public WindowFrontEnd() {
        super("WindowFrontEnd");
        connector = new Connector();
        init();

        splitPane.setDividerLocation(0.8);



        refresh();

        //TODO test zoom
        /*
        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(10);
                    zoomIntoByFactor(-.01);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
        */

    }

    private void zoomIntoByFactor(double factor) {
        connector.zoom(factor);
        connector.createImage();
        canvas.setScreen(connector.getImage());
    }

    private void moveViewportByPixel(int pdx, int pdy) {
        double dx = (1.0 * pdx / canvas.getWidth());
        double dy = (1.0 * pdy / canvas.getHeight());

        connector.moveRenderZone(dx, -dy);
        connector.createImage();
        canvas.setScreen(connector.getImage());
    }

    private void init() {
        createComponents();
        createLayout();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(400,300));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createComponents() {
        canvas = new FractalCanvas();

        textPanel = new JTextArea();
        textPanel.setText("OUTPUT\n");
        textPanel.setForeground(Color.WHITE);
        textPanel.setBackground(Color.BLACK);
        textPanel.setEditable(false);
        scrollPane = new JScrollPane(textPanel);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.add(canvas);
        splitPane.add(scrollPane);

        this.refreshButton = new JButton("Refresh");
        this.refreshButton.addActionListener(e -> this.refresh());

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(mousePoint != null) {
                    moveViewportByPixel(mousePoint.x - e.getX(), mousePoint.y - e.getY());
                    mousePoint = e.getPoint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomIntoByFactor(e.getPreciseWheelRotation());
            }
        };

        canvas.addMouseListener(adapter);
        canvas.addMouseMotionListener(adapter);
        canvas.addMouseWheelListener(adapter);

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                connector.setImagePixelSize(canvas.getWidth(), canvas.getHeight());
                System.out.println("Canvas resize: " + canvas.getWidth() + " " + canvas.getHeight());
                connector.createImage();
                canvas.setScreen(connector.getImage());
            }
        });

        createMenu();
    }

    private void refresh() {
        connector.createImage();
        Screen image = connector.getImage();

        canvas.setScreen(image);
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        {
            //File
            JMenu menuFile = new JMenu("File");

            JMenuItem open = new JMenuItem("Open");
            open.addActionListener(this::fileOpen);
            menuFile.add(open);

            JMenuItem close = new JMenuItem("Close");
            close.addActionListener(this::fileClose);
            menuFile.add(close);

            JMenuItem save = new JMenuItem("Save");
            save.addActionListener(this::fileSave);
            menuFile.add(save);
            menuBar.add(menuFile);
        }
        {
            //Fractal
            JMenu menuFractal = new JMenu("Fractal");


            JMenuItem selectFrac = new JMenuItem("Select Fractal");
            selectFrac.addActionListener(this::selectFractal);
            menuFractal.add(selectFrac);

            JMenuItem setViewport = new JMenuItem("Set viewport");
            selectFrac.addActionListener(this::setViewport);
            menuFractal.add(setViewport);

            menuBar.add(menuFractal);
        }
        {
            //Settings
            JMenu menuSetting = new JMenu("Settings");

            JMenuItem openSettings = new JMenuItem("Open settings");
            openSettings.addActionListener(this::openSettings);

            menuSetting.add(openSettings);

            menuBar.add(menuSetting);
        }

        setJMenuBar(menuBar);
    }

    private void openSettings(ActionEvent actionEvent) {
        JDialog dialog = new JDialog(this, true);

        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);

        GroupLayout.Group ver = layout.createSequentialGroup();
        GroupLayout.Group hor =  layout.createParallelGroup();

        TreeMap<String, Object> map = connector.getAllSettings();
        TreeMap<String, Object[]> tree = new TreeMap<>();

        for(Map.Entry<String, Object> entry: map.entrySet()) {
            JTextField key = new JTextField(entry.getKey());
            key.setEditable(false);
            JTextField value = new JTextField(entry.getValue().toString());
            if(entry.getKey().startsWith("value")) {
                value.setEditable(false);
            }
            tree.put(entry.getKey(), new Object[]{value, entry.getValue()});
            hor.addGroup(layout.createSequentialGroup().addComponent(key).addComponent(value));
            ver.addGroup(layout.createParallelGroup().addComponent(key).addComponent(value));
        }

        JButton closeWithoutSave = new JButton("Exit without saving");
        closeWithoutSave.addActionListener(e -> dialog.dispose());

        JButton closeWithSave = new JButton("Exit and save");
        closeWithSave.addActionListener(e -> {
            tree.forEach((k, v) -> {
                String newString = ((JTextField)v[0]).getText();
                if(!v[1].toString().equals(newString)) {
                    log("New setting for " + k);

                    Object newObj = switch (k.substring(0, k.indexOf('.'))) {
                        case "int" -> Integer.valueOf(newString);
                        case "double" ->  Double.valueOf(newString);
                        case "string" -> newString;
                        default -> throw new RuntimeException("Unsupported type");
                    };

                    connector.setSetting(k, newObj);
                }
            });

            dialog.dispose();
        });

        hor.addGroup(layout.createSequentialGroup().addComponent(closeWithoutSave).addComponent(closeWithSave));
        ver.addGroup(layout.createParallelGroup().addComponent(closeWithoutSave).addComponent(closeWithSave));

        layout.setHorizontalGroup(hor);
        layout.setVerticalGroup(ver);

        panel.setLayout(layout);

        JScrollPane scrollPane = new JScrollPane(panel);
        dialog.add(scrollPane);
        dialog.setSize(new Dimension(400, 300));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void setViewport(ActionEvent actionEvent) {
        assert false;
    }

    private void selectFractal(ActionEvent actionEvent) {
        assert false;
    }

    private void fileClose(ActionEvent actionEvent) {
        System.exit(0);
        assert false;
    }

    private void fileOpen(ActionEvent actionEvent) {
        try {
            UIManager.LookAndFeelInfo[] i = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(i[(int)(Math.random() * i.length)].getClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle("Save in directory");

        dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dialog.setMultiSelectionEnabled(false);
        if(dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            connector.readData(dialog.getSelectedFile());
        }
    }

    private void fileSave(ActionEvent actionEvent) {
        try {
            UIManager.LookAndFeelInfo[] i = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(i[(int)(Math.random() * i.length)].getClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle("Load directory");
        dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dialog.setMultiSelectionEnabled(false);
        if(dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            connector.saveData(dialog.getSelectedFile());
        }
    }

    public void log(String message) {
        textPanel.append(message + "\n");
    }

    private void createLayout() {
        //https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html

        BorderLayout layout = new BorderLayout();

        layout.addLayoutComponent(splitPane, BorderLayout.CENTER);
        layout.addLayoutComponent(refreshButton, BorderLayout.SOUTH);
        this.getContentPane().add(splitPane);
        this.getContentPane().add(refreshButton);
        this.setLayout(layout);
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(WindowFrontEnd::new);
    }
}
