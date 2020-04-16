package ph.chits.rxbox.lifeline.util;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicPseudoFloat extends AtomicInteger {
    public final void setFloat(float newValue) {
        super.set(Float.floatToIntBits(newValue));
    }

    public final float getFloat() {
        return Float.intBitsToFloat(super.get());
    }
}
