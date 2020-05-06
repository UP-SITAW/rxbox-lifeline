package ph.chits.rxbox.lifeline.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StringFormatter {
    public static String formatISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone("HK");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+08:00"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(date);
    }
}
