package net.maxgigapop.mrs.rest.api;

import net.maxgigapop.mrs.rest.api.model.DiscoveryDescription;
import net.maxgigapop.mrs.rest.api.model.DomainDescription;
import net.maxgigapop.mrs.rest.api.model.ServiceDescription;
import net.maxgigapop.mrs.rest.api.model.TopologyDescription;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;

import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("sense/discovery")
@Api(description = "the discovery API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2018-02-07T16:56:34.309Z")
public class SenseDiscoveryApi {

    @GET
    @Path("/edgepoints/{domainID}")
    @ApiOperation(value = "Edge points discovery and description for a specific domain", notes = "List all associated edge points (and capabilities)", response = DomainDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topology desciptions", response = DomainDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryEdgepointsDomainIDGet(@PathParam("domainID") @ApiParam("Name of URI of a target domain") String domainID) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/edgepoints/{domainID}/peer")
    @ApiOperation(value = "edge points discovery and description of peer domain for a given domain or end-site", notes = "List peer domain edge points (and capabilities) that connect this domain (by URI or name)", response = DomainDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topology desciptions", response = DomainDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryEdgepointsDomainIDPeerGet(@PathParam("domainID") @ApiParam("Name of URI of a target end-site domain") String domainID) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/edgepoints")
    @ApiOperation(value = "Topology edge points discovery and description", notes = "List all known domains (and capabilities?)", response = TopologyDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topoogy desciptions", response = TopologyDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryEdgepointsGet() {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @ApiOperation(value = "Orchestrator API discovery and description", notes = "List API dpoints, supported service types and capabilities", response = DiscoveryDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful service creation", response = DiscoveryDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryGet() {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/services")
    @ApiOperation(value = "Service discovery and description", notes = "List service instances", response = ServiceDescription.class, responseContainer = "List", tags={ "discovery" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of service desciptions", response = ServiceDescription.class, responseContainer = "List"),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryServicesGet() {
        return Response.ok().entity("magic!").build();
    }
}
