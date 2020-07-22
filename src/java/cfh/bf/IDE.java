package cfh.bf;

import static java.awt.GridBagConstraints.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

public class IDE {

    public static final String VERSION = "v1.10";
    public static final String NAME = "Brainf*** " + VERSION;
    public static final String STATUS = VERSION + "  2011-08-11  \u00A9  by Carlos F Heuberger";
    
    public static final int DEFAULT_SIZE = 30000;
    public static final Font FONT = new Font("monospaced", Font.PLAIN, 14);
    
    public static void main(String[] args) {
        int size = DEFAULT_SIZE;
        boolean strict = false;
        boolean loop = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-size") && (i+1) < args.length) {
                String arg = args[++i];
                try {
                    size = Integer.parseInt(arg);
                    if (size > 0)
                        continue;
                } catch (NumberFormatException ex) {
                }
            } else if (args[i].equals("-strict")) {
                strict = true;
                loop = true;
                continue;
            }
            System.err.println("unrecognized option/argument: " + args[i] + "\n" +
                        "Options: -size <size>   memory size\n" +
                        "         -strict        strict Brainf*** syntax\n");
            return;
        }
        new IDE(size, strict, loop);
    }

    // TODO mark error, actual pointer(Step)
    
    private JFrame mainFrame;
    
    private JTextArea programField;
    private JTextField inputField;
    private JTextArea outputField;
    
    private JButton runButton;
    private JButton stopButton;
    private JButton stepButton;
    private JButton resetButton;
    private JButton memoryButton;
    private JButton traceButton;
    private JButton helpButton;
    private JButton settingsButton;
    private JLabel statusLeft;
    private JLabel statusRight;
    private HelpDialog helpDialog = null;
    
    private Memory memory;
    private Runtime runtime = null;
    
    private MemoryMonitor memoryMonitor = null;
    private RuntimeMonitor runtimeMonitor = null;

    private Writer outputWriter = null;
    
    private boolean strictMode = false;
    private boolean strictLoop = false;
    
    public IDE(int size, boolean strict, boolean loop) {
        strictMode = strict;
        strictLoop = loop;
        
        memory = new Memory(size, strictMode);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initGUI();
            }
        });
    }
    
    private void initGUI() {
        programField = new JTextArea();
        programField.setBorder(createTitleBorder("Program"));
        programField.setFont(FONT);
        
        inputField = new JTextField();
        inputField.setBorder(createTitleBorder("Input"));
        inputField.setFont(FONT);
        
        outputField = new JTextArea();
        outputField.setBorder(createTitleBorder("Output"));
        outputField.setEditable(false);
        outputField.setFont(FONT);

        runButton = createButton("Run");
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRun();
            }
        });
        
        stopButton = createButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStop();
            }
        });
        stopButton.setEnabled(false);
        
        stepButton = createButton("Step");
        stepButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStep();
            }
        });
        
        memoryButton = createButton("Memory");
        memoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doMemory();
            }
        });
        
        traceButton = createButton("Trace");
        traceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTrace();
            }
        });
        
        resetButton = createButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doReset();
            }
        });
        
        settingsButton = createButton("Settings");
        settingsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSettings();
            }
        });
        
        helpButton = createButton("Help");
        helpButton.setForeground(Color.GREEN.darker());
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doHelp((e.getModifiers() & ActionEvent.CTRL_MASK) != 0);
            }
        });

        Box keyPanel = Box.createHorizontalBox();
        keyPanel.add(Box.createHorizontalStrut(4));
        keyPanel.add(runButton);
        keyPanel.add(Box.createHorizontalStrut(4));
        keyPanel.add(stopButton);
        keyPanel.add(Box.createHorizontalStrut(4));
        keyPanel.add(stepButton);
        keyPanel.add(Box.createHorizontalStrut(16));
        keyPanel.add(memoryButton);
        keyPanel.add(Box.createHorizontalStrut(4));
        keyPanel.add(traceButton);
        keyPanel.add(Box.createHorizontalStrut(16));
        keyPanel.add(resetButton);
        keyPanel.add(Box.createGlue());
        keyPanel.add(settingsButton);
        keyPanel.add(Box.createHorizontalStrut(16));
        keyPanel.add(helpButton);
        keyPanel.add(Box.createHorizontalStrut(4));
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        topPanel.add(
                new JScrollPane(programField), 
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, CENTER, BOTH, new Insets(4, 4, 2, 4), 0, 0));
        topPanel.add(inputField, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 4, 2, 4), 0, 0));
        topPanel.add(keyPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 4, 4, 4), 0, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(new JScrollPane(outputField));
        
        statusLeft = new JLabel(STATUS);
        
        statusRight = new JLabel("init");
        updateStatus();
        
        Box status = Box.createHorizontalBox();
        status.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        status.add(statusLeft);
        status.add(Box.createHorizontalGlue());
        status.add(statusRight);
        
        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setTitle(NAME + " - IDE");
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (runtime != null) {
                    runtime.stop();
                }
                if (memoryMonitor != null) {
                    closeMemoryMonitor();
                }
                if (runtimeMonitor != null) {
                    closeRuntimeMonitor();
                }
                if (memory != null) {
                    memory.dispose();
                }
            }
        });
        
        mainFrame.add(splitPane);
        mainFrame.add(status, BorderLayout.AFTER_LAST_LINE);

        mainFrame.setSize(800, 600);
        mainFrame.validate();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private Border createTitleBorder(String title) {
        Border border = new CompoundBorder(
                new TitledBorder(title),
                BorderFactory.createLoweredBevelBorder());
        return border;
    }
    
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        return button;
    }
    
    private void doRun() {
        runButton.setEnabled(false);
        stepButton.setEnabled(false);
        stopButton.setEnabled(true);
        if (runtime == null) {
            try {
                loadProgram();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, ex);
                return;
            }
            memory.reset();
        } else {
// TODO check already running?
        }
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Runtime tmp = runtime;
                    if (runtimeMonitor != null) {
                        runtimeMonitor.println("RUN");
                    }
                    tmp.run();
                    if (tmp.terminated()) {
                        unloadProgram();
                    }
                    stopButton.setEnabled(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(mainFrame, ex);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    private void doStop() {
        Runtime tmp = runtime;
        if (tmp != null) {
            tmp.stop();
            if (tmp.terminated()) {
                unloadProgram();
            } else {
                runButton.setEnabled(true);
                stepButton.setEnabled(true);
            }
        }
    }
    
    private void doStep() {
        if (runtime == null) {
            try {
                loadProgram();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, ex);
                return;
            }
        }
        try {
            Runtime tmp = runtime;
            tmp.step();
            if (tmp.terminated()) {
                runButton.setEnabled(false);
                stepButton.setEnabled(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, ex);
        }
    }
    
    private void doMemory() {
        if (memoryMonitor == null) {
            memoryMonitor = new MemoryMonitor(memory);
            if (runtime != null) {
                runtime.addPropertyChangeListener(memoryMonitor);
            }
            memoryMonitor.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closeMemoryMonitor();
                }
            });
        } else {
            memoryMonitor.toFront();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainFrame.toFront();
            }
        });
    }
    
    private void doTrace() {
        if (runtimeMonitor == null) {
            runtimeMonitor = new RuntimeMonitor();
            if (runtime != null) {
                runtime.addRuntimeListener(runtimeMonitor);
            }
            runtimeMonitor.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closeRuntimeMonitor();
                }
            });
        } else {
            runtimeMonitor.toFront();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainFrame.toFront();
            }
        });
    }
    
    private void doReset() {
        if (runtime != null) {
            closeRuntime();
        }
        memory.reset();
        outputField.setText(null);
        runButton.setEnabled(true);
        stepButton.setEnabled(true);
        stopButton.setEnabled(false);
        if (runtimeMonitor != null) {
            runtimeMonitor.println("\nRESET");
        }
    }

    private void doSettings() {
        SettingsDialog dialog = new SettingsDialog(mainFrame, memory.getSize(), strictMode, strictLoop); 
        if (dialog.isOkPressed()) {
            strictMode = dialog.getStrict();
            strictLoop = dialog.getLoop();
            int size = dialog.getSize();
            if (memory.getSize() != size) {
                if (runtime != null) {
                    int option = JOptionPane.showConfirmDialog(mainFrame, 
                            "actual program must be ended", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                    if (option != JOptionPane.OK_OPTION)
                        return;
                    closeRuntime();
                }
                boolean hasMonitor = memoryMonitor != null;
                if (hasMonitor) {
                    closeMemoryMonitor();
                }
                Memory newMemory = new Memory(size, strictMode);
                if (size > memory.getSize()) {
                    size = memory.getSize();
                }
                short[] data = memory.getData(0, size);
                for (int i = 0; i < size; i++) {
                    newMemory.setCell(i, data[i]);
                }
                memory = newMemory;
                if (hasMonitor) {
                    doMemory();
                }
            } else {
                memory.setStrictMode(strictMode);
            }
            updateStatus();
        }
    }
    
    private void doHelp(boolean reload) {
        if (reload || helpDialog == null) {
            helpDialog = new HelpDialog(mainFrame);
        }
        helpDialog.setVisible(true);
    }

    private void loadProgram() throws IOException {
        assert runtime == null : runtime;
        
        String program = programField.getText();
        String input = inputField.getText();  // TODO \\u \\n... handling
        
        PipedReader outputReader = new PipedReader();
        outputWriter = new PipedWriter(outputReader);
        Thread thread = new Thread(new ReaderToOutput(outputReader), "ReaderToOutput");
        thread.setDaemon(true);
        thread.start();
        
        runtime = new Runtime(memory, program.toCharArray(),
                new StringReader(input), outputWriter, strictMode, strictLoop);
        if (memoryMonitor != null) {
            runtime.addPropertyChangeListener(memoryMonitor);
        }
        if (runtimeMonitor != null) {
            runtime.addRuntimeListener(runtimeMonitor);
        }
    }
    
    private void unloadProgram() {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, ex);
            }
            outputWriter = null;
        }
        if (runtime != null) {
            closeRuntime();
        }
    }

    private void closeRuntime() {
        runtime.removePropertyChangeListener(memoryMonitor);
        runtime.removeRuntimeListener(runtimeMonitor);
        runtime.stop();
        runtime = null;
    }
    
    private void closeMemoryMonitor() {
        if (runtime != null) {
            runtime.removePropertyChangeListener(memoryMonitor);
        }
        memoryMonitor.dispose();
        memoryMonitor = null;
    }
    
    private void closeRuntimeMonitor() {
        if (runtime != null) {
            runtime.removeRuntimeListener(runtimeMonitor);
        }
        runtimeMonitor.dispose();
        runtimeMonitor = null;
    }
    
    private void updateStatus() {
        StringBuilder builder = new StringBuilder();
        if (strictMode) {
            builder.append("strict ");
        }
//        if (strictLoop) {
//            builder.append("loop ");
//        }
        builder.append(memory.getSize());
        statusRight.setText(builder.toString());
    }
    


    private class ReaderToOutput implements Runnable {
        
        private final Reader reader;
        
        private ReaderToOutput(Reader reader) {
            this.reader = reader;
        }
        
        // implements Runnable
        public void run() {
            int ch;
            try {
                while ((ch = reader.read()) != -1) {
                    outputField.append(Character.toString((char)ch));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            outputField.append("\n");
            outputField.setCaretPosition(outputField.getDocument().getLength());
        }
    }
}
