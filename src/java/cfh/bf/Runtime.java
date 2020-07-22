package cfh.bf;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class Runtime {

    public static final String PROP_POINTER = "Pointer";
    public static final String PROP_INDEX = "Index";
    public static final String PROP_FORK = "Fork";
    
    private static int runnerNum = 0;
    
    private final Memory memory;
    private final char[] program;
    private final PushbackReader input;
    private final Writer output;

    private boolean strictMode = false;
    private boolean strictLoop = false;
    
    private volatile boolean stopRunning = false;

    private final List<Runner> runners;
    private int nextRunner;
    
    private final HashMap<Integer, Integer> endMap;
    private final HashMap<Integer, Integer> startMap;
    
    private final PropertyChangeSupport changeSupport;
    
    private final List<RuntimeListener> listeners;

    // TODO listener (pointer, start, step, stop)
    
    public Runtime(Memory memory, char[] program, Reader input, Writer output, boolean strictMode, boolean strictLoop) {
        assert memory != null;
        assert program != null;
        assert output != null;
        
        this.memory = memory;
        this.program = program;
        this.input = new PushbackReader(input);
        this.output = output;
        this.strictMode = strictMode;
        this.strictLoop = strictLoop;
        
        changeSupport = new PropertyChangeSupport(this);
        listeners = new ArrayList<RuntimeListener>();
    
        runners = new Vector<Runner>();
        endMap = new HashMap<Integer, Integer>();
        startMap = new HashMap<Integer, Integer>();
        
        nextRunner = 0;
        runners.add(new Runner());
    }
    
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    public void setStrictLoop(boolean strictLoop) {
        this.strictLoop = strictLoop;
    }

    public void run() throws BFRuntimeException {
        assert !runners.isEmpty();
        
        stopRunning = false;
        while (!stopRunning && !terminated()) {
            singleStep();
            Thread.yield();
        }
    }
    
    public void step() throws BFRuntimeException {
        assert !runners.isEmpty();
    
        stopRunning = false;
        singleStep();
    }
    
    public void stop() {
        stopRunning = true;
    }
    
    public boolean terminated() {
        return runners.isEmpty() || (runners.size() == 1 && runners.get(0).terminated());
    }

    private synchronized void singleStep() throws BFRuntimeException {
        Runner runner = runners.get(nextRunner);
        try {
            runner.singleStep();
        } finally {
            if (runner.terminated()) {
                runner = runners.remove(nextRunner);
                runner.close();
            } else {
                nextRunner += 1;
            }
            if (nextRunner >= runners.size()) {
                nextRunner = 0;
            }
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

    private void fireIndexChanged(int oldIndex, int newIndex) {
        changeSupport.firePropertyChange(PROP_INDEX, oldIndex, newIndex);
    }
    
    private void firePointerChanged(int oldPointer, int newPointer) {
        changeSupport.firePropertyChange(PROP_POINTER, oldPointer, newPointer);
    }
    
    public boolean addRuntimeListener(RuntimeListener e) {
        return listeners.add(e);
    }

    public RuntimeListener[] getRuntimeListeners() {
        return listeners.toArray(new RuntimeListener[listeners.size()]);
    }

    public boolean removeRuntimeListener(Object o) {
        return listeners.remove(o);
    }
    
    
    class Runner {

        private final int id;
        private final String name;
        
        private int oldPointer;
        private int pointer;
        private int index;
        
        private Runner(int pointer, int index) {
            this.pointer = pointer;
            this.index = index;
            
            oldPointer = -1;
            id = runnerNum++;
            name = "Runner" + id + "[" + pointer + "]";
            for (RuntimeListener listener : listeners) {
                listener.create(this);
            }
        }

        private Runner() {
            this(0, 0);
        }
        
        private void close() {
            for (RuntimeListener listener : listeners) {
                listener.close(this);
            }
        }

        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPointer() {
            return pointer;
        }
        
        public int getIndex() {
            return index;
        }
        
        private synchronized void singleStep() throws BFRuntimeException {
            firePointerChanged(oldPointer, pointer);
            oldPointer = pointer;
            int cmdPointer = -1;
            char command = (char) -1;
            try {
                while (pointer < program.length && !stopRunning) {
                    int oldIndex = index;
                    cmdPointer = pointer++;
                    command = program[cmdPointer];
                    switch (command) {
                        case '>':
                            if (++index >= memory.getSize()) {
                                index = 0;
                            }
                            fireIndexChanged(oldIndex, index);
                            return;
                        case '<':
                            if (--index < 0) {
                                index = memory.getSize() - 1;
                            }
                            fireIndexChanged(oldIndex, index);
                            return;
                        case '+':
                            memory.incCell(index);
                            return;
                        case '-':
                            memory.decCell(index);
                            return;
                        case '.': {
                            int value = memory.getCell(index);
                            try {
                                output.write(value);
                            } catch (IOException ex) {
                                BFRuntimeException bf = new BFRuntimeException(
                                        "write: " + ex, pointer, index, value, name);
                                bf.initCause(ex);
                                throw bf;
                            }
                            return;
                        }
                        case ',': {
                            int value;
                            try {
                                value = input.read();
                            } catch (IOException ex) {
                                BFRuntimeException bf = new BFRuntimeException(
                                        "read: " + ex, pointer, index, memory.getCell(index), name);
                                bf.initCause(ex);
                                throw bf;
                            }
                            memory.setCell(index, (short) value);
                            return;
                        }
                        case '[': {
                            int value = memory.getCell(index);
                            if ((strictLoop && value == 0) || (!strictLoop && value <= 0)) {
                                pointer = findLoopEnd();
                                if (pointer == program.length) 
                                    throw new BFRuntimeException("can' find loop end", 
                                            oldPointer, index, value, name);
                                pointer += 1;
                            }
                            return;
                        }
                        case ']': {
                            int value = memory.getCell(index);
                            if ((strictLoop && value != 0) || (!strictLoop && value > 0)) {
                                pointer = findLoopStart();
                                if (pointer == program.length) 
                                    throw new BFRuntimeException("can' find loop start", 
                                        oldPointer, index, value, name);
//                                pointer += 1;
                            }
                            return;
                        }
                        case ':': 
                            if (!strictMode) {
                                int value = memory.getCell(index);
                                int ch;
                                if (0 <= value && value <= 9) {
                                    ch = '0' + value;
                                } else {
                                    ch = ' ';
                                }
                                try {
                                    output.write(ch);
                                } catch (IOException ex) {
                                    BFRuntimeException bf = new BFRuntimeException(
                                            "write char: " + ex, pointer, index, value, name);
                                    bf.initCause(ex);
                                    throw bf;
                                }
                                return;
                            }
                            break;
                        case ';': 
                            if (!strictMode) {
                                int value;
                                try {
                                    value = input.read();
                                } catch (IOException ex) {
                                    BFRuntimeException bf = new BFRuntimeException("read char: " + ex, 
                                            pointer, index, memory.getCell(index), name);
                                    bf.initCause(ex);
                                    throw bf;
                                }
                                if ('0' <= value && value <= '9') {
                                    value -= '0';
                                } else if (value != -1) {
                                    value = 10;
                                }
                                memory.setCell(index, (short) value);
                                return;
                            }
                            break;
                        case '@':
                            if (!strictMode) {
                                pointer = program.length;
                                return;
                            }
                            break;
                        case '{':
                            if (!strictMode) {
                                int end = findForkEnd();
                                if (end == program.length) 
                                    throw new BFRuntimeException("can' find thread end", 
                                            oldPointer, index, memory.getCell(index), name);
                                runners.add(new Runner(pointer, index));
                                pointer = end + 1;
                                return;
                            }
                            break;
                        case '}':
                            if (!strictMode) {
                                pointer = program.length;  // terminate
                                return;
                            }
                            break;
                        default:
                            command = (char) -1;  // ignored
                            break;
                    }
                }
            } finally {
                if (command != (char) -1) {
                    for (RuntimeListener listener : listeners) {
                        listener.step(this, cmdPointer, command, index, memory.getCell(index));
                    }
                }
            }
        }
        
        private synchronized boolean terminated() {
            return pointer >= program.length;
        }
        
        private int findLoopStart() {
            Integer start = startMap.get(pointer);
            if (start != null) {
                return start;
            }
            int open = 0;
            for (int i = pointer-1; i >= 0; i--) {
                switch (program[i]) {
                    case ']':
                        open += 1;
                        break;
                    case '[':
                        open -= 1;
                        if (open == 0) {
                            startMap.put(pointer, i);
                            return i;
                        }
                        break;
                    default:
                        // ignore
                        break;
                }
            }
            return program.length;
        }
        
        private int findLoopEnd() {
            Integer end = endMap.get(pointer);
            if (end != null) {
                return end;
            }
            int open = 0;
            for (int i = pointer-1; i < program.length; i++) {
                switch (program[i]) {
                    case '[':
                        open += 1;
                        break;
                    case ']':
                        open -= 1;
                        if (open == 0) {
                            endMap.put(pointer, i);
                            return i;
                        }
                        break;
                    default:
                        // ignore
                        break;
                }
            }
            return program.length;
        }
        
        private int findForkEnd() {
            int open = 0;
            for (int i = pointer-1; i < program.length; i++) {
                switch (program[i]) {
                    case '{':
                        open += 1;
                        break;
                    case '}':
                        open -= 1;
                        if (open == 0) {
                            return i;
                        }
                        break;
                    default:
                        // ignore
                        break;
                }
            }
            return program.length;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
