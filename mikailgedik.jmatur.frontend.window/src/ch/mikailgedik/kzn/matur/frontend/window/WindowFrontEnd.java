package ch.mikailgedik.kzn.matur.frontend.window;

import ch.mikailgedik.kzn.matur.backend.connector.Connector;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public class WindowFrontEnd extends JFrame {
    private FractalCanvas canvas;
    private final Connector connector;

    public WindowFrontEnd() {
        super("WindowFrontEnd");
        connector = new Connector();
        init();

        refresh();

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component c = e.getComponent();
                canvas.repaint();
                canvas.revalidate();
            }
        });
    }

    private void init() {
        createComponents();
        createLayout();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(200,150));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createComponents() {
        canvas = new FractalCanvas();

        createMenu();
    }

    private void refresh() {
        connector.calculate();
        connector.createImage();
        Screen image = connector.getImage();

        canvas.setScreen(image);

        this.validate();
        this.repaint();
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        {
            //File
            JMenu menuFile = new JMenu("File");
            menuFile.add(new JMenuItem("Open"));
            menuFile.add(new JMenuItem("Close"));
            menuFile.add(new JMenuItem("Save"));
            menuBar.add(menuFile);
        }
        {
            //Fractal
            JMenu menuFractal = new JMenu("Fractal");
            menuFractal.add(new JMenuItem("Select Fractal"));
            menuFractal.add(new JMenuItem("Set viewport"));
            menuBar.add(menuFractal);
        }
        {
            //Settings
            JMenu menuSetting = new JMenu("Settings");
            menuSetting.add(new JMenuItem("Open settings"));
            menuBar.add(menuSetting);
        }

        setJMenuBar(menuBar);
    }

    private void createLayout() {
        //https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html

        GroupLayout layout = new GroupLayout(this.getContentPane());
        //Horizontal
        {
            GroupLayout.Group hor = layout.createSequentialGroup();
            hor.addComponent(canvas,0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
            layout.setHorizontalGroup(hor);

        }
        //Vertical
        {
            GroupLayout.Group ver = layout.createParallelGroup();
            ver.addComponent(canvas, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
            layout.setVerticalGroup(ver);
        }
        this.setLayout(layout);
    }


    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(WindowFrontEnd::new);
    }
}
