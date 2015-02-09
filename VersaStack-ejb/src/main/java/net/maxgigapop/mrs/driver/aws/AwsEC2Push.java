/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author max
 */
public class AwsEC2Push 
{
    public AmazonEC2Client client=null;
     Logger logger = Logger.getLogger(AwsModelBuilder.class.getName());
    
    public AwsEC2Push(String access_key_id, String secret_access_key,Regions region)
    {
        AwsAuthenticateService authenticate=new AwsAuthenticateService(access_key_id,secret_access_key);
        this.client = authenticate.AwsAuthenticateEC2Service(Region.getRegion(region));
    }
    
   //function to create EC2 Instances, returns true if operations were succesfull
    /*public boolean createInstances(HashMap<String,Object> args)
    {
        RunInstancesRequest runInstances= new RunInstancesRequest();
        
        for(String key: args.keySet())
        {
            switch(key)
            {
                case "withBlockDeviceMapping":runInstances.withBlockDeviceMappings((BlockDeviceMapping) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                case "withDisableApiTermination:": runInstances.withDisableApiTermination((boolean) args.get(key));
                    break;  
                case "withEbsOptimized:": runInstances.withEbsOptimized((boolean) args.get(key));
                    break;
                case "withIamInstanceProfile:": runInstances.withIamInstanceProfile((IamInstanceProfileSpecification) args.get(key));
                    break;
                case "withImageId:": runInstances.withImageId((String) args.get(key));
                    break;
                case "withInstanceInitiatedShutdownBehavior": runInstances.withInstanceInitiatedShutdownBehavior((String) args.get(key));
                    break;
                case "withInstanceType": runInstances.withInstanceType((String) args.get(key));
                    break;
                case "withKernelId:": runInstances.withKernelId((String) args.get(key));
                    break;
                case "withKeyName": runInstances.withKeyName((String) args.get(key));
                    break;
                case "withMaxCount:": runInstances.withMaxCount((int) args.get(key));
                    break;
                case "withMinCount:": runInstances. withMinCount((int) args.get(key));
                    break;
                case "withMonitoring:": runInstances.withMonitoring((boolean) args.get(key));
                    break;
                case "withNetworkInterfaces:": runInstances.withNetworkInterfaces((Collection<InstanceNetworkInterfaceSpecification>) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                case "withClientToken:": runInstances.withClientToken((String) args.get(key));
                    break;
                
            }
            
        }
        try
        {
            this.client.runInstances(runInstances);
        }
        catch (Exception e)
        {
            logger.log(Level.INFO,"Error ocurred during termination {0} :", e.getMessage());
            return false;
        }
        return true;
    }*/
    
    //function to create EC2 Instances, returns true if operations were succesfull
    public boolean createInstances(String imageId, String instanceType,String subnetId,String key, int numberOfInstances)
    {
        RunInstancesRequest runInstances= new RunInstancesRequest();
        runInstances.withImageId(imageId)
                .withInstanceType(instanceType)
                .withSubnetId(subnetId)
                .withKeyName(key)
                .withMinCount(numberOfInstances)
                .withMaxCount(numberOfInstances);
        try
        {
            this.client.runInstances(runInstances);
        }
        catch (Exception e)
        {
            logger.log(Level.INFO,"Error ocurred during termination {0} :", e.getMessage());
            return false;
        }
        return true;
    }
    
    //function to delete one  EC2 instance, returns true if deleted succesfully
    public boolean deleteInstances(String id)
    {
         TerminateInstancesRequest terminateRequest= new TerminateInstancesRequest();
         terminateRequest.withInstanceIds(id);
         try
         {
            this.client.terminateInstances(terminateRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred during termination {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to delete one or more EC2 instances, returns true if deleted succesfully
    public boolean deleteInstances(List<String> id)
    {
         TerminateInstancesRequest terminateRequest= new TerminateInstancesRequest();
         terminateRequest.withInstanceIds(id);
         try
         {
            this.client.terminateInstances(terminateRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred during termination {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to stop one  EC2 Instance, returns true if stopped succesfuly
    public boolean stopInstances(List<String> id)
    {
         StopInstancesRequest stopRequest= new StopInstancesRequest();
         stopRequest.withInstanceIds(id);
         try
         {
            this.client.stopInstances(stopRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while stopping instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to stop one  EC2 Instance, returns true if stopped succesfuly
    public boolean stopInstances(String id)
    {
         StopInstancesRequest stopRequest= new StopInstancesRequest();
         stopRequest.withInstanceIds(id);
         try
         {
            this.client.stopInstances(stopRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while stopping instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to start one or more  EC2 Instances, returns true if stopped succesfuly
    public boolean startInstances(List<String> id)
    {
         StartInstancesRequest startRequest= new StartInstancesRequest();
         startRequest.withInstanceIds(id);
         try
         {
            this.client.startInstances(startRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while starting instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to start one  EC2 Instance, returns true if stopped succesfuly
    public boolean startInstances(String id)
    {
         StartInstancesRequest startRequest= new StartInstancesRequest();
         startRequest.withInstanceIds(id);
         try
         {
            this.client.startInstances(startRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while starting instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
}
