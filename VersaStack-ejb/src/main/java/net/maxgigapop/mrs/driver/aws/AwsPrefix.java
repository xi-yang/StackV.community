/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

/**
 *
 * @author muzcategui
 */
public class AwsPrefix {
    public static String prefix = "aws.amazon.cloud:aws-cloud";
    //@TODO: dynamic pattern prefix
    public static String bucket = prefix+":bucket+%s";
    
    public static String ebsService = prefix+":ebsservice+%s";

    public static String ec2Service = prefix+":ec2service+%s";

    public static String directConnectService = prefix+":directconnect+%s";

    public static String gateway = prefix+":gateway+%s";

    public static String instance = prefix+":vpc+%s:subnet+%s:instance+%s";

    public static String label = prefix+":vif+%s:label+%s";

    public static String labelGroup = prefix+":vif+%s:labelgroup+%s";
    
    public static String nic = prefix+":vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = prefix+":vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    public static String publicAddress = prefix+":public-address";

    public static String route = prefix+":vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = prefix+":vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = prefix+":vpc+%s:routingtable+%s:route+%s:routeto";

    public static String routingService = prefix+":vpc+%s:routingservice";

    public static String routingTable = prefix+":vpc+%s:routingtable+%s";

    public static String s3Service = prefix+":s3service+%s";

    public static String subnet = prefix+":vpc+%s:subnet+%s";

    public static String subnetNetworkAddress = prefix+":vpc+%s:subnet+%s:cidr";

    public static String switchingService = prefix+":vpc+%s:switchingservice";

    public static String vif = prefix+":vif+%s";

    public static String vlan = prefix+":vif+%s:vlan+%s";

    public static String volume = prefix+":volume+%s";

    public static String vpc = prefix+":vpc+%s";

    public static String vpcNetworkAddress = prefix+":vpc+%s:cidr";

    public static String vpcService = prefix+":vpcservice+%s";

}
