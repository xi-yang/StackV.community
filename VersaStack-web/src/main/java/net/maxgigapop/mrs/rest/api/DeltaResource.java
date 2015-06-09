/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaBase;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("delta")
public class DeltaResource {
    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;
    
    public DeltaResource(){
    }
    
    
    @PUT
    @Path("/{refUUID}/{action}")
    public String commit(@PathParam("refUUID")String refUUID, @PathParam("action") String action) throws ExecutionException, InterruptedException{
        if (!action.toLowerCase().equals("commit")) {
            throw new BadRequestException("Invalid action: "+action);
        }
        Future<String> result;
        try{
            result = systemCallHandler.commitDelta(refUUID);
        }catch(EJBException e){
            return e.getMessage();
        }
        if(!result.isDone()){
            return "PROCESSING";
        }
        return result.get();
    }
    
    @GET
    @Path("/{refUUID}/{action}")
    public String checkStatus(@PathParam("refUUID")String refUUID, @PathParam("action") String action) throws InterruptedException, ExecutionException{
        if (!action.toLowerCase().equals("checkstatus")) {
            throw new BadRequestException("Invalid action: "+action);
        }
        SystemInstance siCache = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (siCache == null) {
            return ("Unknown systemInstance with referenceUUID="+ refUUID);
        }
        SystemInstance siDb = SystemInstancePersistenceManager.findById(siCache.getId());
        if(siDb.getSystemDelta() == null)
            return "System Instance has not yet propagated";
        if(!siCache.getCommitFlag())
            return "System Instance has not yet commit";
        if(siCache.getCommitStatus() == null)
            return "PROCESSING";
        return siCache.getCommitStatus().get();
    }
    
    
    @POST
    @Consumes({"application/xml","application/json"})
    @Path("/{refUUID}/{action}")
    public String push(@PathParam("refUUID")String SysInstanceRefUUID, ApiDeltaBase deltabase, @PathParam("action") String action) throws Exception{
        if (!action.toLowerCase().equals("propagate")) {
            throw new BadRequestException("Invalid action: "+action);
        }
        
        SystemDelta systemDelta = new SystemDelta();
        systemDelta.setReferenceVersionGroup(VersionGroupPersistenceManager.findByReferenceId(deltabase.getReferenceVersion()));
        
        OntModel modelAddition = ModelUtil.unmarshalOntModel(deltabase.getModelAddition());
        DeltaModel dmAddition = new DeltaModel();
        dmAddition.setCommitted(false);
        dmAddition.setDelta(systemDelta);
        dmAddition.setIsAddition(true);
        dmAddition.setOntModel(modelAddition);
        
        OntModel modelReduction = ModelUtil.unmarshalOntModel(deltabase.getModelReduction());
        DeltaModel dmReduction = new DeltaModel();
        dmReduction.setCommitted(false);
        dmReduction.setDelta(systemDelta);
        dmReduction.setIsAddition(false);
        dmReduction.setOntModel(modelReduction);
        
        systemDelta.setModelAddition(dmAddition);
        systemDelta.setModelReduction(dmReduction);
        
        try{
            systemCallHandler.propagateDelta(SysInstanceRefUUID, systemDelta);
        }catch(Exception e){
            return e.getMessage();
        }
        return "propagate successfully";
    }
    
    
}
