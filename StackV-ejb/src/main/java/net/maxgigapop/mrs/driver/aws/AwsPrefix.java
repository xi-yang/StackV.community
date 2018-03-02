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

import java.util.logging.Logger;

/**
 *
 * @author muzcategui
 */
public class AwsPrefix {
    private String prefix = "";
    private String defaultPrefix = "aws.amazon.com:aws-cloud";

    Logger log = Logger.getLogger(AwsDriver.class.getName());

    public AwsPrefix () {
        prefix = defaultPrefix;
    }
    
    public AwsPrefix (String uri) {
        setTopologyPrefix(uri);
    }
    
    public void setTopologyPrefix(String uri) {
        if (uri.startsWith("urn:ogf:network:")) {
            prefix = uri.substring("urn:ogf:network:".length());
        } else {
            String[] fields = uri.split(":");
            for (String field: fields) {
                if (field.matches("^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\\\.)+[A-Za-z]{2,6}$")) {
                    prefix = uri.substring(uri.indexOf(field));
                    return;
                }
            }
            AwsDriver.logger.warning("setTopologyPrefix", "AwsPrefix failed with invalid uri="+uri);
        }
    }
    
    public String defaultPrefix() { return defaultPrefix;  }
    
    public String bucket() { return prefix+":bucket+%s";  }
    
    public String ebsService() {
        return prefix+":ebsservice+%s"; 
    }

    public String ec2Service() {  return prefix+":ec2service+%s";  }
    
    public String cgw() { return prefix+":customer-gateway+%s"; }

    public String directConnectService() {  return defaultPrefix+":directconnect+%s";  }

    public String directConnect() {  return prefix+":directconnect+%s";  }

    public String gateway() {  return prefix+":gateway+%s";  }

    public String instance() {  return prefix+":vpc+%s:subnet+%s:instance+%s";  }
    
    public String nic() {  return prefix+":vpc+%s:subnet+%s:nic+%s";  }
    
    public String nicNetworkAddress() {  return prefix+":vpc+%s:subnet+%s:nic+%s:ip+%s";  }
    
    public String publicAddress() {  return prefix+":public-ip+%s";  }

    public String route() {  return prefix+":vpc+%s:routingtable+%s:route+%s";  }

    public String routeFrom() {  return prefix+":vpc+%s:routingtable+%s:route+%s:routefrom";  }

    public String routeTo() {  return prefix+":vpc+%s:routingtable+%s:route+%s:routeto";  }

    public String routingService() {  return prefix+":vpc+%s:routingservice";  }

    public String routingTable() {  return prefix+":vpc+%s:routingtable+%s";  }

    public String s3Service() {  return prefix+":s3service+%s";  }

    public String subnet() {  return prefix+":vpc+%s:subnet+%s";  }

    public String subnetNetworkAddress() {  return prefix+":vpc+%s:subnet+%s:cidr";  }

    public String switchingService() {  return prefix+":vpc+%s:switchingservice";  }

    public String volume() {  return prefix+":volume+%s";  }

    public String vpc() {  return prefix+":vpc+%s";  }

    public String vpcNetworkAddress() {  return prefix+":vpc+%s:cidr";  }

    public String vpcService() {  return prefix+":vpcservice+%s";  }
    
    public String vpn() {  return prefix+":vpn+%s";  }
    
    // DirectConnect related
    static public String vif() { return "%s:vlanport+%s";  }

    static public String label() { return "%s:label+%s";  }

    static public String labelGroup() { return "%s:labelgroup+%s";  }

}
