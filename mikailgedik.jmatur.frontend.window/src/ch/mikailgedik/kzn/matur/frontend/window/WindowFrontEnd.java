package ch.mikailgedik.kzn.matur.frontend.window;

import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;
import ch.mikailgedik.kzn.matur.backend.connector.Connector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class WindowFrontEnd extends JFrame {
    private FractalCanvas canvas;
    private Point mousePoint;

    private JPanel slaveContainer;
    private JButton exit;

    private JPanel masterContainer;
    private JTextArea textPanel;
    private JScrollPane scrollPane;
    private JSplitPane splitPane;
    private JMenuBar menuBar;

    private JPanel loginContainer;
    private JCheckBox isSlave;
    private JButton start, openSettings, editKernelCalc, editKernelRender;

    private final Connector connector;

    private ExecutorService executorService;
    private Future<?> task;

    public WindowFrontEnd() {
        super("WindowFrontEnd");
        connector = new Connector();
        executorService = Executors.newSingleThreadExecutor();
        //Ensure task != null
        task = executorService.submit(() -> {});
        init();
    }

    private void zoomIntoByFactor(double factor) {
        if(task.isDone()) {
            connector.zoom(factor);

            refresh();
        }
    }

    private void moveViewportByPixel(int pdx, int pdy) {
        if(task.isDone()) {
            double dx = (1.0 * pdx / canvas.getWidth());
            double dy = (1.0 * pdy / canvas.getHeight());
            connector.moveRenderZone(dx * connector.getAspectRatio(), -dy);

            refresh();
        }
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
        {
            loginContainer = new JPanel();
            start = new JButton("Start");
            openSettings = new JButton("Open calculation settings");
            editKernelCalc = new JButton("Edit calculation kernel");
            editKernelRender = new JButton("Edit render kernel");

            isSlave = new JCheckBox("Use as slave");

            start.addActionListener((event) -> {
                showUnitsDialog();
                boolean changed = false;

                if(isSlave.isSelected()) {
                    try {
                        connector.initSlave();
                        setContentPane(slaveContainer);
                        changed = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Failed to connect!");
                    }
                } else {
                    connector.initMaster(-1);
                    splitPane.add(scrollPane);
                    setJMenuBar(menuBar);
                    canvas.setBusy(true);
                    setContentPane(masterContainer);
                    changed = true;
                }
                if(changed) {
                    this.validate();
                    splitPane.setDividerLocation(0.8);
                    if(!connector.isSlave()) {
                        refresh();
                    }
                }
            });

            editKernelRender.addActionListener((event) -> connector.setClKernelRender(showStringEditDialog(connector.getClKernelRender())));
            editKernelCalc.addActionListener((event) -> connector.setClKernelCalculate(showStringEditDialog(connector.getClKernelCalculate())));
            openSettings.addActionListener(this::openSettings);
        }
        {
            masterContainer = new JPanel();
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
                    connector.setImagePixelHeight(canvas.getHeight());
                    System.out.println("Canvas resize: " + canvas.getWidth() + " " + canvas.getHeight());

                    refresh();
                }
            });
        }
        {
            slaveContainer = new JPanel();
            exit = new JButton("Exit");
        }

        createMenu();
    }

    private void refresh() {
        if(!task.isDone()) {
            return;//Discard new imageca
        }
        this.task = executorService.submit(() -> {
            SwingUtilities.invokeLater(() -> canvas.setBusy(true));
            System.out.println("Gray");
            connector.createImage();
            SwingUtilities.invokeLater(() -> {
                canvas.setScreen(connector.getImage());
                canvas.setBusy(false);
            });
        });
    }

    private void createMenu() {
        menuBar = new JMenuBar();
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
    }

    private String showStringEditDialog(String start) {
        JDialog dialog = new JDialog(this, true);
        JPanel panel = new JPanel();
        JTextArea textArea = new JTextArea();
        textArea.append(start);
        JButton closeWithSave = new JButton("Exit and save");
        AtomicReference<String> s = new AtomicReference<>(start);
        closeWithSave.addActionListener(e -> {
            s.set(textArea.getText());
            dialog.dispose();
        });
        JScrollPane scrollPane = new JScrollPane(textArea);

        BorderLayout layout = new BorderLayout();
        layout.addLayoutComponent(scrollPane, BorderLayout.CENTER);
        layout.addLayoutComponent(closeWithSave, BorderLayout.SOUTH);
        panel.add(scrollPane);
        panel.add(closeWithSave);
        panel.setLayout(layout);

        dialog.add(panel);
        dialog.setSize(new Dimension(400, 600));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return s.get();
    }

    private void showUnitsDialog() {
        DefaultListModel<CalculatorUnit> listModel = new DefaultListModel<>();

        JList<CalculatorUnit> list = new JList<>(listModel);

        JDialog dialog = new JDialog(this, true);
        JPanel panel = new JPanel();
        JButton closeWithSave = new JButton("Exit and save");
        closeWithSave.addActionListener(e -> {
            ArrayList<CalculatorUnit> units = new ArrayList<>();
            for(int i: list.getSelectedIndices()) {
                units.add(listModel.get(i));
            }
            connector.useCalculatorUnits(units);
            dialog.dispose();
        });
        JScrollPane scrollPane = new JScrollPane(list);

        BorderLayout layout = new BorderLayout();
        layout.addLayoutComponent(scrollPane, BorderLayout.CENTER);
        layout.addLayoutComponent(closeWithSave, BorderLayout.SOUTH);
        panel.add(scrollPane);
        panel.add(closeWithSave);
        panel.setLayout(layout);

        dialog.add(panel);
        dialog.setSize(new Dimension(400, 600));
        dialog.setLocationRelativeTo(this);
        connector.sendAvailableUnitsTo(listModel::addElement);
        dialog.setVisible(true);
    }

    private void openSettings(ActionEvent actionEvent) {
        JDialog dialog = new JDialog(this, true);

        JPanel settingsPanel = new JPanel();
        GroupLayout layout = new GroupLayout(settingsPanel);

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

        layout.setHorizontalGroup(hor);
        layout.setVerticalGroup(ver);
        settingsPanel.setLayout(layout);

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

        JScrollPane scrollPane = new JScrollPane(settingsPanel);
        dialog.add(scrollPane);
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new GridLayout(1,2));
            buttonPanel.add(closeWithoutSave);
            buttonPanel.add(closeWithSave);
        }

        dialog.add(buttonPanel);
        BorderLayout dialogLayout = new BorderLayout();
        dialogLayout.addLayoutComponent(scrollPane, BorderLayout.CENTER);
        dialogLayout.addLayoutComponent(buttonPanel, BorderLayout.SOUTH);

        dialog.setLayout(dialogLayout);

        dialog.setSize(new Dimension(400, 600));
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
        {
            //https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
            BorderLayout layout = new BorderLayout();

            layout.addLayoutComponent(splitPane, BorderLayout.CENTER);
            masterContainer.add(splitPane);
            masterContainer.setLayout(layout);
        }

        {
            GridLayout layout = new GridLayout(10,1);

            loginContainer.add(openSettings);
            loginContainer.add(editKernelCalc);
            loginContainer.add(editKernelRender);
            loginContainer.add(isSlave);
            loginContainer.add(start);
            loginContainer.setLayout(layout);
            this.setContentPane(loginContainer);
        }
        {
            BorderLayout layout = new BorderLayout();

            layout.addLayoutComponent(scrollPane, BorderLayout.CENTER);
            slaveContainer.add(scrollPane);
            slaveContainer.setLayout(layout);
        }
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        if(args.length == 0) {
            SwingUtilities.invokeAndWait(WindowFrontEnd::new);
        } else {
            assert false;
        }
    }
}
