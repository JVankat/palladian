package ws.palladian.helper.date;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ws.palladian.helper.RegExp;
import ws.palladian.preprocessing.normalization.DateNormalizer;

// TODO Move to Extraction package
/**
 * This class helps to transform and help with dates.
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class DateHelper {

    // shortcuts to the number of milliseconds of certain time spans
    // TODO there is java.util.concurrent.TimeUnit which provides units and conversion operations
    public static final long SECOND_MS = 1000;
    public static final long MINUTE_MS = 60 * SECOND_MS;
    public static final long HOUR_MS = 60 * MINUTE_MS;
    public static final long DAY_MS = 24 * HOUR_MS;
    public static final long WEEK_MS = 7 * DAY_MS;
    public static final long MONTH_MS = 30 * DAY_MS;
    public static final long YEAR_MS = 365 * DAY_MS;

    public static boolean containsDate(String searchString) {
        Pattern pat = null;
        try {
            pat = Pattern.compile(RegExp.DATE_ALL);
        } catch (PatternSyntaxException e) {
            org.apache.log4j.Logger.getRootLogger().error(
                    "PatternSyntaxException for " + searchString + " with regExp " + RegExp.DATE_ALL, e);
            return false;
        }
        Matcher m = pat.matcher(searchString);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static String getCurrentDatetime(String format) {
        return getDatetime(format, System.currentTimeMillis());
    }

    public static String getDatetime(long timestamp) {
        return getDatetime("yyyy-MM-dd HH:mm:ss", timestamp);
    }

    public static String getDatetime(String format, long timestamp) {
        LocalizeHelper.setLocaleEnglish();
        // LocalizeHelper.setUTC();

        DateFormat dfm = new SimpleDateFormat(format);
        String dateTime = dfm.format(new Date(timestamp));

        LocalizeHelper.restoreLocale();
        // LocalizeHelper.restoreTimeZone();
        return dateTime;
    }

    /**
     * Get the number of hours, minutes, seconds, or milliseconds that passed on the given day from midnight.
     * 
     * @param date The date of the day including time.
     * @param resolution The resolution (Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND or Calendar.MILLISECOND)
     * 
     * @return A positive number of the passed time.
     */
    public static long getTimeOfDay(Date date, int resolution) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE) + 60 * hours;
        int seconds = calendar.get(Calendar.SECOND) + 60 * minutes;
        int milliseconds = calendar.get(Calendar.MILLISECOND) + 1000 * seconds;

        switch (resolution) {
            case Calendar.HOUR:
                return hours;
            case Calendar.MINUTE:
                return minutes;
            case Calendar.SECOND:
                return seconds;
            case Calendar.MILLISECOND:
                return milliseconds;
        }

        return milliseconds;
    }

    public static long getTimeOfDay(long timestamp, int resolution) {
        return getTimeOfDay(new Date(timestamp), resolution);
    }

    /**
     * Return the current date as a string with the format "yyyy-MM-dd_HH-mm-ss".
     * 
     * @return The date as a string.
     */
    public static String getCurrentDatetime() {
        return getCurrentDatetime("yyyy-MM-dd_HH-mm-ss");
    }

    public static String monthNameToNumber(String monthName) {
        monthName = monthName.toLowerCase().trim();

        if (monthName.equals("january") || monthName.equals("jan")) {
            return "01";
        } else if (monthName.equals("february") || monthName.equals("feb")) {
            return "02";
        } else if (monthName.equals("march") || monthName.equals("mar")) {
            return "03";
        } else if (monthName.equals("april") || monthName.equals("apr")) {
            return "04";
        } else if (monthName.equals("may")) {
            return "05";
        } else if (monthName.equals("june") || monthName.equals("jun")) {
            return "06";
        } else if (monthName.equals("july") || monthName.equals("jul")) {
            return "07";
        } else if (monthName.equals("august") || monthName.equals("aug")) {
            return "08";
        } else if (monthName.equals("september") || monthName.equals("sep")) {
            return "09";
        } else if (monthName.equals("october") || monthName.equals("oct")) {
            return "10";
        } else if (monthName.equals("november") || monthName.equals("nov")) {
            return "11";
        } else if (monthName.equals("december") || monthName.equals("dec")) {
            return "12";
        }

        // no valid month name given
        return "";
    }

    /**
     * Returns the time that passed since the start time.
     * 
     * @param startTime A timestamp.
     * @return The passed time since the time of the timestamp. The format is Hh:Mm:Ss:YYYms.
     */
    public static String getRuntime(long startTime) {
        return getRuntime(startTime, System.currentTimeMillis(), false);
    }

    public static String getRuntime(long startTime, long stopTime) {
        return getRuntime(startTime, stopTime, false);
    }

    public static String getRuntime(long startTime, long stopTime, boolean output) {
        long seconds = (stopTime - startTime) / 1000;
        long hours = seconds / 3600;
        seconds = seconds % 3600;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long millis = (stopTime - startTime) % 1000;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h:");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(minutes).append("m:");
        }
        if (hours > 0 || minutes > 0 || seconds > 0) {
            sb.append(seconds).append("s:");
        }

        sb.append(millis).append("ms");

        if (output) {
            System.out.println(":::: runtime: " + sb);
        }

        return sb.toString();
    }

    /**
     * Get interval in millisecond between two dates. Dates are not checked correct order: in case intervalStartTime >
     * intervalStopTime, a negative value is returned. In case date(s) are <code>null</code>, 0 is returned.
     * 
     * @param intervalStartTime the older date.
     * @param intervalStopTime the newer date.
     * @return interval in millisecond between two Dates. In case date(s) are <code>null</code>, 0 is returned.
     */
    public static long getIntervalLength(Date intervalStartTime, Date intervalStopTime) {
        long intervalLength = 0;
        if (intervalStartTime != null && intervalStopTime != null) {
            intervalLength = intervalStopTime.getTime() - intervalStartTime.getTime();
        }
        return intervalLength;
    }

    public static String getTimeString(long time) {
        return getRuntime(0, time);
    }

    /**
     * Create the UNIX timestamp for the given date (UTC).
     * 
     * @param normalizedDate A date in normalized form: yyyy-MM-dd [hh:mm:ss[.f]]
     * @return The UNIX timestamp for that date.
     */
    public static long getTimestamp(String date) throws Exception {
        LocalizeHelper.setUTCandEnglish();

        String normalizedDate = DateNormalizer.normalizeDate(date, true);
        long timestampUTC = Timestamp.valueOf(normalizedDate).getTime();

        LocalizeHelper.restoreTimeZoneAndLocale();
        return timestampUTC;
    }

    /**
     * Formats a given {@link Date} to ISO8601 "yyyy-MM-dd'T'HH:mm:ss+HH:mm.S", using the given {@link TimeZone}
     * <p>
     * examples: <br />
     * "2011-02-16T17:32:35.300+01:00" for {@link TimeZone} "Europe/Berlin" and showMillisecond=<code>true</code><br />
     * "2011-02-16T17:32:35+01:00" for {@link TimeZone} "Europe/Berlin" and showMillisecond=<code>false</code><br />
     * "2011-02-16T17:32:35+00:00" for {@link TimeZone} "etc/UTC" and showMillisecond=<code>false</code>
     * </p>
     * 
     * @param date The date to format.
     * @param timeZoneS The {@link TimeZone} the date is in.
     * @param showMillisecond Show millisecond if provided by {@link Date}.
     * @return The formatted date
     * @see http://biese.wordpress.com/2006/10/11/xml-schema-datetime-data-type/
     */
    public static String getISO8601DateTime(final Date date, final TimeZone timeZone, final boolean showMillisecond) {

        String precisionS = "";
        if (showMillisecond) {
            precisionS = ".S";
        }

        SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss" + precisionS);
        ISO8601Local.setTimeZone(timeZone);
        DecimalFormat twoDigits = new DecimalFormat("00");

        int offset = ISO8601Local.getTimeZone().getOffset(date.getTime());
        String sign = "+";
        if (offset < 0) {
            offset = -offset;
            sign = "-";
        }
        int hours = offset / (int) HOUR_MS;
        int minutes = (offset - hours * (int) HOUR_MS) / (int) MINUTE_MS;
        // As things stand any odd seconds in the offset are silently truncated.
        // Uncomment the next 5 lines to detect that rare circumstance.
        // if (offset != hours * 3600000 + minutes * 60000) {
        // // E.g. TZ=Asia/Riyadh87
        // throw new RuntimeException(“TimeZone offset (” + sign + offset +
        // ” ms) is not an exact number of minutes”);
        // }
        String ISO8601Now = ISO8601Local.format(date) + sign + twoDigits.format(hours) + ":"
                + twoDigits.format(minutes);
        return ISO8601Now;
    }

    /**
     * Checks whether a date's year exceeds the given maximum. Useful to store a date in a mysql database since the
     * maximum value of the DATETIME type is the year 9999.
     * 
     * @param date date to check.
     * @param maxYear maximum year allowed.
     * @return The given date if it's year <= maxYear or <code>null</code> if date == null or its year > maxYear.
     */
    public static Date validateYear(Date date, int maxYear) {
        Date validatedDate = date;
        if (date != null) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            if (year >= maxYear) {
                validatedDate = null;
            }
        }
        return validatedDate;
    }

    public static void main(String[] t) {
        System.out.println(DateHelper.getCurrentDatetime());
        System.out.println(getTimeString(-1));
        System.out.println(getCurrentDatetime("yyyy-MM-dd HH:mm:ss"));
        System.out.println(getCurrentDatetime());
        System.out.println(getDatetime("dd.MM.yyyy", 1274313600000l));
        /*
         * long t1 = System.currentTimeMillis(); for (int i = 0; i < 94353; i++) { System.out.println("."); }
         * DateHelper.getRuntime(t1,true);
         */
    }

}