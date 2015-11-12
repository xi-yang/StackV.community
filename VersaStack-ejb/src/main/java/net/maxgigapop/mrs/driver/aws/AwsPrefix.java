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
    
    public static String bucket = "aws.amazon.cloud:bucket+%s";
    
    public static String ebsService = "aws.amazon.cloud:ebsservice+%s";

    public static String ec2Service = "aws.amazon.cloud:ec2service+%s";

    public static String directConnectService = "aws.amazon.cloud:directconnect+%s";

    public static String gateway = "aws.amazon.cloud:gateway+%s";

    public static String instance = "aws.amazon.cloud:vpc+%s:subnet+%s:instance+%s";

    public static String labelGroup = "aws.amazon.cloud:vif+%s:labelgroup+%s";

    public static String nic = "aws.amazon.cloud:vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = "aws.amazon.cloud:vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    public static String publicAddress = "aws.amazon.cloud:public-address";

    public static String route = "aws.amazon.cloud:vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = "aws.amazon.cloud:vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = "aws.amazon.cloud:vpc+%s:routingtable+%s:route+%s:routeto";

    public static String routingService = "aws.amazon.cloud:vpc+%s:routingservice";

    public static String routingTable = "aws.amazon.cloud:vpc+%s:routingtable+%s";

    public static String s3Service = "aws.amazon.cloud:s3service+%s";

    public static String subnet = "aws.amazon.cloud:vpc+%s:subnet+%s";

    public static String subnetNetworkAddress = "aws.amazon.cloud:vpc+%s:subnet+%s:cidr";

    public static String switchingService = "aws.amazon.cloud:vpc+%s:switchingservice";

    public static String vif = "aws.amazon.cloud:vif%s";

    public static String vlan = "aws.amazon.cloud:vif+%s:vlan+%s";

    public static String volume = "aws.amazon.cloud:volume+%s";

    public static String vpc = "aws.amazon.cloud:vpc+%s";

    public static String vpcNetworkAddress = "aws.amazon.cloud:vpc+%s:cidr";

    public static String vpcService = "aws.amazon.cloud:vpcservice+%s";

}
