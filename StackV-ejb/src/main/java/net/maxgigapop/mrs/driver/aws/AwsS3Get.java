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

import net.maxgigapop.mrs.driver.aws.AwsAuthenticateService;
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
public class AwsS3Get {

    private AmazonS3Client client = null;
    private List<Bucket> buckets = null;

    public AwsS3Get(String access_key_id, String secret_access_key, Regions region) {
        AwsAuthenticateService authenticate = new AwsAuthenticateService(access_key_id, secret_access_key);
        this.client = authenticate.AwsAuthenticateS3Service(Region.getRegion(region));

        buckets = this.client.listBuckets();
    }

    //get the current client
    public AmazonS3Client getClient() {
        return this.client;
    }

    //get the account owner
    public Owner getAccountOwner() {
        return this.client.getS3AccountOwner();
    }

    //get the list of all the buckets in the account
    public List<Bucket> getBuckets() {
        return buckets;
    }

    //get a specific bucket from the a list of buckets from its name
    public Bucket getBucket(List<Bucket> buckets, String name) {
        for (Bucket b : buckets) {
            if (b.getName().equals(name)) {
                return b;
            }
        }

        return null;
    }

    //get a list of all the objects within an specific bucket 
    public List<S3ObjectSummary> getObjects(Bucket bucket) {
        List<S3ObjectSummary> objects = new ArrayList();

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket.getName());
        ObjectListing objectListing;

        do {
            objectListing = this.client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                objects.add(objectSummary);
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

        return objects;
    }
}
