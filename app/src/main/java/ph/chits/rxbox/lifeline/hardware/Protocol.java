package ph.chits.rxbox.lifeline.hardware;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class Protocol {
    public static final class Serial {
        public static final int BAUDRATE = 256000;
        public static final int DATABITS = 8;
        public static final int STOP_BITS = UsbSerialPort.STOPBITS_1; // 1 stop bit
        public static final int PARITY = UsbSerialPort.PARITY_NONE; // no parity
    }

    public final static char SET_ECG_FILTER_TO_DIAGNOSIS = 'a';
    public final static char SET_ECG_FILTER_TO_MONITOR = 'b';
    public final static char SET_ECG_FILTER_TO_SURGERY = 'c';
    public final static char SET_ECG_FILTER_TO_STRONG = 'd';
    public final static char SET_ECG_NOTCH_FILTER_TO_OFF = 'e';
    public final static char SET_ECG_NOTCH_FILTER_TO_60HZ = 'f';
    public final static char SET_ECG_NOTCH_FILTER_TO_50HZ = 'g';
    public final static char SET_ECG_GAIN_I_TO_FOURTH = 'h';
    public final static char SET_ECG_GAIN_I_TO_HALF = 'i';
    public final static char SET_ECG_GAIN_I_TO_ONE = 'j';
    public final static char SET_ECG_GAIN_I_TO_TWO = 'k';
    public final static char SET_ECG_GAIN_II_TO_FOURTH = 'l';
    public final static char SET_ECG_GAIN_II_TO_HALF = 'm';
    public final static char SET_ECG_GAIN_II_TO_ONE = 'n';
    public final static char SET_ECG_GAIN_II_TO_TWO = 'o';
    public final static char SET_ECG_GAIN_3_TO_FOURTH = 'F';
    public final static char SET_ECG_GAIN_3_TO_HALF = 'G';
    public final static char SET_ECG_GAIN_3_TO_ONE = 'H';
    public final static char SET_ECG_GAIN_3_TO_TWO = 'I';

    public final static char SET_TEMP_PROBE_TWO_K = 'J';
    public final static char SET_TEMP_PROBE_TEN_K = 'K';

    public final static char SET_ECG_ACKNOWLEGE_RESET = 'p';

    public final static char BP_REQUEST_DATA = 'q';
    public final static char SET_BP_MANUAL_MODE = 'r';
    public final static char BP_POWER_DOWN = 's';
    public final static char SET_BP_ADULT_MEASURING = 't';
    public final static char SET_BP_PRESSURE_ADULT_80 = 'u';
    public final static char SET_BP_PRESSURE_ADULT_100 = 'v';
    public final static char SET_BP_PRESSURE_ADULT_120 = 'w';
    public final static char SET_BP_PRESSURE_ADULT_140 = 'x';
    public final static char SET_BP_PRESSURE_ADULT_160 = 'y';
    public final static char SET_BP_PRESSURE_ADULT_180 = 'z';
    public final static char SET_BP_PRESSURE_ADULT_200 = 'A';
    public final static char SET_BP_PRESSURE_ADULT_220 = 'B';
    public final static char SET_BP_PRESSURE_ADULT_240 = 'C';
    public final static char BP_START_MEASUREMENT = 'D';
    public final static char BP_ABORT = 'E';

    public final static char FM_RESET_TOCO_ZERO = 'L';
    public final static char FM_VOLUME_0 = 'M';
    public final static char FM_VOLUME_1 = 'N';
    public final static char FM_VOLUME_2 = 'O';
    public final static char FM_VOLUME_3 = 'P';
    public final static char FM_VOLUME_4 = 'Q';
    public final static char FM_VOLUME_5 = 'R';
    public final static char FM_VOLUME_6 = 'S';
    public final static char FM_VOLUME_7 = 'T';

    public final static int ID_ECG_WAVEFORM_I_II_V1_RESP = 0x01;
    public final static int ID_ECG_WAVEFORM_I_II_V1 = 0x02;
    public final static int ID_ECG_WAVEFORM_V2_TO_V6 = 0x12;
    public final static int ID_ECG_HEART_RATE_RESPIRATION_RATE = 0x04;
    public final static int ID_ECG_LEAD_CONNECTIONS_INFO_1 = 0x09;
    public final static int ID_ECG_LEAD_CONNECTIONS_INFO_2 = 0x13;
    public final static int ID_ECG_BOARD_RESET = 0x0E;
    public final static int ID_ECG_TEMPERATURE_AND_PROBE = 0x11;

    public final static int ID_PULSE_OXIMETER = 0x21;

    public final static int ID_BP_CUFF_TX_2 = 0x32;
    public final static int ID_BP_END_CUFF_TX = 0x33;
    public final static int ID_BP_PART_1 = 0x34;
    public final static int ID_BP_STATUS_2 = 0x35;
    public final static int ID_BP_STATUS_3 = 0x36;
    public final static int ID_BP_STATUS_4 = 0x37;
    public final static int ID_BP_STATUS_5 = 0x38;
    public final static int ID_BP_STATUS_6 = 0x39;

    public final static int ID_FM = 0x40;

    public static boolean isIdentifier(int r) {
        return (r & 0x80) == 0x00; // byte is identifier if bit 7 is 0
    }

    public static int lengthOfPacket(int packet_id) {
        switch (packet_id) {
            case ID_ECG_WAVEFORM_I_II_V1_RESP:
            case ID_ECG_TEMPERATURE_AND_PROBE:
            case ID_ECG_WAVEFORM_V2_TO_V6:
            case ID_PULSE_OXIMETER:
            case ID_BP_END_CUFF_TX:
                return 7;
            case ID_ECG_WAVEFORM_I_II_V1:
            case ID_ECG_HEART_RATE_RESPIRATION_RATE:
                return 6;
            case ID_ECG_LEAD_CONNECTIONS_INFO_1:
            case ID_ECG_LEAD_CONNECTIONS_INFO_2:
                return 3;
            case ID_ECG_BOARD_RESET:
                return 1;
            case ID_BP_CUFF_TX_2:
                return 4;
            case ID_BP_PART_1:
            case ID_BP_STATUS_2:
            case ID_BP_STATUS_3:
            case ID_BP_STATUS_4:
            case ID_BP_STATUS_5:
                return 9;
            case ID_BP_STATUS_6:
            case ID_FM:
                return 8;
            default:
                return 0;
        }
    }

    public static boolean readBit(int input, int bit_number) {
        int detector = 0x01 << bit_number;
        return (input & detector) == detector;
    }

    public static void restoreBitSeven(int[] array) {
        int length = lengthOfPacket(array[0]);
        for (int i = 2; i < length; i++) {
            if (!readBit(array[1], i - 2)) {
                array[i] &= ~0x80;
            }
        }
    }
}
