package cfh.bf;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

public class Memory {

    public static final String PROP_ALL = null;
    public static final String PROP_CELL = "Cell";
    
    protected short[] data;
    
    private boolean strictMode;
    
    private final PropertyChangeSupport changeSupport;
    
    public Memory(int size, boolean strictMode) {
        assert size > 0 : size;
        
        data = new short[size];
        this.strictMode = strictMode;
        changeSupport = new PropertyChangeSupport(this);
    }
    
    public synchronized void decCell(int index) {
        int oldValue = data[index]--;
        if (strictMode) {
            data[index] &= 0xFF;
        }
        fireCellChanged(index, oldValue, data[index]);
    }
    
    public synchronized void incCell(int index) {
        int oldValue = data[index]++;
        if (strictMode) {
            data[index] &= 0xFF;
        }
        fireCellChanged(index, oldValue, data[index]);
    }
    
    public int getCell(int index) {
        return data[index];
    }
    
    public synchronized void setCell(int index, short value) {
        int old = data[index];
        data[index] = value;
        if (strictMode) {
            data[index] &= 0xFF;
        }
        fireCellChanged(index, old, value);
    }
    
    public void setStrictMode(boolean strictMode) {
        boolean oldMode = this.strictMode;
        this.strictMode = strictMode;
        if (strictMode && strictMode != oldMode) {
            for (int index = 0; index < data.length; index++) {
                short old = data[index];
                short value = (short) (old & 0xFF);
                data[index] = value;
                if (value != old) {
                    fireCellChanged(index, old, value);
                }
            }
        }
    }
    
    public short[] getData(int from, int to) {
        assert from >= 0 : from;
        assert to <= data.length : to;
        assert from <= to : from + " > " + to;
        
        return Arrays.copyOfRange(data, from, to);
    }
    
    public int getSize() {
        return data.length;
    }
    
    public synchronized void reset() {
        Arrays.fill(data, (short) 0);
        changeSupport.fireIndexedPropertyChange(PROP_ALL, -1, null, null);
    }
    
    public void dispose() {
        data = null;
        for (PropertyChangeListener listener : changeSupport.getPropertyChangeListeners()) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return changeSupport.getPropertyChangeListeners();
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    private void fireCellChanged(int index, int oldValue, int newValue) {
        changeSupport.fireIndexedPropertyChange(PROP_CELL, index, oldValue, newValue);
    }
}
