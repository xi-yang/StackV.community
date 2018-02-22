package net.maxgigapop.mrs.rest.api;

import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequest;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;

import javax.validation.Valid;
import javax.ws.rs.core.Context;
import net.maxgigapop.mrs.common.TokenHandler;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequestConnections;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequestQueries;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponseQueries;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceTerminationPoint;
import net.maxgigapop.mrs.service.HandleServiceCall;
import net.maxgigapop.mrs.service.ServiceEngine;
import net.maxgigapop.mrs.service.VerificationHandler;
import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Path("/sense/service")
@Api(description = "the service API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2018-02-08T15:27:04.431Z")
public class SenseServiceApi {
    private final String restapi = "http://127.0.0.1:8080/StackV-web/restapi";
    JSONParser parser = new JSONParser();

    @Context
    private HttpRequest httpRequest;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create service instance", notes = "Create a service instance (negotiation optional)", response = ServiceIntentResponse.class, tags={ "computation", "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful service creation", response = ServiceIntentResponse.class),
        @ApiResponse(code = 400, message = "Malformed or bad request", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response servicePost(@Valid ServiceIntentRequest body) {
        return serviceSiUUIDPost(null, body);
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
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDPost(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID,@Valid ServiceIntentRequest body) {
    if (!body.getServiceType().equalsIgnoreCase("Multi-Path P2P VLAN")) { //@TBD
            return Response.status(Response.Status.BAD_REQUEST).encoding("Unknown service type: " + body.getServiceType()).build();
        }
        JSONObject jsonReq = new JSONObject();
        jsonReq.put("service", "dnc");
        jsonReq.put("alias", body.getServiceAlias());
        JSONObject jsonData = new JSONObject();
        jsonReq.put("data", jsonData);
        jsonReq.put("synchronous", "true");
        jsonReq.put("proceed", "false");
        jsonData.put("type", "Multi-Path P2P VLAN");
        JSONArray jsonConns = new JSONArray();
        jsonData.put("connections", jsonConns);
        for (ServiceIntentRequestConnections conn: body.getConnections()) {
            JSONObject jsonConn = new JSONObject();
            jsonConns.add(jsonConn);
            jsonConn.put("name", conn.getName());
            if (conn.getBandwidth() != null) {
                JSONObject jsonBw = new JSONObject();
                jsonConn.put("bandwidth", jsonBw);
                jsonBw.put("qos_class", conn.getBandwidth().getQosClass());
                jsonBw.put("capacity", conn.getBandwidth().getCapacity());
                jsonBw.put("unit", conn.getBandwidth().getUnit());
            }
            JSONArray jsonTerminals = new JSONArray();
            jsonConn.put("terminals", jsonTerminals);
            for (ServiceTerminationPoint stp: conn.getTerminals()) {
                JSONObject jsonTerminal = new JSONObject();
                jsonTerminals.add(jsonTerminal);
                jsonTerminal.put("uri", stp.getUri());
                if (stp.getType() == null || stp.getType().isEmpty() || stp.getType().equals("ethernet/vlan")) {
                    jsonTerminal.put("vlan_tag", stp.getLabel());
                }
            }
        }
        SenseServiceQuery.preQueries(jsonData, body.getQueries());
        String svcUUID;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/app/service" + (siUUID == null ? "" : "/"+siUUID), restapi));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            String data = jsonReq.toJSONString();
            svcUUID = executeHttpMethod(url, conn, "POST", data, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse Intent parsing, templating and MCE computation exceptions
        String sysDelta = ServiceEngine.getCachedSystemDelta(svcUUID);
        String ttlModel = null;
        if (sysDelta != null) {
            try {
                Object obj = parser.parse(sysDelta);
                ttlModel = (String) ((JSONObject) obj).get("modelAddition");
            } catch (ParseException ex) {
                ;
            }
        }
        ServiceIntentResponse response = new ServiceIntentResponse()
                .serviceUuid(svcUUID)
                .model(ttlModel);

        try {
            // body.queries -> SPARQL -> response.queries
            SenseServiceQuery.postQueries(body.getQueries(), response.getQueries(), ttlModel, httpRequest);
        } catch (IOException |  ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        return Response.ok().entity(response).build();
    }
    
    @DELETE
    @Path("/{siUUID}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Delete service instance", notes = "Deleting service instance that is in final status (ACTIVE, TERMINATED or FAILED)", response = Void.class, tags={ "computation", "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully deleted", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDDelete(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(auth, refresh);
        try {
            URL url = new URL(String.format("%s/app/service/%s/delete", restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            executeHttpMethod(url, conn, "DELETE", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        return Response.ok().build();
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
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReservePost(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID, @Valid ServiceIntentRequest body) {
        if (body.getQueries() != null && !body.getQueries().isEmpty()) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Intent posted to reserve method should be final and without query.").build();
        }
        Response response = this.serviceSiUUIDPost(siUUID, body);
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            return response;
        }
        return this.serviceSiUUIDReservePut(siUUID);
    }

    @PUT
    @Path("/{siUUID}/reserve")
    @Produces({ "application/json" })
    @ApiOperation(value = "Reserve service instance", notes = "Transactionally populating service instance data to make reservation", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Successfully reserved", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReservePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        Response response = serviceSiUUIDStatusGet(siUUID);
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource unfound:"+siUUID).build();
        }
        String status = response.getEntity().toString();
        String operation;
        if (status.equals("CREATE - COMPILED")) {
            operation = "propagate";
        } else if (status.equals("CREATE - FAILED")) {
            operation = "propagate_forcedretry";
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Request unacceptable under status:"+status).build();
        }
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/service/%s/" + operation, restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            executeHttpMethod(url, conn, "PUT", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        return Response.status(Response.Status.CREATED).build();
    }

    // commit is a sync operation => poll with GET status and verify after COMMITTED
    @PUT
    @Path("/{siUUID}/commit")
    @Produces({ "application/json" })
    @ApiOperation(value = "Commit service reservation", notes = "Committing service instance for resource allocation and blocking for commit status", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully committed", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDCommitPut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        Response response = serviceSiUUIDStatusGet(siUUID);
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource unfound:"+siUUID).build();
        }
        String status = response.getEntity().toString();
        String operation;
        if (status.startsWith("CREATE - PROPAGATED")) {
            operation = "commit";
        } else if (status.equals("CREATE - FAILED")) {
            operation = "force_retry";
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Request unacceptable under status:"+status).build();
        }
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/app/service/%s/" + operation, restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            executeHttpMethod(url, conn, "PUT", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        if (operation.equals("commit")) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                ;
            }
            boolean verifyStarted = false;
            while (true) {
                response = serviceSiUUIDStatusGet(siUUID);
                if (!response.getStatusInfo().equals(Response.Status.OK)) {
                    if (response.getEntity().toString().contains("Server returned HTTP response code: 401") || response.getStatusInfo().equals(Response.Status.UNAUTHORIZED)) {
                        continue;
                    }
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to poll status after commit").build();
                }
                status = response.getEntity().toString();

                if (status.startsWith("CREATE - COMMITTED") && !verifyStarted) {
                    try {
                        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                        final TokenHandler token = new TokenHandler(auth, refresh);
                        URL url = new URL(String.format("%s/app/service/%s/verify", restapi, siUUID));
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Refresh", refresh);
                        executeHttpMethod(url, conn, "PUT", null, token.auth());
                        verifyStarted = true;
                    } catch (IOException ex) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
                    }
                } else if (status.equals("CREATE - READY")) {
                    return Response.ok().build();
                } else if (status.equals("CREATE - COMMITTING")) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        ;
                    }
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Request unacceptable under status:" + status).build();
                }
            }
        }

        return Response.ok().build();
    }

    //@TODO: add commit_async and verify calls
    
    @GET
    @Path("/{siUUID}/status")
    @Produces({ "application/json" })
    @ApiOperation(value = "Instance status", notes = "Retrieve service instance status", response = String.class, tags={ "connection", "computation", "monitoring", "troubleshoot",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of service status", response = String.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDStatusGet(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        String status;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/app/service/%s/status", restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            status = executeHttpMethod(url, conn, "GET", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        return Response.ok().entity(status).build();
    }


    @PUT
    @Path("/{siUUID}/release")
    @Produces({ "application/json" })
    @ApiOperation(value = "Commit release service instance reservation", notes = "Transactionally populating service instance data to release reservation", response = Void.class, tags={ "connection",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully released", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 406, message = "Request unacceptable", response = Void.class),
        @ApiResponse(code = 409, message = "Resource conflict", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDReleasePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        Response response = serviceSiUUIDStatusGet(siUUID);
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource unfound:"+siUUID).build();
        }
        String status = response.getEntity().toString();
        String operation;
        if (status.equals("CREATE - READY")) {
            operation = "release";
        } else if (status.equals("CREATE - COMMITTED") || status.equals("CREATE - FAILED") ) {
            operation = "force_release";
        } else if (status.equals("CANCEL - FAILED")) {
            operation = "force_retry";
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Request unacceptable under status:"+status).build();
        }        
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(auth, refresh);
        try {
            URL url = new URL(String.format("%s/app/service/%s/"+operation, restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            executeHttpMethod(url, conn, "PUT", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        return Response.ok().build();    
    }


    @PUT
    @Path("/{siUUID}/terminate")
    @Produces({ "application/json" })
    @ApiOperation(value = "Commit service release", notes = "Committing service instance for resource deallocation and blocking for commit status", response = Void.class, tags={ "connection" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully terminated", response = Void.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response serviceSiUUIDTerminatePut(@PathParam("siUUID") @ApiParam("service instance UUID") String siUUID) {
        Response response = serviceSiUUIDStatusGet(siUUID);
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Resource unfound:"+siUUID).build();
        }
        String status = response.getEntity().toString();
        String operation;
        if (status.startsWith("CANCEL - PROPAGATED")) {
            operation = "commit";
        } else if (status.equals("CANCEL - FAILED")) {
            operation = "force_retry";
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity("Request unacceptable under status:"+status).build();
        }
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/app/service/%s/"+operation, restapi, siUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Refresh", refresh);
            executeHttpMethod(url, conn, "PUT", null, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        } 
        //@TODO: catch and parse other exceptions
        if (operation.equals("commit")) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                ;
            }
            boolean verifyStarted = false;
            while (true) {
                response = serviceSiUUIDStatusGet(siUUID);
                if (!response.getStatusInfo().equals(Response.Status.OK)) {
                    if (response.getEntity().toString().contains("Server returned HTTP response code: 401") || response.getStatusInfo().equals(Response.Status.UNAUTHORIZED)) {
                        continue;
                    }
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to poll status after commit").build();
                }
                status = response.getEntity().toString();

                if (status.startsWith("CANCEL - COMMITTED") && !verifyStarted) {
                    try {
                        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                        final TokenHandler token = new TokenHandler(auth, refresh);
                        URL url = new URL(String.format("%s/app/service/%s/verify", restapi, siUUID));
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Refresh", refresh);
                        executeHttpMethod(url, conn, "PUT", null, token.auth());
                        verifyStarted = true;
                    } catch (IOException ex) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
                    }
                } else if (status.equals("CANCEL - READY")) {
                    return Response.ok().build();
                } else if (status.equals("CANCEL - COMMITTING") || (status.startsWith("CANCEL - COMMITTED") && verifyStarted)) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        ;
                    }
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Request unacceptable under status:" + status).build();
                }
            }
        }

        return Response.ok().build();
    }
}
