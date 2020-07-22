package cfh.bf;

import cfh.bf.Runtime.Runner;

public interface RuntimeListener {

    public void create(Runner runner);
    
    public void close(Runner runner);
    
    public void step(Runner runner, int pointer, char command, int index, int value);
}
