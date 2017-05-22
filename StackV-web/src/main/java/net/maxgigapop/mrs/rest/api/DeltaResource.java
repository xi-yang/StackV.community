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
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaBase;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("delta")
public class DeltaResource {
    
    private final StackLogger logger = new StackLogger(DeltaResource.class.getName(), "DeltaResource");

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;
    
    public DeltaResource(){
    }
    
    
    @PUT
    @Path("/{refUUID}/{action}")
    public String commit(@PathParam("refUUID") String refUUID, @PathParam("action") String action) throws ExecutionException, InterruptedException {
        String method = "commit";
        logger.refuuid(refUUID);
        logger.trace_start(method);
        if (!action.toLowerCase().equals("commit")) {
            throw new BadRequestException("Invalid action: " + action);
        }
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (systemInstance == null) {
            throw logger.error_throwing(method, "commitDelta encounters unknown ref:SystemInstance");
        }
        if (systemInstance.getCommitStatus() != null) {
            throw logger.error_throwing(method, "commitDelta has already been done once with ref:SystemInstance");
        }
        try {
            Future<String> result = systemCallHandler.commitDelta(systemInstance);
            systemInstance.setCommitStatus(result);
            if (!result.isDone()) {
                logger.trace_end(method);
                return "PROCESSING";
            }
            logger.trace_end(method);
            return result.get();
        } catch (Exception ex) {
            systemInstance.setCommitStatus(new AsyncResult<>(ex.getMessage()));
            throw logger.throwing(method, ex);
        }
    }

    @GET
    @Path("/{refUUID}/{action}")
    public String checkStatus(@PathParam("refUUID") String refUUID, @PathParam("action") String action) throws InterruptedException, ExecutionException {
        String method = "commit";
        logger.refuuid(refUUID);
        logger.trace_start(method);
        if (!action.toLowerCase().equals("checkstatus")) {
            logger.error(method, "Invalid action: "+action);
            throw new BadRequestException("Invalid action: "+action);
        }
        SystemInstance siCache = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (siCache == null) {
            throw logger.error_throwing(method, "checkStatus encounters unknown ref:SystemInstance");
        }
        SystemInstance siDb = SystemInstancePersistenceManager.findById(siCache.getId());
        if( siDb.getSystemDelta() == null) {
            throw logger.error_throwing(method, "System Instance has not yet propagated");
        }
        if (siCache.getCommitStatus() == null){
            throw logger.error_throwing(method, "System Instance has not yet committed");
        }
        if (siCache.getCommitStatus() != null && !siCache.getCommitStatus().isDone()) {
            logger.trace_end(method, "PROCESSING");
            return "PROCESSING";
        }
        String result = "";
        try {
            result = siCache.getCommitStatus().get();
        } catch(InterruptedException | ExecutionException ex){
            throw logger.throwing(method, ex);
        }
        logger.trace_end(method);
        return result;
    }
    
    @POST
    @Consumes({"application/xml","application/json"})
    @Path("/{refUUID}/{action}")
    public String push(@PathParam("refUUID")String SysInstanceRefUUID, ApiDeltaBase deltabase, @PathParam("action") String action) throws Exception{
        String method = "commit";
        logger.refuuid(SysInstanceRefUUID);
        logger.trace_start(method);
        if (!action.toLowerCase().startsWith("propagate")) {
            logger.error(method, "Invalid action: "+action);
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
            throw logger.throwing(method, e);
        }
        logger.trace_end(method);
        return "propagate successfully";
    }
    
    
}