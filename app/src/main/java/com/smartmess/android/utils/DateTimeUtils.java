package com.smartmess.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final String ISO_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DISPLAY_FORMAT = "dd MMM, yyyy";

    public static String currentDate() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date());
    }

    public static String tomorrowDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(cal.getTime());
    }

    public static String nowIso() {
        return new SimpleDateFormat(ISO_FORMAT, Locale.US).format(new Date());
    }

    public static String getCurrentDate() {
        return currentDate();
    }

    public static String getCurrentDateTime() {
        return nowIso();
    }

    public static String getCurrentMonthStart() {
        return currentMonthStart(1);
    }

    public static String getCurrentMonthEnd() {
        return currentMonthEnd(1);
    }

    public static String currentMonthStart(int startDay) {
        Calendar cal = Calendar.getInstance();
        if (startDay <= 0 || startDay > 28) startDay = 1;
        cal.set(Calendar.DAY_OF_MONTH, startDay);
        // If current day is before cycle start day, the cycle started last month
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < startDay) {
            cal.add(Calendar.MONTH, -1);
        }
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(cal.getTime());
    }

    public static String currentMonthEnd(int startDay) {
        Calendar cal = Calendar.getInstance();
        if (startDay <= 0 || startDay > 28) startDay = 1;
        cal.set(Calendar.DAY_OF_MONTH, startDay);
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < startDay) {
            cal.add(Calendar.MONTH, -1);
        }
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(cal.getTime());
    }

    public static String formatDisplayDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            Date date = new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(dateStr);
            if (date != null) {
                return new SimpleDateFormat(DISPLAY_FORMAT, Locale.US).format(date);
            }
        } catch (ParseException ignored) {}
        return dateStr;
    }

    /**
     * Checks if current time is past cutoff time (e.g., "22:00:00")
     */
    public static boolean isPastCutoffTime(String cutoffTime) {
        if (cutoffTime == null || cutoffTime.trim().isEmpty()) {
            cutoffTime = "22:00:00";
        }
        try {
            SimpleDateFormat tf = new SimpleDateFormat(TIME_FORMAT, Locale.US);
            Date cutoff = tf.parse(cutoffTime);
            String currentStr = tf.format(new Date());
            Date now = tf.parse(currentStr);
            if (cutoff != null && now != null) {
                return now.after(cutoff);
            }
        } catch (ParseException ignored) {}
        return false;
    }
}
