/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.AmazonDirectConnectClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 *
 * @author muzcategui
 */


public class AwsAuthenticateService 
{    
    private BasicAWSCredentials credentials=null;
    
    
    //set the access key id and the secret key 
    public AwsAuthenticateService(String access_key_id, String secret_access_key)
    {
        credentials=new BasicAWSCredentials(access_key_id,secret_access_key);
    }

    
    /*
    Authenticate EC2 client
    */
    public AmazonEC2Client  AwsAuthenticateEC2Service(Region region)
    {
        AmazonEC2Client ec2Client=null;
        try
        {
            ec2Client=new AmazonEC2Client(credentials);
            ec2Client.setRegion(region);
        }
        catch (Exception e )
        {
            throw new AmazonClientException(e.getMessage());
        }
        return ec2Client;
    }
    
    /*
    Authenticate S3 client
    */
    public AmazonS3Client  AwsAuthenticateS3Service(Region region)
    {
        AmazonS3Client S3Client=null;
        try
        {
            S3Client=new AmazonS3Client(credentials);
            S3Client.setRegion(region);
        }
        catch (Exception e )
        {
            throw new AmazonClientException(e.getMessage());
        }
        return S3Client;
    }
    
    /*
    Authenticate Direct Connect client
    */
    public AmazonDirectConnectClient AwsAuthenticateDCService(Region region)
    {
        AmazonDirectConnectClient dcClient=null;
        try
        {
            dcClient=new AmazonDirectConnectClient(credentials);
            dcClient.setRegion(region);
        }
        catch(Exception e)
        {
            throw new AmazonClientException(e.getMessage());
        }
        return dcClient;
    }
}
