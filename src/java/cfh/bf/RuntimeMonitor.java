package cfh.bf;

import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import cfh.bf.Runtime.Runner;

public class RuntimeMonitor implements RuntimeListener {

    private JFrame mainFrame;
    private JTextArea text;
    
    public RuntimeMonitor() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initGUI();
            }
        });
    }
    
    private  void initGUI() {
        text = new JTextArea();
        text.setEditable(false);
        text.setFont(IDE.FONT);
        
        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setTitle(IDE.NAME + " - Trace");
        
        mainFrame.add(new JScrollPane(text));
        
        mainFrame.setSize(600, 400);
        mainFrame.validate();
        mainFrame.setVisible(true);
    }

    public void addWindowListener(final WindowListener listener) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainFrame.addWindowListener(listener);
            }
        });
    }

    public void toFront() {
        mainFrame.toFront();
    }

    public void dispose() {
        mainFrame.dispose();
    }

    // implements RuntimeListener
    @Override
    public void create(Runner runner) {
        println(runner + ":" + runner.getPointer() + "\tstart");
    }
    
    // implements RuntimeListener
    @Override
    public void step(Runner runner, int pointer, char command, int index, int value) {
        println(runner + ":" + pointer + "\t" + command + "\t[" + index + "]=" + value);
    }

    // implements RuntimeListener
    @Override
    public void close(Runner runner) {
        println(runner + ":" + runner.getPointer() + "\tstop");
    }
    
    public void println(String msg) {
        text.append(msg);
        text.append("\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
}
