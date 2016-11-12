/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
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

    public static String label = "%s:label+%s";

    public static String labelGroup = "%s:labelgroup+%s";
    
    public static String nic = prefix+":vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = prefix+":vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    public static String publicAddress = prefix+":public-ip+%s";

    public static String route = prefix+":vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = prefix+":vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = prefix+":vpc+%s:routingtable+%s:route+%s:routeto";

    public static String routingService = prefix+":vpc+%s:routingservice";

    public static String routingTable = prefix+":vpc+%s:routingtable+%s";

    public static String s3Service = prefix+":s3service+%s";

    public static String subnet = prefix+":vpc+%s:subnet+%s";

    public static String subnetNetworkAddress = prefix+":vpc+%s:subnet+%s:cidr";

    public static String switchingService = prefix+":vpc+%s:switchingservice";

    public static String vif = "%s:dxvif+vlan%s";

    public static String vlan = prefix+":vif+%s:vlan+%s";

    public static String volume = prefix+":volume+%s";

    public static String vpc = prefix+":vpc+%s";

    public static String vpcNetworkAddress = prefix+":vpc+%s:cidr";

    public static String vpcService = prefix+":vpcservice+%s";

}
