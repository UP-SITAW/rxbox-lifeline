package ph.chits.rxbox.lifeline.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StringUtils {
    public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public static String formatISO(Date date) {
        if (date == null) return null;
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat(ISO8601_PATTERN, Locale.US);
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static Date parseISO(String string) {
        if (string == null) return null;
        DateFormat df = new SimpleDateFormat(ISO8601_PATTERN, Locale.US);
        try {
            return df.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
