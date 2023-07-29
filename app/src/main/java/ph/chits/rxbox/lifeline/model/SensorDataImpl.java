package ph.chits.rxbox.lifeline.model;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class SensorDataImpl<T> implements SensorData<T> {

    private static final int DEFAULT_MAX_SAMPLES = 4800;
    private int maxSample = DEFAULT_MAX_SAMPLES;
    private ArrayList<T> data = new ArrayList<>();

    public void setMaxSample(int maxSample) {
        this.maxSample = maxSample;
    }

    public int getMaxSample() {
        return maxSample;
    }

    private final Object mutex = new Object();

    @Override
    public boolean hasData() {
        return !this.data.isEmpty();
    }

    @Override
    public T getData(int index) {
        // silent implementation. meaning it just throw null when index out of bounds.
        try {
            return this.data.get(index);
        } catch (IndexOutOfBoundsException e) {
            // do nothing
        }
        return null;
    }

    @Override
    public T getLatestData() {
        return hasData() ? this.data.get(this.data.size() - 1) : null;
    }

    @Override
    public List<T> getBulk(@Nullable Integer count) {
        synchronized (mutex) {
            if (!this.hasData()) {
                return new ArrayList<>();
            }
            if (count == null) {
                count = this.maxSample;
            }
            int size = this.data.size();
            if (count < 0) {
                // return data from the end of array if count is negative
                int startIndex = size + count;
                startIndex = Math.max(startIndex, 0);
                ArrayList<T> hardCopySubList = new ArrayList<>();
                for (int x = 0; x < -(count); x++) {
                    int index = startIndex + x;
                    if (index >= this.data.size()) {
                        break;
                    }
                    hardCopySubList.add(this.data.get(index));
                }
                return hardCopySubList;
            } else {
                // return data from the start of the array
                ArrayList<T> hardCopySubList = new ArrayList<>();
                for (int x = 0; x < count && x < this.data.size(); x++) {
                    hardCopySubList.add(this.data.get(x));
                }
                return hardCopySubList;
            }
        }
    }

    @Override
    public void addData(T data) {
        synchronized (this.mutex) {
            if (this.data.size() >= this.maxSample) {
                this.data.remove(0);
            }
            this.data.add(data);
        }
    }

    @Override
    public void addData(Collection<T> data) {
        for (T datum : data) {
            this.addData(datum);
        }
    }
}
