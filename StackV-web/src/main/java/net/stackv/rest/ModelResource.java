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

package net.stackv.rest;

import java.util.Map;
import java.util.UUID;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.ontology.OntModel;

import org.json.simple.JSONObject;

import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.stackv.rest.model.ApiModelBase;
import net.stackv.rest.model.ApiModelViewRequest;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * REST Web Service
 *
 * @author max
 */
@Path("model")
@RequestScoped
public class ModelResource {

    private final StackLogger logger = new StackLogger(ModelResource.class.getName(), "ModelResource");

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of ModelResource
     */
    public ModelResource() {
    }

    /**
     * Retrieves representation of an instance of
     * net.stackv.rest.ModelResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("application/xml")
    @Path("/{refUUID}")
    public ApiModelBase pullXml(@PathParam("refUUID") String refUUID) throws Exception {
        String method = "pullXml";
        logger.refuuid(refUUID);
        logger.trace_start(method);
        ModelBase modelBase = systemCallHandler.retrieveVersionGroupModel(refUUID);
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(modelBase.getCxtVersionTag());
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(modelBase.getCreationTime()));
        // apiModelBase.setStatus("");
        apiModelBase.setTtlModel(ModelUtil.marshalOntModel(modelBase.getOntModel()));
        logger.trace_end(method);
        return apiModelBase;
    }

    @GET
    @Produces("application/json")
    @Path("/{refUUID}")
    public ApiModelBase pull(@PathParam("refUUID") String refUUID) throws Exception {
        String method = "pull";
        logger.refuuid(refUUID);
        logger.trace_start(method);
        ModelBase modelBase = systemCallHandler.retrieveVersionGroupModel(refUUID);
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(modelBase.getCxtVersionTag());
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(modelBase.getCreationTime()));
        // apiModelBase.setStatus("");
        apiModelBase.setTtlModel(ModelUtil.marshalOntModelJson(modelBase.getOntModel()));
        logger.trace_end(method);
        return apiModelBase;
    }

    /**
     * PUT method for updating or creating an instance of ModelResource
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Produces({ "application/xml", "application/json" })
    @Path("/{refUUID}")
    public ApiModelBase update(@PathParam("refUUID") String refUUID) throws Exception {
        String method = "update";
        logger.refuuid(refUUID);
        logger.trace_start(method);
        systemCallHandler.updateHeadVersionGroup_API(refUUID);
        ApiModelBase apiData = this.pull(refUUID);
        logger.trace_end(method);
        return apiData;
    }

    @GET
    @Produces({ "application/xml" })
    public ApiModelBase creatHeadVersionGroup() throws Exception {
        String method = "creatHeadVersionGroup";
        logger.trace_start(method);
        VersionGroup vg = systemCallHandler.createHeadVersionGroup_API(UUID.randomUUID().toString());
        ApiModelBase apiData = this.pullXml(vg.getRefUuid());
        logger.trace_end(method);
        return apiData;
    }

    @GET
    @Produces({ "application/json" })
    public ApiModelBase creatHeadVersionGroupJson() throws Exception {
        String method = "creatHeadVersionGroupJson";
        logger.trace_start(method);
        VersionGroup vg = systemCallHandler.createHeadVersionGroup_API(UUID.randomUUID().toString());
        ApiModelBase apiData = this.pull(vg.getRefUuid());
        logger.trace_end(method);
        return apiData;
    }

    @GET
    @Path("/systeminstance")
    @Produces({ "application/xml", "application/json" })
    public String push() {
        String method = "push";
        logger.trace_start(method);
        String ret = systemCallHandler.createInstance_API().getReferenceUUID();
        logger.trace_end(method);
        return ret;
    }

    @DELETE
    @Path("/systeminstance/{refUUID}")
    public String terminate(@PathParam("refUUID") String refUUID) {
        String method = "terminate";
        logger.refuuid(refUUID);
        systemCallHandler.terminateInstance_API(refUUID);
        logger.trace_end(method);
        return "Successfully terminated";
    }

    @POST
    @Consumes({ "application/xml", "application/json" })
    @Produces("application/xml")
    @Path("/view/{refUUID}")
    public ApiModelBase queryView(@PathParam("refUUID") String refUUID, ApiModelViewRequest viewRequest)
            throws Exception {
        String method = "queryView";
        logger.refuuid(refUUID);
        OntModel ontModel = systemCallHandler.queryModelView(refUUID, viewRequest.getFilters());
        if (ontModel == null) {
            throw logger.error_throwing(method, "systemCallHandler.queryModelView return null model.");
        }
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setVersion(refUUID);
        java.util.Date now = new java.util.Date();
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(new java.sql.Date(now.getTime())));
        apiModelBase.setTtlModel(ModelUtil.marshalOntModel(ontModel));
        logger.trace_end(method);
        return apiModelBase;
    }

    @POST
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/json" })
    @Path("/view/{refUUID}")
    public ApiModelBase queryViewJson(@PathParam("refUUID") String refUUID, ApiModelViewRequest viewRequest)
            throws Exception {
        String method = "queryViewJson";
        logger.refuuid(refUUID);
        OntModel ontModel = systemCallHandler.queryModelView(refUUID, viewRequest.getFilters());
        if (ontModel == null) {
            throw logger.error_throwing(method, "systemCallHandler.queryModelView return null model.");
        }
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setVersion(refUUID);
        java.util.Date now = new java.util.Date();
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(new java.sql.Date(now.getTime())));
        apiModelBase.setTtlModel(ModelUtil.marshalOntModelJson(ontModel));
        logger.trace_end(method);
        return apiModelBase;
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/refresh")
    public Response refreshVersionModels(Map<String, String> requestMap) throws Exception {
        JSONObject jsonRet = new JSONObject();
        for (String topoUri : requestMap.keySet()) {
            String modelUuid = requestMap.get(topoUri); // alternative: time stamp
            ModelBase modelBase = systemCallHandler.retrieveLatestModelByDriver(topoUri);
            if (modelBase == null || modelUuid != null && modelBase.getId().equalsIgnoreCase(modelUuid)) {
                jsonRet.put(topoUri, null);
                continue;
            }
            JSONObject jsonTopo = new JSONObject();
            jsonTopo.put("uuid", modelBase.getId());
            jsonTopo.put("time", modelBase.getCreationTime());
            jsonTopo.put("ttl", modelBase.getTtlModel());
            jsonTopo.put("json", ModelUtil.marshalOntModelJson(modelBase.getOntModel()));
            jsonRet.put(topoUri, jsonTopo);
        }
        return Response.status(200).entity(jsonRet).build();
    }

    @GET
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/refresh/{topoURI}")
    public Response refreshModelAllOrAny(@PathParam("topoURI") String topoURI) throws Exception {
        JSONObject jsonRet = new JSONObject();
        if (topoURI.equalsIgnoreCase("all")) {
            Map<String, ModelBase> topoModelMap = systemCallHandler.retrieveAllLatestModels();
            for (String aTopoUri : topoModelMap.keySet()) {
                ModelBase modelBase = topoModelMap.get(aTopoUri);
                JSONObject jsonTopo = new JSONObject();
                jsonTopo.put("uuid", modelBase.getId());
                jsonTopo.put("time", modelBase.getCreationTime());
                jsonTopo.put("ttl", modelBase.getTtlModel());
                jsonTopo.put("json", ModelUtil.marshalOntModelJson(modelBase.getOntModel()));
                jsonRet.put(aTopoUri, jsonTopo);
            }
            return Response.status(200).entity(jsonRet).build();
        } else {
            ModelBase modelBase = systemCallHandler.retrieveLatestModelByDriver(topoURI);
            if (modelBase == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("no model available for topology: " + topoURI)
                        .build();
            }
            JSONObject jsonTopo = new JSONObject();
            jsonTopo.put("uuid", modelBase.getId());
            jsonTopo.put("time", modelBase.getCreationTime());
            jsonTopo.put("ttl", modelBase.getTtlModel());
            jsonTopo.put("json", ModelUtil.marshalOntModelJson(modelBase.getOntModel()));
            jsonRet.put(topoURI, jsonTopo);
        }
        return Response.status(200).entity(jsonRet).build();
    }

}
