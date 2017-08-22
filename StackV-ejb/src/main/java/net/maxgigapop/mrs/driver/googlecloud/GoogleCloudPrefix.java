/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

/**
 *
 * @author raymonddsmith
 */
public class GoogleCloudPrefix {
    public static String prefix = "google.com:google-cloud";
    
    public static String project =  prefix+":project+%s";
    public static String vpc =      prefix+":project+%s:vpc+%s";
    public static String subnet =   prefix+":project+%s:vpc+%s:subnet+%s";
    public static String instance = prefix+":project+%s:vpc+%s:subnet+%s:instance+%s";
    
}
