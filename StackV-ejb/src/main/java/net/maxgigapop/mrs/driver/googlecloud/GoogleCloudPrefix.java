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
    public static String prefix = "google.com:google-cloud";

    public static String vpcService =     prefix+":vpcService+%s";    
    public static String computeService = prefix+":computeService+%s";
    public static String storageService = prefix+":storageService+%s";
    public static String routingService = prefix+":routingService+%s";

    public static String vpc =            prefix+":vpc+%s";
    public static String subnet =         prefix+":vpc+%s:region+%s:subnet+%s";
    public static String internetGateway =prefix+":vpc+%s:internetGateway"; //for now, each vpc can only have one internet gateway
    public static String instance =       prefix+":vpc+%s:instance+%s"; //todo: put zone into instance uri
    public static String volume =         prefix+":instance+%s:volume+%s";
    public static String routingTable =   prefix+":routingTable+%s";
    public static String route =          prefix+":vpc+%s:route+%s";

}
