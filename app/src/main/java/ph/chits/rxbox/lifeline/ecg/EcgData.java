package ph.chits.rxbox.lifeline.ecg;

import java.nio.IntBuffer;

public class EcgData {
    private IntBuffer buffer;

    public EcgData(int limit) {
        buffer = IntBuffer.allocate(limit);
    }

    public void add(IntBuffer buf) {

    }
}
