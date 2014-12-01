/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.openstack;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;

/**
 *
 * @author james
 */
public class OpenStackModelPush {
    
    public static void pushDelta(OntModel model, DriverSystemDelta sysDelta) {
        
        DeltaModel modelAdd = sysDelta.getModelAddition();
        DeltaModel modelSub = sysDelta.getModelReduction();
        
        OntModel addition = modelAdd.getOntModel();
        OntModel remove = modelSub.getOntModel();
        
        ArrayList<Resource> serversToAdd = new ArrayList<>();
        ArrayList<Resource> serversToDelete = new ArrayList<>();
        
        Resource Nova = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-nova");
        Property hasService = model.createProperty( "http://schemas.ogf.org/nml/2013/03/base#hasService" );
        
        //get servers to add
        ResIterator it = addition.listResourcesWithProperty(hasService, Nova);
        while(it.hasNext()) {
            serversToAdd.add(it.next());
        }
        
        //get servers to delete
        it = remove.listResourcesWithProperty(hasService, Nova);
        while(it.hasNext()) {
            serversToDelete.add(it.next());
        }
        
        
        
    }
    
}
