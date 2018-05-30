package com.lamanchy.verygoodalarmclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Mozna by bylo fajn vytvorit si par metod jako getInt() a setString() a pak je pouzivat misto sharedPreferences.edit().putInt().apply(), zprehledni to kod.
 *
 * Zamyslel bych se nad poradim metod. Public nahoru, private dolu. Kdyz prijdu do tridy, zajimaji me public metody a nechci pro ne scrollovat na konec tridy.
 */
public class CustomPreferences {
    private SharedPreferences sharedPreferences;
    private String prefix;

    CustomPreferences(Context context, String prefix) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private Integer getDefaultTimeValue() {
        switch (prefix) {
            case Enums.MORNIN_PREFIX:
                return 7 * 60;
            case Enums.EVENIN_PREFIX:
                return 23 * 60;
        }
        return 0;
    }

    private String getTimeId(String type) {
        return prefix + "time" + type;
    }

    private String getEnabledId(String type) {
        return prefix + "enabled" + type;
    }

    // Potrebujes vracet Integer? U setteru staci void, pokud neni duvod vracet neco jineho. V tomto pripade je Integer zbytecny a rika ti to i Lint.
    // Jak uz jsem rikal na cviku, kdyz muzes, nikdy nepouzivej objektovou reprezentaci primitivnich typu, pokud nemusis. Zabira to fakt moc mista v pameti.
    public Integer setTime(String type, Integer time) {
        sharedPreferences.edit().putInt(getTimeId(type), time).apply();
        return time;
    }

    public Integer getTime(String type) {
        return sharedPreferences.getInt(getTimeId(type), getDefaultTimeValue()); // 7:00 1970
    }

    @SuppressLint("DefaultLocale")
    public String getTimeAsString(String type) {
        Integer time = getTime(type);       // SimpleDateFormat, ne tohle
        return String.format("%02d", time / 60) + ':' + String.format("%02d", time % 60);
    }

    // this method makes sure, that the state of preferences is never wrong
    public Boolean setEnabled(String type, Boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!type.equals(Enums.ONE_TIME_OFF)) {
            editor.putBoolean(getEnabledId(Enums.ONE_TIME_OFF), false);
            editor.putBoolean(getEnabledId(type), enabled);
        } else {
            if (!getEnabled(Enums.REGULAR_ALARM) &&
                    !getEnabled(Enums.ONE_TIME_ALARM)) {
                throw new IllegalStateException("Cant turn one time off on");
            }

            editor.putBoolean(getEnabledId(Enums.ONE_TIME_ALARM), false);

            if (getEnabled(Enums.REGULAR_ALARM)) {
                editor.putBoolean(getEnabledId(type), enabled);
            }
        }
        editor.apply();
        return getEnabled(type);
    }

    public Boolean getEnabled(String type) {
        return sharedPreferences.getBoolean(getEnabledId(type), false);
    }

    public Boolean flipEnabled(String type) {
        return setEnabled(type, !getEnabled(type));
    }
}
