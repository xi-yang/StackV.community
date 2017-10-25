/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

/**
 *
 * @author Adam Smith
 */

public class GoogleCloudPrefix {
    public static final String prefix = "google.com:google-cloud";

    public static final String vpcService =           prefix+":vpcService+%s";
    public static final String computeService =       prefix+":computeService+%s";
    public static final String blockStorageService =  prefix+":blockStorageService+%s";
    public static final String objectStorageService = prefix+":objectStorageService+%s";
    public static final String routingService =       prefix+":vpc+%s:routingService+%s";
    
    public static final String vpc =                  prefix+":vpc+%s";
    public static final String subnet =               prefix+":vpc%s:region+%s:subnet+%s";
    public static final String internetGateway =      prefix+":vpc+%s:internetGateway"; //for now, each vpc can only have one internet gateway
    public static final String instance =             prefix+":zone+%s:instance+%s";
    public static final String nic =                  prefix+":vpc+%s:instance+%s:nic+%s";
    public static final String nicNetworkAddress =    prefix+":vpc+%s:region+%s:subnet+%s:nic+%s:ip+%s";
    public static final String volume =               prefix+":zone+%s:volume+%s";
    public static final String routingTable =         prefix+":vpc+%s:routingTable";
    public static final String route =                prefix+":vpc+%s:route+%s";
    public static final String bucket =               prefix+":bucket+%s";
}
