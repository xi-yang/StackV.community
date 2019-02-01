/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author xyang
 */
public class DateTimeUtil {

    public static String longToDateString(long miliseconds) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
        return dateFormat.format(new Date(miliseconds)).toString();
    }

    public static Date stringToDate(String timestr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
        return dateFormat.parse(timestr);
    }

    public static long dateStringToLong(String timestr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
        return dateFormat.parse(timestr).getTime();
    }

    public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
        if (time < 0) {
            throw new DatatypeConfigurationException("Illegal time value specified " + time);
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        return newXMLGregorianCalendar;
    }

    public static XMLGregorianCalendar xmlGregorianCalendar() throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public static XMLGregorianCalendar xmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public static XMLGregorianCalendar xmlGregorianCalendar(String date) throws DatatypeConfigurationException {
        DateTime dt = ISODateTimeFormat.dateTimeParser().parseDateTime(date);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dt.toDate());
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public static Date xmlGregorianCalendarToDate(XMLGregorianCalendar cal) throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = cal.toGregorianCalendar();
        return gregorianCalendar.getTime();
    }

    public static long getBandwidthScheduleSeconds(String time) throws Exception {
        if (time.equalsIgnoreCase("now")) {
            return new GregorianCalendar().getTimeInMillis() / 1000L;
        }
        try {
            XMLGregorianCalendar xgc = xmlGregorianCalendar(time);
            Date dt = xmlGregorianCalendarToDate(xgc);
            return dt.getTime() / 1000L;
        } catch (java.lang.IllegalArgumentException ex) {
            ;
        }
        if (time.startsWith("+")) {
            String[] fields = time.substring(1).split(":");
            long seconds = 0;
            for (String field : fields) {
                field = field.toLowerCase();
                long factor;
                if (field.endsWith("d")) {
                    factor = 60 * 60 * 24;
                } else if (field.endsWith("h")) {
                    factor = 60 * 60;
                } else if (field.endsWith("m")) {
                    factor = 60;
                } else if (field.endsWith("s")) {
                    factor = 1;
                } else {
                    throw new Exception("malformed bandwidth schedule time:" + time);
                }
                field = field.substring(0, field.length() - 1);
                seconds += factor * Long.parseLong(field);
            }
            return seconds;
        }
        throw new Exception("malformed bandwidth schedule time:" + time);
    }

    public static long getBandwidthScheduleSeconds_Obsolute(String time) throws Exception {
        long secs = getBandwidthScheduleSeconds(time);
        if (time.startsWith("+")) {
            secs += (new GregorianCalendar().getTimeInMillis() / 1000L);
        }
        return secs;
    }
}
