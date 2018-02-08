package net.maxgigapop.mrs.rest.api;

import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequest;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;

import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/sense/service")
@Api(description = "the service API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2018-02-08T15:27:04.431Z")
public class SenseServiceApi {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create service instance", notes = "Create a service instance (negotiation optional)", response = ServiceIntentResponse.class, tags={ "computation", "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful service creation", response = ServiceIntentResponse.class),
        @ApiResponse(code = 400, message = "Malformed or bad request", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response servicePost(@Valid ServiceIntentRequest body) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{siUUID}/commit")
    @ApiOperation(value = "Commit service reservation", notes = "Committing service instance for resource allocation and blocking for commit status", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully committed", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDCommitPut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }

    @DELETE
    @Path("/{siUUID}")
    @ApiOperation(value = "Delete service instance", notes = "Deleting service instance that is in final status (ACTIVE, TERMINATED or FAILED)", response = Void.class, tags={ "computation", "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully deleted", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDDelete(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{siUUID}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create/Negotiate a service instance", notes = "Create/Negotiate a service instance (negotiation optional)", response = ServiceIntentResponse.class, tags={ "computation", "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful service creation", response = ServiceIntentResponse.class),
        @ApiResponse(code = 400, message = "Malformed or bad request", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDPost(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID,@Valid ServiceIntentRequest body) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{siUUID}/release")
    @ApiOperation(value = "Commit release service instance reservation", notes = "Transactionally populating service instance data to release reservation", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully released", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReleasePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/{siUUID}/reserve")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Reserve service instance with an intent", notes = "Compute an intent and make reservation using the instant result", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful service creation", response = Void.class),
        @ApiResponse(code = 400, message = "Malformed or bad request", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReservePost(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID,@Valid ServiceIntentRequest body) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{siUUID}/reserve")
    @ApiOperation(value = "Reserve service instance", notes = "Transactionally populating service instance data to make reservation", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Successfully reserved", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReservePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/{siUUID}/status")
    @ApiOperation(value = "Instance status", notes = "Retrieve service instance status", response = String.class, tags={ "connection", "computation", "monitoring", "troubleshoot",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of service status", response = String.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDStatusGet(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }

    @PUT
    @Path("/{siUUID}/terminate")
    @ApiOperation(value = "Commit service release", notes = "Committing service instance for resource deallocation and blocking for commit status", response = Void.class, tags={ "connection" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully terminated", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDTerminatePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        return Response.ok().entity("magic!").build();
    }
}
