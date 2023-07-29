package ph.chits.rxbox.lifeline.model;

import java.util.Collection;
import java.util.List;

public interface SensorData<T> {
    public boolean hasData();
    public T getData(int index);
    public T getLatestData();
    public List<T> getBulk(Integer count);
    public void addData(T data);
    public void addData(Collection<T> data);
}
