package ph.chits.rxbox.lifeline.rest;

import java.util.List;

public class Bundle<K> {
    public String type;
    public Integer total;
    public List<DeviceRequest> entry;
}
