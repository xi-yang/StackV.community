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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.directconnect.AmazonDirectConnectAsyncClient;
import com.amazonaws.services.directconnect.AmazonDirectConnectClient;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 *
 * @author muzcategui
 */
public class AwsAuthenticateService {

    private BasicAWSCredentials credentials = null;

    //set the access key id and the secret key 
    public AwsAuthenticateService(String access_key_id, String secret_access_key) {
        credentials = new BasicAWSCredentials(access_key_id, secret_access_key);
    }

    /*
     Authenticate EC2 client
     */
    public AmazonEC2Client AwsAuthenticateEC2Service(Region region) {
        AmazonEC2Client ec2Client = null;
        try {
            ec2Client = new AmazonEC2Client(credentials);
            ec2Client.setRegion(region);
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        return ec2Client;
    }

    /*
     Authenticate EC2 client
     */
    public AmazonEC2AsyncClient AwsAuthenticateEC2ServiceAsync(Region region) {
        AmazonEC2AsyncClient ec2Client = null;
        try {
            ec2Client = new AmazonEC2AsyncClient(credentials);
            ec2Client.setRegion(region);
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        return ec2Client;
    }

    /*
     Authenticate S3 client
     */
    public AmazonS3Client AwsAuthenticateS3Service(Region region) {
        AmazonS3Client S3Client = null;
        try {
            S3Client = new AmazonS3Client(credentials);
            S3Client.setRegion(region);
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        return S3Client;
    }

    /*
     Authenticate Direct Connect client
     */
    public AmazonDirectConnectClient AwsAuthenticateDCService(Region region) {
        AmazonDirectConnectClient dcClient = null;
        try {
            dcClient = new AmazonDirectConnectClient(credentials);
            dcClient.setRegion(region);
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        return dcClient;
    }
    
    public AmazonDirectConnectAsyncClient AwsAuthenticateDCServiceAsync(Region region) {
        AmazonDirectConnectAsyncClient dcClient = null;
        try {
            dcClient = new AmazonDirectConnectAsyncClient(credentials);
            dcClient.setRegion(region);
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        return dcClient;
    }
}
