package cfh.bf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowListener;
import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;


public class MemoryMonitor implements PropertyChangeListener {

    private static final int COLUMNS = 10;

    private Memory memory;
    
    private JFrame mainFrame;
    private DefaultTableModel model;
    private JTable table;
    private JLabel status;

    private int runtimeIndex = 0;
    private int lastChange = -1;
    private boolean changeWasInc = false;

    
    public MemoryMonitor(Memory memory) {
        assert memory != null;
        
        this.memory = memory;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initGUI();
                initListener();
            }
        });
    }

    public void toFront() {
        if (mainFrame != null) {
            mainFrame.toFront();
        }
    }

    public void dispose() {
        memory.removePropertyChangeListener(this);
        memory = null;
        table = null;
        model = null;
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    public void addWindowListener(final WindowListener listener) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainFrame.addWindowListener(listener);
            }
        });
    }

    private void initGUI() {
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("");
        for (int i = 0; i < COLUMNS; i++) {
            model.addColumn(i);
        }
        Object[] row = new Object[COLUMNS+1];
        for (int i = 0; i < memory.getSize(); i += row.length-1) {
            row[0] = i;
            model.addRow(row);
        }
        
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        
        status = new JLabel();
        
        Box statusBar = Box.createHorizontalBox();
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(status);
        statusBar.add(Box.createHorizontalStrut(8));
        
        fillModel();

        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setTitle(IDE.NAME + " - Memory");
        
        mainFrame.add(new JScrollPane(table), BorderLayout.CENTER);
        mainFrame.add(statusBar, BorderLayout.AFTER_LAST_LINE);
        
        mainFrame.setSize(600, 400);
        mainFrame.validate();
        mainFrame.setVisible(true);
    }

    private void initListener() {
        memory.addPropertyChangeListener(this);
    }

    private void fillModel() {
        short[] data = memory.getData(0, memory.getSize());
        for (int i = 0; i < data.length; i++) {
            model.setValueAt(data[i], getRow(i), getColumn(i));
        }
        updateStatus();
    }

    // PropertyChangeListener
    public void propertyChange(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
        if (property == Memory.PROP_ALL) {
            // reset
            fillModel();
            runtimeIndex = 0;
            lastChange = -1;
        } else if (property.equals(Memory.PROP_CELL)) {
            int index = ((IndexedPropertyChangeEvent) evt).getIndex();
            if (lastChange >= 0 && lastChange != index) {
                model.fireTableCellUpdated(getRow(lastChange), getColumn(lastChange));
            }
            Integer newValue = (Integer) evt.getNewValue();
            model.setValueAt(newValue, getRow(index), getColumn(index));
            lastChange = index;
            changeWasInc = (newValue.compareTo((Integer)evt.getOldValue()) > 0);
        } else if (property.equals(Runtime.PROP_INDEX)) {
            int old = (Integer) evt.getOldValue();
            if (0 <= old && old < memory.getSize()) {
                model.fireTableCellUpdated(getRow(old), getColumn(old));
            }
            runtimeIndex = (Integer) evt.getNewValue();
            if (0 <= runtimeIndex && runtimeIndex < memory.getSize()) {
                model.fireTableCellUpdated(getRow(runtimeIndex), getColumn(runtimeIndex));
            }
        }
        updateStatus();
    }
    
    private static final int getIndex(int row, int col) {
        return col > 0 ? row * COLUMNS + col - 1 : -1;
    }
    
    private static final int getRow(int index) {
        return index / COLUMNS;
    }
    
    private static final int getColumn(int index) {
        return index % COLUMNS + 1;
    }

    private void updateStatus() {
// TODO        status.setText(memory.getCell());
    }
    
    private class CellRenderer extends DefaultTableCellRenderer {

        private final Border INDEX_BORDER = BorderFactory.createLineBorder(
                Color.GREEN.darker(), 3);
        private final Color INC_COLOR = Color.BLUE;
        private final Color DEC_COLOR = Color.RED;
        private final Color NULL_COLOR = Color.LIGHT_GRAY;

        private final Font normalFont;
        private final Font indexFont;
        private final Font changedFont;
        
        private CellRenderer() {
            normalFont = table.getFont();
            indexFont = normalFont.deriveFont(Font.BOLD);
            changedFont = normalFont.deriveFont(Font.BOLD);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(TRAILING);
            setFont(normalFont);
            setForeground(table.getForeground());
            setBackground(table.getBackground());
            if (col > 0) {
                int index = getIndex(row, col);
                if (index >= memory.getSize()) {
                    setBackground(NULL_COLOR);
                } else if (index == runtimeIndex) {
                    setBorder(INDEX_BORDER);
                }
                if (index == lastChange) {
                    setFont(changedFont);
                    setForeground(changeWasInc ? INC_COLOR : DEC_COLOR);
                }
            } else {
                setHorizontalAlignment(CENTER);
                setFont(indexFont);
            }
            return this;
        }
        
    }
}
