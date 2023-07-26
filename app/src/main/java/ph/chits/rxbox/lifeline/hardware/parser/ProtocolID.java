package ph.chits.rxbox.lifeline.hardware.parser;

public enum ProtocolID {

    ECG_WAVEFORM_I_II_V1_RESP(0x01, 7),
    ECG_WAVEFORM_I_II_V1(0x02, 6),
    ECG_WAVEFORM_V2_TO_V6(0x12, 7),
    ECG_HEART_RATE_RESPIRATION_RATE(0x04, 6),
    ECG_LEAD_CONNECTIONS_INFO_1(0x09, 3),
    ECG_LEAD_CONNECTIONS_INFO_2(0x13, 3),
    ECG_BOARD_RESET(0x0E, 1),
    ECG_TEMPERATURE_AND_PROBE(0x11, 7),
    PULSE_OXIMETER(0x21, 7),
    BP_CUFF_TX_2(0x32, 4),
    BP_END_CUFF_TX(0x33, 7),
    BP_PART_1(0x34, 9),
    BP_STATUS_2(0x35, 9),
    BP_STATUS_3(0x36, 9),
    BP_STATUS_4(0x37, 9),
    BP_STATUS_5(0x38, 9),
    BP_STATUS_6(0x39, 8),
    FM(0x40, 8);

    private final int value;
    private final int length;

    private ProtocolID(int value, int length) {
        this.value = value;
        this.length = length;
    }

    public static ProtocolID get(int value) {
        for(ProtocolID a : ProtocolID.values()) {
            if (a.getValue() == value) {
                return a;
            }
        }
        return null;
    }

    public int getValue() {
        return this.value;
    }

    public int getLength() {
        return length;
    }
}
