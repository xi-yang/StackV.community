/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import net.maxgigapop.mrs.driver.onosystem.OnosAuthenticateService;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author muzcategui
 */
public class OnosS3Get 
{
    private AmazonS3Client client= null;
    private List<Bucket> buckets=null;
    
    public OnosS3Get(String access_key_id, String secret_access_key,Regions region)
    {
        OnosAuthenticateService authenticate=new OnosAuthenticateService(access_key_id,secret_access_key);
        this.client = authenticate.AwsAuthenticateS3Service(Region.getRegion(region));
        
         buckets=this.client.listBuckets();
    }
    
    //get the current client
    public  AmazonS3Client getClient()
    {
        return this.client;
    }
    
    //get the account owner
    public Owner getAccountOwner()
    {
        return this.client.getS3AccountOwner();
    }
    
    //get the list of all the buckets in the account
    public List<Bucket> getBuckets()
    {
        return buckets;
    }
    
    //get a specific bucket from the a list of buckets from its name
    public Bucket getBucket(List<Bucket> buckets,String name)
    {
        for(Bucket b : buckets)
        {
            if(b.getName().equals(name))
                return b;
        }
        
        return null;
    }
    
   //get a list of all the objects within an specific bucket 
    public List<S3ObjectSummary> getObjects(Bucket bucket)
    {
       List<S3ObjectSummary> objects=new ArrayList(); 
       
       ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
               .withBucketName(bucket.getName());
       ObjectListing objectListing;

       do {
            objectListing = this.client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary :objectListing.getObjectSummaries()) 
            {
		objects.add(objectSummary);
            }
	listObjectsRequest.setMarker(objectListing.getNextMarker());
            } while (objectListing.isTruncated());
       
       return objects;
    }
}
