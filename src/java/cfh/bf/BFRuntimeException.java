package cfh.bf;

public class BFRuntimeException extends Exception {

    private final int pointer;
    private final int index;
    private final int value;
    private final String name;

    public BFRuntimeException(String message, int pointer, int index, int value, String name) {
        super(message);
        this.pointer = pointer;
        this.index = index;
        this.value = value;
        this.name = name;
    }

    public int getPointer() {
        return pointer;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        String string = super.toString() + " at pointer:" + pointer + " index:" + index;
        if (getCause() != null) {
            string += "\nCaused by: " + getCause();
        }
        return string;
    }
}
