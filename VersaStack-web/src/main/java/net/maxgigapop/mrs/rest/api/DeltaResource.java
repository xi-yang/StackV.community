/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

package net.maxgigapop.mrs.rest.api;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
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
    public String commit(@PathParam("refUUID") String refUUID, @PathParam("action") String action) throws ExecutionException, InterruptedException {
        if (!action.toLowerCase().equals("commit")) {
            throw new BadRequestException("Invalid action: " + action);
        }
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (systemInstance == null) {
            throw new EJBException("commitDelta encounters unknown systemInstance with referenceUUID=" + refUUID);
        }
        if (systemInstance.getCommitStatus() != null) {
            throw new EJBException("commitDelta has already been done once with systemInstance with referenceUUID=" + refUUID);
        }
        try {
            Future<String> result = systemCallHandler.commitDelta(systemInstance);
            systemInstance.setCommitStatus(result);
            if (!result.isDone()) {
                return "PROCESSING";
            }
            return result.get();
        } catch (EJBException ex) {
            systemInstance.setCommitStatus(new AsyncResult<>(ex.getMessage()));
            return ex.getMessage();
        }
    }

    @GET
    @Path("/{refUUID}/{action}")
    public String checkStatus(@PathParam("refUUID") String refUUID, @PathParam("action") String action) throws InterruptedException, ExecutionException {
        if (!action.toLowerCase().equals("checkstatus")) {
            throw new BadRequestException("Invalid action: "+action);
        }
        SystemInstance siCache = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (siCache == null) {
            throw new EJBException ("checkStatus encounters unknown systemInstance with referenceUUID="+ refUUID);
        }
        SystemInstance siDb = SystemInstancePersistenceManager.findById(siCache.getId());
        if(siDb.getSystemDelta() == null)
            return "System Instance has not yet propagated";
        if(siCache.getCommitStatus() == null)
            return "System Instance has not yet committed";
        if(siCache.getCommitStatus() != null && !siCache.getCommitStatus().isDone())
            return "PROCESSING";
        String result = "";
        try{
            result = siCache.getCommitStatus().get();
        }catch(InterruptedException | ExecutionException ex){
            return "System Instance with referenceUUID="+ refUUID + "throws exception when committing: " + ex;
        }
        return result;
    }
    
    @POST
    @Consumes({"application/xml","application/json"})
    @Path("/{refUUID}/{action}")
    public String push(@PathParam("refUUID")String SysInstanceRefUUID, ApiDeltaBase deltabase, @PathParam("action") String action) throws Exception{
        if (!action.toLowerCase().startsWith("propagate")) {
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
        
        try {
            if (action.toLowerCase().equals("propagate")) {
                systemCallHandler.propagateDelta(SysInstanceRefUUID, systemDelta, true, false);
            } else if (action.toLowerCase().equals("propagate_forward")) {
                systemCallHandler.propagateDelta(SysInstanceRefUUID, systemDelta, false, false);
            } else if (action.toLowerCase().equals("propagate_forced")) {
                systemCallHandler.propagateDelta(SysInstanceRefUUID, systemDelta, false, true);
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return "propagate successfully";
    }
    
    
}