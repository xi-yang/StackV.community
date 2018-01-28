/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author xyang
 */
public class AddressUtil {

    public static long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {

            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);

        }
        return result;
    }
    
    public static String ipToString(Long ip, Long mask) {
        return ((ip >> 24 ) & 0xFF) + "." +
               ((ip >> 16 ) & 0xFF) + "." +
               ((ip >>  8 ) & 0xFF) + "." +
               ( ip        & 0xFF) + "/" + mask;
    }
    
    // up to 256 IPs; first element is mask
    public static List<Long> ipMaskRangeToLongList(String ipRange) {
        long mask = 32;
        String[] ipRangeStartEnd = ipRange.split("-");
        if (ipRangeStartEnd.length != 2) {
            return null;
        }
        String[] tmpArray = ipRangeStartEnd[0].split("/");
        ipRangeStartEnd[0] = tmpArray[0];
        if (tmpArray.length > 1) {
            mask = Long.parseLong(tmpArray[1]);
        }
        tmpArray = ipRangeStartEnd[1].split("/");
        ipRangeStartEnd[1] = tmpArray[0];
        if (tmpArray.length > 1) {
            mask = Long.parseLong(tmpArray[1]);
        }
        List<Long> ipArray = new ArrayList<>();
        ipArray.add(mask);
        long ipStart = ipToLong(ipRangeStartEnd[0]);
        long ipEnd = ipToLong(ipRangeStartEnd[1]);
        if (ipEnd - ipStart > 255) {
            ipEnd = ipStart + 255;
        }
        for (long i = ipStart; i <= ipEnd; i++) {
            ipArray.add(i);
        }
        return ipArray;
    }

    // up to /24 ; first element is mask
    public static List<Long> ipPrefixRangeToLongList(String ipRange) {
        long mask = 32;
        String[] ipRangeMask = ipRange.split("/");
        if (ipRangeMask.length == 2) {
            mask = Long.parseLong(ipRangeMask[1]);;
        }
        if (mask < 23) {
            return null;
        }
        List<Long> ipArray = new ArrayList<>();
        ipArray.add(mask);
        long ipStart = ipToLong(ipRangeMask[0]);
        long ipEnd = ipStart + (long)Math.pow(2, 32-mask) - 1;
        if (ipEnd - ipStart > 1023) {
            ipEnd = ipStart + 1023;
        }
        for (long i = ipStart; i <= ipEnd; i++) {
            ipArray.add(i);
        }
        return ipArray;
    }

    
    // up to 1024 MACs
    public static long macToLong(String macAddress) {
        String[] macAddressInArray = macAddress.split("\\:");
        long result = 0;
        for (int i = 0; i < macAddressInArray.length; i++) {
            int power = 3 - i;
            int mac = Integer.parseInt(macAddressInArray[i], 16);
            result += mac * Math.pow(256, power);
        }
        return result;
    }

    public static String macToString(Long mac) {
        return Long.toHexString((mac >> 40 ) & 0xFF) + ":" +
               Long.toHexString((mac >> 32 ) & 0xFF) + ":" +
               Long.toHexString((mac >> 24 ) & 0xFF) + ":" +
               Long.toHexString((mac >> 16 ) & 0xFF) + ":"  +
               Long.toHexString((mac >> 8 ) & 0xFF) + ":" +
               Long.toHexString(mac  & 0xFF);
    }
    
    
    public static List<Long> macRangeToLongList(String ipRange) {
        String[] macRangeStartEnd = ipRange.split("-");
        if (macRangeStartEnd.length != 2) {
            return null;
        }
        List<Long> macArray = new ArrayList<>();
        long macStart = ipToLong(macRangeStartEnd[0]);
        long macEnd = ipToLong(macRangeStartEnd[1]);
        if (macEnd - macStart > 1023) {
            macEnd = macStart + 1023;
        }
        for (long i = macStart; i <= macEnd; i++) {
            macArray.add(i);
        }
        return macArray;
    }
    
    private static final String IPADDRESS_PATTERN =
		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
   
     private static final String MACADDRESS_PATTERN =
		"^([0-9A-Fa-f]{2}[\\\\.:-]){5}([0-9A-Fa-f]{2})$";

    public static boolean isIpAddress(String ip) {
        String[] ipArray = ip.split("/");
        if (ipArray.length == 2) {
            try {
                int mask = Integer.parseInt(ipArray[1]);
                if (mask > 32 || mask < 0) {
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
        } else if (ipArray.length > 2) {
            return false;
        } 
        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile(IPADDRESS_PATTERN);
     	matcher = pattern.matcher(ipArray[0]);
        return matcher.matches();
    }
    
    public static boolean isMacAddress(String mac) {
        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile(MACADDRESS_PATTERN);
     	matcher = pattern.matcher(mac);
        return matcher.matches();
    }
    
    public static boolean isIpAddressMaskRange(String ipRange) {
        String[] ipRangeArray = ipRange.split("-");
        if (ipRangeArray.length != 2) {
            return false;
        }
        if (isIpAddress(ipRangeArray[0]) && isIpAddress(ipRangeArray[1])) {
            return true;
        }
        return false;
    }
    
    public static boolean isIpAddressPrefixkRange(String ipRange) {
        return (isIpAddress(ipRange) && ipRange.contains("/"));
    }
    
    public static String getNameFromIp(String ip) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ip);
        } catch (UnknownHostException ex) {
            return null;
        }
        return addr.getHostName();
    }
    
    public static String getIpFromName(String fqdn) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(fqdn);
        } catch (UnknownHostException ex) {
            return null;
        }
        return addr.getHostAddress();
    }
}
