package net.maxgigapop.mrs.rest.api;

import net.maxgigapop.mrs.rest.api.model.sense.DiscoveryDescription;
import net.maxgigapop.mrs.rest.api.model.sense.DomainDescription;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceDescription;
import net.maxgigapop.mrs.rest.api.model.sense.TopologyDescription;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import net.maxgigapop.mrs.common.TokenHandler;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import net.maxgigapop.mrs.rest.api.model.sense.DomainDescriptionEdgePoints;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceTerminationPoint;
import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Path("/sense/discovery")
@Api(description = "the discovery API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2018-02-08T15:27:04.431Z")
public class SenseDiscoveryApi {
    private final String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private final String kc_url = System.getProperty("kc_url");
    private final String keycloakStackVClientID = "5c0fab65-4577-4747-ad42-59e34061390b";
    JSONParser parser = new JSONParser();

    @Context
    private HttpRequest httpRequest;

    @GET
    @Path("/edgepoints/{domainID}")
    @ApiOperation(value = "Edge points discovery and description for a specific domain", notes = "List all associated edge points (and capabilities)", response = DomainDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topology desciptions", response = DomainDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class)})
    public Response discoveryEdgepointsDomainIDGet(@PathParam("domainID") @ApiParam("Name of URI of a target domain") String domainID) {
        final String jsonTemplate = "{\n" +
"	\"domain_uri\": \"?domain?\",\n" +
"	\"domain_name\": \"?name?\",\n" +
"	\"edge_points\": [\n" +
"		{\n" +
"			\"stp\": \"?ep?\",\n" +
"			\"peer\": \"?peer?\"\n" +
"		},\n" +
"		\"sparql\": \"SELECT ?ep ?peer WHERE {?domain? nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. ?other_domain nml:hasBidirectonalPort ?peer. ?other_domain a nml:Topology. FILTER (?other_domain != ?domain)} UNION {?domain? nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. ?other_domain nml:hasNode ?other_node. ?node_node nml:hasBidirectonalPort ?peer. ?other_domain a nml:Topology. FILTER (?other_domain != ?domain)}\"\n" +
"	],\n" +
String.format("	\"sparql\": \"SELECT ?domain ?name WHERE {?domain a nml:Topology. OPTIONAL {?domain nml:name ?name} FILTER (?domain = \"%s\" || ?name = \"%s\") }\",\n", domainID, domainID) +
"	\"required\": \"true\"\n" +
"}";
        String responseStr;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/service/manifest", host));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            responseStr = executeHttpMethod(url, conn, "POST", data, auth);
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        DomainDescription response = new DomainDescription();
        try {
            JSONObject jo = (JSONObject)parser.parse(responseStr);
            if (jo.containsKey("domain_name")) {
                response.setName((String)jo.get("domain_name"));
            }
            if (jo.containsKey("domain_uri")) {
             response.setUri((String)jo.get("domain_uri"));
            }
            if (jo.containsKey("edge_points")) {
                JSONArray ja = (JSONArray) jo.get("edge_points");
                List<DomainDescriptionEdgePoints> edgePoints = new ArrayList<DomainDescriptionEdgePoints>();
                response.setEdgePoints(edgePoints);
                for (Object obj: ja) {
                    DomainDescriptionEdgePoints ep = new DomainDescriptionEdgePoints();
                    JSONObject joEp = (JSONObject) obj;
                    ep.setPeerUri((String) joEp.get("peer"));
                    ServiceTerminationPoint stp = new ServiceTerminationPoint();
                    stp.setUri((String) joEp.get("stp"));
                    stp.setType("ethernet/vlan"); // temp hardcoded
                    ep.setStp(stp);
                }
            }
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        return Response.ok().entity(response).build();
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
