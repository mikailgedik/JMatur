package ch.mikailgedik.kzn.matur.ui.window;

import ch.mikailgedik.kzn.matur.core.connector.CalculatorUnit;
import ch.mikailgedik.kzn.matur.core.connector.Connector;
import ch.mikailgedik.kzn.matur.core.connector.Constants;
import ch.mikailgedik.kzn.matur.core.connector.VideoPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class FractalWindow extends JFrame {
    private FractalCanvas canvas;
    private Point mousePoint;
    private int mouseButton;

    private JPanel slaveContainer;
    private JButton exit;

    private JPanel masterContainer;
    private JMenuBar menuBar;
    private JMenuItem[] animationItems;
    private JPanel loginContainer;
    private JCheckBox isSlave;
    private JButton start, openSettings, editKernelCalc, editKernelRender;

    private final Connector connector;

    private ExecutorService executorService;
    private Future<?> task;

    private int[] selectedArea;

    private VideoPath videoPath;

    public FractalWindow() {
        super("WindowFrontEnd");
        connector = new Connector();
        executorService = Executors.newSingleThreadExecutor();
        //Ensure task != null
        submitTask(() -> {});
        init();
    }

    private void submitTask(Runnable runnable) {
        task = executorService.submit(() -> {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred:\n" + e,
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (AssertionError e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An assertion error occurred:\n" + e,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void zoomIntoByFactor(double factor) {
        if(task.isDone()) {
            connector.zoom(factor);

            refresh();
        }
    }

    private void moveViewportByPixel(int pdx, int pdy) {
        if(task.isDone()) {
            double dx = (1.0 * pdx / canvas.getScreen().getWidth());
            double dy = (1.0 * pdy / canvas.getScreen().getHeight());
            connector.moveRenderZone(dx, -dy);

            refresh();
        }
    }

    private void init() {
        createComponents();
        createLayout();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(160 * 10 / 2,90 * 10 / 2));
        setLocationRelativeTo(null);

        setVisible(true);
    }

    private double[] convertCoordinates(double[] imgLoc) {
        {
            double h = imgLoc[1];
            imgLoc[1] = imgLoc[3];
            imgLoc[3] = h;

            imgLoc[1] = 1 - imgLoc[1];
            imgLoc[3] = 1 - imgLoc[3];
        }

        double[] currCen = connector.getRenderCenter();
        double curHeight = connector.getRenderHeight();
        double curWidth = curHeight * connector.getAspectRatio();

        double[] start = {currCen[0] - curWidth/2, currCen[1] - curHeight/2};
        double newH = curHeight * (imgLoc[3] - imgLoc[1]);
        double newW = newH * connector.getAspectRatio();

        start[0] += imgLoc[0] * curWidth;
        start[1] += imgLoc[1] * curHeight;

        return new double[]{
                start[0] + newW /2,
                start[1] + newH / 2, newH
        };
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

                if(isSlave.isSelected()) {
                    try {
                        connector.initSlave();
                        setContentPane(slaveContainer);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Failed to connect!\n" + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    connector.initMaster(-1);
                    setJMenuBar(menuBar);
                    canvas.setBusy(true);
                    setContentPane(masterContainer);
                }
                this.validate();
            });

            editKernelRender.addActionListener((event) -> connector.setClKernelRender(showStringEditDialog(connector.getClKernelRender())));
            editKernelCalc.addActionListener((event) -> connector.setClKernelCalculate(showStringEditDialog(connector.getClKernelCalculate())));
            openSettings.addActionListener(this::openSettings);
        }
        {
            masterContainer = new JPanel();
            canvas = new FractalCanvas();

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    mousePoint = e.getPoint();
                    mouseButton = e.getButton();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(task.isDone() && selectedArea != null) {
                        double[] imgLoc = canvas.getSelectedAreaOnScreen();

                        double[] nC = convertCoordinates(imgLoc);

                        connector.setRenderParameters(nC, nC[2]);

                        refresh();
                    }
                    selectedArea = null;
                    canvas.setSelectedArea(null, getInfoString());
                    mouseButton = MouseEvent.NOBUTTON;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if(mousePoint != null) {
                        if(mouseButton == MouseEvent.BUTTON1) {
                            moveViewportByPixel(mousePoint.x - e.getX(), mousePoint.y - e.getY());
                        } else if(mouseButton == MouseEvent.BUTTON3) {
                            //Area
                            if(selectedArea == null) {
                                selectedArea = new int[]{
                                        e.getX(),
                                        e.getY(),
                                        e.getX()+1,
                                        e.getY()+1
                                };
                            } else {
                                selectedArea[2] = e.getX();
                                selectedArea[3] = e.getY();
                                int h;
                                if(selectedArea[0] > selectedArea[2]) {
                                    h = selectedArea[2];
                                    selectedArea[2] = selectedArea[0];
                                    selectedArea[0] = h;
                                }
                                if(selectedArea[1] > selectedArea[3]) {
                                    h = selectedArea[3];
                                    selectedArea[3] = selectedArea[1];
                                    selectedArea[1] = h;
                                }
                            }

                            if(selectedArea[3] - selectedArea[1] < 10) {
                                selectedArea[3] = selectedArea[1] + 10;
                            }
                            selectedArea[2] = (int)
                                    (selectedArea[0] + (selectedArea[3] - selectedArea[1]) * connector.getAspectRatio());

                            canvas.setSelectedArea(selectedArea, getInfoString());
                        }
                    }
                    mousePoint = e.getPoint();
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

                    refresh();
                }
            });
        }
        {
            slaveContainer = new JPanel();
            exit = new JButton("Exit");
            exit.addActionListener((e) -> System.exit(0));
        }

        createMenu();
    }

    private String getInfoString() {
        String s = "Center x: " + connector.getRenderCenter()[0] + "\n" +
                "Center y: " + connector.getRenderCenter()[1];
        //TODO canvas.getSelectedAreaOnCanvas returns null on first request when dragging
        if(canvas.getSelectedAreaOnCanvas() != null) {
            double[] d = canvas.getSelectedAreaOnScreen();
            d = convertCoordinates(d);
            s += "\nSelected center x: " + d[0] + "\n" +
                    "Selected center y: " + d[1] + "\n" +
                    "Selected center height: " + d[2] + "\n";
        }
        return s;
    }

    private void refresh() {
        if(task.isDone()) {
            submitTask(this::refreshWithOutBlock);
        }
    }

    private void refreshWithOutBlock() {
        SwingUtilities.invokeLater(() -> canvas.setBusy(true));
        connector.createImage();
        SwingUtilities.invokeLater(() -> {
            canvas.setScreen(connector.getImage(), getInfoString());
            canvas.setBusy(false);
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

            JMenuItem save = new JMenuItem("Save");
            save.addActionListener(this::fileSave);
            menuFile.add(save);

            menuFile.addSeparator();
            JMenuItem about = new JMenuItem("About");
            about.addActionListener(this::showAbout);
            menuFile.add(about);

            menuFile.addSeparator();

            JMenuItem close = new JMenuItem("Exit");
            close.addActionListener(this::exitApplication);
            menuFile.add(close);

            menuBar.add(menuFile);
        }
        {
            //Fractal
            JMenu menuFractal = new JMenu("Fractal");

            JMenuItem setViewport = new JMenuItem("Set viewport");
            setViewport.addActionListener(this::setViewport);
            menuFractal.add(setViewport);

            JMenuItem exportImage = new JMenuItem("Export image");
            exportImage.addActionListener(this::exportImage);
            menuFractal.add(exportImage);

            menuBar.add(menuFractal);
        }
        {
            //Settings
            JMenu menuAnimation = new JMenu("Animation");

            String[] text = {
                    "Add position to path",
                    "Go through path",
                    "Import animation path",
                    "Export animation path",
                    "Destroy video path",
                    "Render animation path"
            };
            ActionListener[] lis = {
                    this::addAnimationPosition,
                    this::goThroughPath,
                    this::importAnimationPath,
                    this::exportAnimationPath,
                    this::destroyVideoPath,
                    this::renderAnimation
            };
            KeyStroke[] accelerators = {
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK),
                    null,
                    KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK),
                    KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK),
                    null,
                    null
            };

            animationItems = new JMenuItem[text.length];

            for(int i = 0; i < text.length; i++) {
                animationItems[i] = new JMenuItem(text[i]);
                animationItems[i].addActionListener(lis[i]);
                animationItems[i].setEnabled(false);
                animationItems[i].setAccelerator(accelerators[i]);
                menuAnimation.add(animationItems[i]);
            }
            animationItems[0].setEnabled(true);
            animationItems[2].setEnabled(true);
            menuBar.add(menuAnimation);
        }
    }

    private void exportImage(ActionEvent event) {
        submitTask(() -> {
            File f = showFileDialog("File to save", JFileChooser.FILES_ONLY, true);
            if(f != null) {
                try {
                    askConnectorParameters(false);
                    SwingUtilities.invokeLater(() -> canvas.setBusy(true));
                    connector.createImage();
                    connector.saveImage(f.getAbsolutePath());
                    putOldParameters();
                    connector.createImage();
                    refreshWithOutBlock();
                    SwingUtilities.invokeLater(() -> canvas.setBusy(false));
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Could not parse number:\n" + e1,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                putOldParameters();
            }
        });
    }

    private void showAbout(ActionEvent event) {
        JDialog dialog = new JDialog(this, true);
        JTextPane textPane = new JTextPane();
        textPane.setText("About: This software calculates and renders fractals using OpenCL and Java\n" +
                "Author: Mikail Gedik, November 2020");
        dialog.add(textPane);

        dialog.setLayout(new GridLayout(1,1));

        dialog.setSize(new Dimension(400, 300));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void setVideoPath(VideoPath p) {
        this.videoPath = p;
        if(p == null) {
            destroyVideoPath(null);
        } else {
            for (JMenuItem animationItem : this.animationItems) {
                animationItem.setEnabled(true);
            }
        }
    }

    private void createVideoPath(VideoPath.VideoPoint p) {
        setVideoPath(new VideoPath(p));
    }

    private void destroyVideoPath(ActionEvent event) {
        this.videoPath = null;
        for (JMenuItem animationItem : this.animationItems) {
            animationItem.setEnabled(false);
        }
        animationItems[0].setEnabled(true);
        animationItems[2].setEnabled(true);
    }

    private void addAnimationPosition(ActionEvent event) {
        for (JMenuItem animationItem : this.animationItems) {
            animationItem.setEnabled(true);
        }
        if(this.videoPath == null) {
            createVideoPath(new VideoPath.VideoPoint(
                    connector.getRenderCenter(), connector.getRenderHeight()
            ));
        } else {
            try {
                int frames = Integer.parseInt(JOptionPane.showInputDialog(this, "How many frames will there " +
                        "be between this point and the previous?", "Input needed", JOptionPane.QUESTION_MESSAGE));
                this.videoPath.addNext(new VideoPath.VideoPoint(
                        connector.getRenderCenter(), connector.getRenderHeight()), frames);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Could not parse number; no breakpoint set",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Object[] oldParameters;

    private void askConnectorParameters(boolean onlyOutPut) {
        JDialog dialog = new JDialog(this, true);
        JButton saveAndClose = new JButton("Save parameters");

        double[] c = connector.getRenderCenter().clone();
        double height = connector.getRenderHeight();
        int hP = connector.getImagePixelHeight();
        double aspect = connector.getAspectRatio();

        JTextField[][] textFields;

        if(!onlyOutPut) {
            dialog.setLayout(new GridLayout(6,1));
            textFields = new JTextField[][]{
                    {new JTextField("Center x"), new JTextField(Double.toString(c[0]))},
                    {new JTextField("Center y"), new JTextField(Double.toString(c[1]))},
                    {new JTextField("Fractal height"), new JTextField(Double.toString(height))},
                    {new JTextField("Image height (pixels)"), new JTextField(Integer.toString(hP))},
                    {new JTextField("Aspect ratio"), new JTextField(Double.toString(aspect))}
            };
        } else {
            dialog.setLayout(new GridLayout(3,1));
            textFields = new JTextField[][]{
                    {new JTextField("Image height (pixels)"), new JTextField(Integer.toString(hP))},
                    {new JTextField("Aspect ratio"), new JTextField(Double.toString(aspect))}
            };
        }

        for(JTextField[] t: textFields) {
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(2,1));
            t[0].setEditable(false);
            p.add(t[0]);
            p.add(t[1]);
            dialog.add(p);
        }


        saveAndClose.addActionListener((e) -> {
            try {
                this.oldParameters = new Object[]{
                        c, height, hP, aspect
                };
                if(!onlyOutPut) {
                    connector.setRenderParameters(new double[]{
                            Double.parseDouble(textFields[0][1].getText()),
                            Double.parseDouble(textFields[1][1].getText())
                    }, Double.parseDouble(textFields[2][1].getText()));
                    connector.setImagePixelHeight(Integer.parseInt(textFields[3][1].getText()));
                    connector.setAspectRatio(Double.parseDouble(textFields[4][1].getText()));
                } else {
                    connector.setImagePixelHeight(Integer.parseInt(textFields[0][1].getText()));
                    connector.setAspectRatio(Double.parseDouble(textFields[1][1].getText()));
                }

                dialog.dispose();
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(this, "Could not parse number:\n" + e1,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialog.add(saveAndClose);
        dialog.setSize(new Dimension(400, 300));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        System.out.println("Out");
    }

    private void putOldParameters() {
        assert oldParameters != null;
        connector.setRenderParameters((double[]) oldParameters[0], (Double) oldParameters[1]);
        connector.setImagePixelHeight((Integer) oldParameters[2]);
        connector.setAspectRatio((Double) oldParameters[3]);
    }

    private void renderAnimation(ActionEvent event) {
        if(task.isDone()) {


            JDialog dialog = new JDialog(this, true);
            dialog.setLayout(new GridLayout(4,1));

            final JProgressBar[] bars = new JProgressBar[2];
            JTextField[] desc = new JTextField[2];
            desc[0] = new JTextField("Calculation progress: 0%");
            desc[1] = new JTextField("Render progress:      0%");
            for(int i = 0; i < bars.length; i++) {
                desc[i].setEditable(false);
                desc[i].setBorder(BorderFactory.createLineBorder(Color.GREEN, 5, true));
                dialog.add(desc[i]);

                bars[i] = new JProgressBar();
                dialog.add(bars[i]);
            }

            Thread updater = new Thread(() -> {
                try {
                    while(true) {
                        Thread.sleep(250);
                        double[] d = connector.getVideoCreationStage();
                        synchronized (bars) {
                            if(bars[0] != null) {
                                bars[0].setValue((int) (d[0] * 100 + .5));
                                bars[1].setValue((int) (d[1] * 100 + .5));
                                desc[0].setText("Calculation progress: " + (int) (d[0] * 100 + .5) + "%");
                                desc[1].setText("Render progress:      " + (int) (d[1] * 100 + .5) + "%");
                            } else {
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            submitTask(() -> {
                File f = showFileDialog("Select output file", JFileChooser.FILES_ONLY, true);
                if(f == null) {
                    return;
                }
                askConnectorParameters(true);
                try {
                    canvas.setBusy(true);
                    FileOutputStream ooo = new FileOutputStream(f);
                    connector.startVideoCreation(this.videoPath, ooo);
                    updater.start();
                    connector.getVideoCreator().join();
                    ooo.close();
                    putOldParameters();
                    synchronized (bars) {
                        bars[0] = null;
                        dialog.dispose();
                    }
                    canvas.setBusy(false);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });

            dialog.setSize(new Dimension(400, 250));
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    private void goThroughPath(ActionEvent event) {
        if(task.isDone()) {
            submitTask(() -> {
                for(VideoPath.VideoPoint p: videoPath) {
                    connector.setRenderParameters(p.getCenter(), p.getHeight());
                    refreshWithOutBlock();
                }
            });
        }
    }

    private void importAnimationPath(ActionEvent event) {
        File file = showFileDialog("Select file", JFileChooser.FILES_ONLY, false);
        if(file != null) {
            try {
                FileInputStream in = new FileInputStream(file);
                setVideoPath(VideoPath.read(in));
                in.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    private void exportAnimationPath(ActionEvent event) {
        File file = showFileDialog("Select file", JFileChooser.FILES_ONLY, true);
        if(file != null) {
            try {
                FileOutputStream out = new FileOutputStream(file);
                VideoPath.write(out, this.videoPath);
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
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
            JTextField key = new JTextField(Constants.getDescription(entry.getKey()));
            key.setEditable(false);
            JTextField value = new JTextField(entry.getValue().toString());
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
                    System.out.println(("New setting for " + k));

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
        if(task.isDone()) {
            JDialog dialog = new JDialog(this, true);
            JButton ok = new JButton("Ok"), cancel = new JButton("Cancel");
            JTextField[] fields = {
                    new JTextField(String.valueOf(connector.getRenderCenter()[0])),
                    new JTextField(String.valueOf(connector.getRenderCenter()[1])),
                    new JTextField(String.valueOf(connector.getRenderHeight()))
            };

            JPanel panel = new JPanel();
            for(JTextField f: fields) {
                panel.add(f);
            }
            cancel.addActionListener((ev) -> dialog.dispose());
            ok.addActionListener((ev) -> {
                try {
                    double[] c = {Double.parseDouble(fields[0].getText()),
                            Double.parseDouble(fields[1].getText())};
                    double h = Double.parseDouble(fields[2].getText());
                    connector.setRenderParameters(c, h);
                    refresh();
                } catch(NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Could not parse number; no breakpoint set",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                dialog.dispose();
            });

            panel.add(ok);
            panel.add(cancel);
            panel.setLayout(new GridLayout(5,1, 30, 10));
            dialog.setContentPane(panel);

            dialog.setSize(new Dimension(400, 250));
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    private void exitApplication(ActionEvent actionEvent) {
        System.exit(0);
    }

    private void fileOpen(ActionEvent actionEvent) {
        File f = showFileDialog("Load from directory", JFileChooser.DIRECTORIES_ONLY, false);
        if(f != null) {
            connector.readData(f);
        }
    }

    private void fileSave(ActionEvent actionEvent) {
        File f = showFileDialog("Load from directory", JFileChooser.DIRECTORIES_ONLY, true);
        if(f != null) {
            connector.saveData(f);
        }
    }

    private File showFileDialog(String title, int mode, boolean saveDialog) {
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle(title);
        dialog.setFileSelectionMode(mode);
        dialog.setMultiSelectionEnabled(false);
        int r;
        if(saveDialog) {
            r = dialog.showSaveDialog(this);
        } else {
            r = dialog.showOpenDialog(this);
        }
        if(r == JFileChooser.APPROVE_OPTION) {
            return dialog.getSelectedFile();
        } else {
            return null;
        }
    }

    private void createLayout() {
        {
            //https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
            BorderLayout layout = new BorderLayout();

            layout.addLayoutComponent(canvas, BorderLayout.CENTER);
            masterContainer.add(canvas);
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
            GridLayout layout = new GridLayout(2,1);
            JTextArea text = new JTextArea();
            text.setText("Thanks for sharing");
            text.setEditable(false);
            slaveContainer.add(text);
            slaveContainer.add(exit);
            slaveContainer.setLayout(layout);
        }
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        if(args.length == 0) {
            SwingUtilities.invokeAndWait(FractalWindow::new);
        } else {
            assert false;
        }
    }
}
