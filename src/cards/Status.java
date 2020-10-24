package cards;

import java.io.Serializable;

public class Status implements Serializable {
    private static final long serialVersionUID = 1L;
    public int block;
    public int duration;

    public Status(int block) {
        this.block = block;
        this.duration = 2;
    }
}
