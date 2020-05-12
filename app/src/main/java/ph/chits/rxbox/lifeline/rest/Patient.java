package ph.chits.rxbox.lifeline.rest;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Patient {
    private final String TAG = this.getClass().getSimpleName();

    String id, gender, birthdate;
    List<Identifier> identifier;
    List<Name> name;

    public static final String BIRTHDATE_FORMAT = "dd/MM/yyyy";

    public static class Identifier {
        String value, system;
    }

    public static class Name {
        String use, text;
    }

    public String getName() {
        for (Name n : name) {
            if (n.use.equalsIgnoreCase("official")) {
                return n.text;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthdate() {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(BIRTHDATE_FORMAT, Locale.US);
            if (birthdate == null) return null;
            return simpleDateFormat.parse(birthdate);
        } catch (ParseException e) {
            Log.d(TAG, "failed to parse birthdate " + birthdate, e);
            return null;
        }
    }
}
