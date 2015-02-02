/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.AmazonDirectConnectClient;
import com.amazonaws.services.directconnect.model.*;
import java.util.List;

/**
 *
 * @author max
 */
public class AwsDCGet 
{
    private AmazonDirectConnectClient client=null;
    private List<VirtualGateway> virtualGateways=null;
    private List<VirtualInterface> virtualInterfaces=null;
    private List<Connection> connections=null;
    
    public AwsDCGet(String access_key_id, String secret_access_key,Regions region)
    {
        AwsAuthenticateService authenticate=new AwsAuthenticateService(access_key_id,secret_access_key);
        this.client = authenticate.AwsAuthenticateDCService(Region.getRegion(region));
        
        //get all the privateGateways gateways under the account
        DescribeVirtualGatewaysResult virtualGatewaysResult= this.client.describeVirtualGateways();
        virtualGateways=virtualGatewaysResult.getVirtualGateways();
        
        //get all the virtual interfaces under the account
        DescribeVirtualInterfacesResult virtualInterfacesResult=this.client.describeVirtualInterfaces();
        virtualInterfaces=virtualInterfacesResult.getVirtualInterfaces();
        
        //get all the connections under the account
        DescribeConnectionsResult connectionsResult=this.client.describeConnections();
        connections= connectionsResult.getConnections();
    }
    
    //get all the virtual Interfaces 
    public List<VirtualInterface> getVirtualInterfaces()
    {
        return virtualInterfaces;
    }
    
    //get all the direct connect connections
    public List<Connection> getConnections()
    {
        return connections;
    }
    
    //get all the private gateways 
    public List<VirtualGateway> getVirtualGateways()
    {
        return virtualGateways;
    }
}
