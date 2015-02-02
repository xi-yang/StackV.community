/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.regions.Regions;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;

/**
 *
 * @author muzcategui
 */
public class AwsPullTest 
{
    public static void main (String [] args) throws IOException
    {
        OntModel ontModel = AwsModelBuilder.createOntology("access_key_id","secret_key_id",Regions.US_EAST_1);
    }
}
