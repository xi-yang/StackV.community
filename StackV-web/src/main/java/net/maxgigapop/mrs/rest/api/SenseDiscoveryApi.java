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
    private final String restapi = "http://127.0.0.1:8080/StackV-web/restapi";
    JSONParser parser = new JSONParser();

    @Context
    private HttpRequest httpRequest;

    @GET
    @Path("/edgepoints/{domainID}")
    @Produces("application/json")
    @ApiOperation(value = "Edge points discovery and description for a specific domain", notes = "List all associated edge points (and capabilities)", response = DomainDescription.class, tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topology desciptions", response = DomainDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class)})
    public Response discoveryEdgepointsDomainIDGet(@PathParam("domainID") @ApiParam("Name of URI of a target domain") String domainID) {
        final String jsonTemplate = "{\n" +
"	\"domain\": \"?domain?\",\n" +
"	\"domain_name\": \"?domain_name?\",\n" +
"	\"edge_points\": [\n" +
"		{\n" +
"			\"stp\": \"?ep?\",\n" +
"			\"peer\": \"?peer?\",\n" +
"      \"sparql\": \"SELECT DISTINCT ?ep ?peer WHERE { {?domain nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. FILTER (NOT EXISTS {?other_domain nml:hasBidirectionalPort ?peer. ?other_domain a nml:Topology} &amp;&amp; NOT EXISTS {?other_domain nml:hasNode ?other_node. ?other_node nml:hasBidirectionalPort ?peer. ?other_domain a nml:Topology}) } UNION {?domain nml:hasNode ?node. ?node nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. FILTER (NOT EXISTS {?other_domain nml:hasBidirectionalPort ?peer. ?other_domain a nml:Topology} &amp;&amp; NOT EXISTS {?other_domain nml:hasNode ?other_node. ?other_node nml:hasBidirectionalPort ?peer. ?other_domain a nml:Topology}) } }\"\n" +
"		}\n" +
"	],\n" +
String.format("	\"sparql\": \"SELECT DISTINCT ?domain ?domain_name WHERE {?domain a nml:Topology. OPTIONAL {?domain nml:name ?domain_name} FILTER (?domain=&lt;%s&gt; || ?domain_name=&lt;%s&gt;) }\",\n", domainID, domainID) +
"	\"required\": \"true\"\n" +
"}";
        String responseStr;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/service/manifest", restapi));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        DomainDescription response = new DomainDescription();
        try {
            JSONObject jo = (JSONObject)parser.parse(responseStr);
            jo = (JSONObject)parser.parse((String)jo.get("jsonTemplate"));
            if (jo.containsKey("domain_name") && !((String)jo.get("domain_name")).startsWith("?")) {
                response.setName((String)jo.get("domain_name"));
            }
            if (jo.containsKey("domain")) {
             response.setUri((String)jo.get("domain"));
            }
            if (jo.containsKey("edge_points")) {
                JSONArray ja = (JSONArray) jo.get("edge_points");
                List<DomainDescriptionEdgePoints> edgePoints = new ArrayList();
                response.setEdgePoints(edgePoints);
                for (Object obj: ja) {
                    DomainDescriptionEdgePoints ep = new DomainDescriptionEdgePoints();
                    JSONObject joEp = (JSONObject) obj;
                    ep.setPeerUri((String) joEp.get("peer"));
                    ServiceTerminationPoint stp = new ServiceTerminationPoint();
                    stp.setUri((String) joEp.get("stp"));
                    stp.setType("ethernet/vlan"); // temp hardcoded
                    ep.setStp(stp);
                    edgePoints.add(ep);
                }
            }
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        return Response.ok().entity(response).build();
    }

    @GET
    @Path("/edgepoints/{domainID}/peer")
    @Produces("application/json")
    @ApiOperation(value = "edge points discovery and description of peer domain for a given domain or end-site", notes = "List peer domain edge points (and capabilities) that connect this domain (by URI or name)", response = DomainDescription.class, responseContainer = "List", tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topology desciptions", response = DomainDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryEdgepointsDomainIDPeerGet(@PathParam("domainID") @ApiParam("Name of URI of a target end-site domain") String domainID) {
        final String jsonTemplate = "{\n" +
"    \"edge_points\": [\n" +
"        {\n" +
"           \"peer_stp\": \"?peer_stp?\",\n" +
"           \"peer_domain\": \"?peer_domain?\",\n" +
"           \"peer_domain_name\": \"?peer_domain_name?\",\n" +
String.format("           \"sparql\": \"SELECT DISTINCT ?peer_stp ?peer_domain ?peer_domain_name WHERE { { { ?peer_domain nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology. ?peer_stp nml:isAlias ?stp. OPTIONAL {?peer_domain nml:name ?peer_domain_name} } UNION {?peer_domain nml:hasNode ?peer_node. ?peer_node nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology. ?peer_stp nml:isAlias ?stp. OPTIONAL {?peer_domain nml:name ?peer_domain_name} } FILTER ((?stp=&lt;%s&gt;) &amp;&amp; (NOT EXISTS {?parent_domain nml:hasTopology ?peer_domain})  &amp;&amp; (NOT EXISTS {?peer_domain nml:hasBidirectionalPort ?stp}) &amp;&amp; (NOT EXISTS {?peer_domain nml:hasNode ?other_node. ?other_node nml:hasBidirectionalPort ?stp}) ) } UNION {{ ?peer_domain nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology.  OPTIONAL {?domain nml:hasNode ?node. ?node nml:hasBidirectionalPort ?stp} OPTIONAL {?peer_domain nml:name ?peer_domain_name} OPTIONAL {?domain nml:name ?domain_name} } UNION {?peer_domain nml:hasNode ?peer_node. ?peer_node nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology. OPTIONAL {?domain nml:hasNode ?node. ?node nml:hasBidirectionalPort ?stp} OPTIONAL {?peer_domain nml:name ?peer_domain_name} OPTIONAL {?domain nml:name ?domain_name} } UNION  { ?peer_domain nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology. OPTIONAL {?domain nml:hasBidirectionalPort ?stp} OPTIONAL {?peer_domain nml:name ?peer_domain_name} OPTIONAL {?domain nml:name ?domain_name} } UNION {?peer_domain nml:hasNode ?peer_node. ?peer_node nml:hasBidirectionalPort ?peer_stp. ?peer_domain a nml:Topology. OPTIONAL {?domain nml:hasBidirectionalPort ?stp} OPTIONAL {?peer_domain nml:name ?peer_domain_name} OPTIONAL {?domain nml:name ?domain_name} } FILTER ((?domain=&lt;%s&gt; || ?domain_name = '%s') &amp;&amp; (EXISTS {?domain a nml:Topology}) &amp;&amp; (EXISTS {?peer_stp nml:isAlias ?stp.} || EXISTS {?stp nml:isAlias ?peer_stp.})  &amp;&amp; (NOT EXISTS {?parent_domain nml:hasTopology ?peer_domain}) &amp;&amp; (?peer_domain != ?domain))} }\"\n", domainID, domainID, domainID) +
"		}\n" +
"    ]\n" +
"}";
        String responseStr;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/service/manifest", restapi));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        List<DomainDescription> response = new ArrayList();
        try {
            JSONObject jo = (JSONObject)parser.parse(responseStr);
            jo = (JSONObject)parser.parse((String)jo.get("jsonTemplate"));
            if (jo.containsKey("edge_points")) {
                JSONArray ja = (JSONArray) jo.get("edge_points");
                for (Object obj: ja) {
                    JSONObject joEp = (JSONObject) obj;
                    String peerStp = (String) joEp.get("peer_stp");
                    String peerDomainUri = (String) joEp.get("peer_domain");
                    String peerDomainName = (String) joEp.get("peer_domain_name");
                    if (peerDomainName.startsWith("?")) {
                        peerDomainName = null;
                    }
                    DomainDescription domainInList = null;
                    for (DomainDescription domainDesc: response) {
                        if (domainDesc.getUri().equals(peerDomainUri)) {
                            domainInList = domainDesc;
                            break;
                        }
                    }
                    if (domainInList == null) {
                        domainInList = new DomainDescription().uri(peerDomainUri)
                                .name(peerDomainName)
                                .edgePoints(new ArrayList());
                        response.add(domainInList);
                    }
                    DomainDescriptionEdgePoints ep = new DomainDescriptionEdgePoints().stp(new ServiceTerminationPoint().uri(peerStp));
                    domainInList.getEdgePoints().add(ep);
                }
            }
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        return Response.ok().entity(response).build();
    }

    @GET
    @Path("/edgepoints")
    @Produces("application/json")
    @ApiOperation(value = "Topology edge points discovery and description", notes = "List all known domains (and capabilities?)", response = DomainDescription.class, responseContainer = "List", tags={ "discovery",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful retrieval of topoogy desciptions", response = TopologyDescription.class),
        @ApiResponse(code = 401, message = "Request unauthorized", response = Void.class),
        @ApiResponse(code = 404, message = "Resource unfound", response = Void.class),
        @ApiResponse(code = 500, message = "Server internal error", response = Void.class) })
    public Response discoveryEdgepointsGet() {
        final String jsonTemplate = "{\n" +
"    \"all_domains\": [\n" +
"        {\n" +
"	\"domain\": \"?domain?\",\n" +
"	\"domain_name\": \"?domain_name?\",\n" +
"	\"edge_points\": [\n" +
"		{\n" +
"			\"stp\": \"?ep?\",\n" +
"			\"peer\": \"?peer?\",\n" +
"      \"sparql\": \"SELECT DISTINCT ?ep ?peer WHERE { {?domain nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. FILTER (NOT EXISTS {?domain nml:hasBidirectionalPort ?peer.} &amp;&amp; NOT EXISTS {?domain nml:hasNode ?other_node. ?other_node nml:hasBidirectionalPort ?peer}) } UNION {?domain nml:hasNode ?node. ?node nml:hasBidirectionalPort ?ep. ?ep nml:isAlias ?peer. FILTER (NOT EXISTS {?domain nml:hasBidirectionalPort ?peer} &amp;&amp; NOT EXISTS {?domain nml:hasNode ?other_node. ?other_node nml:hasBidirectionalPort ?peer}) } }\"\n" +
"		}\n" +
"	],\n" +
"	\"sparql\": \"SELECT DISTINCT ?domain ?domain_name WHERE {?domain a nml:Topology. OPTIONAL {?domain nml:name ?domain_name} FILTER NOT EXISTS {?parent_domain nml:hasTopology ?domain} }\",\n" +
"	\"required\": \"true\"\n" +
"       }\n" +
"    ]\n" +
"}";
        String responseStr;
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(auth, refresh);
            URL url = new URL(String.format("%s/service/manifest", restapi));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        List<DomainDescription> response = new ArrayList();
        try {
            JSONObject jo = (JSONObject)parser.parse(responseStr);
            jo = (JSONObject)parser.parse((String)jo.get("jsonTemplate"));
            if (jo.containsKey("all_domains")) {
                JSONArray ja = (JSONArray) jo.get("all_domains");
                for (Object obj : ja) {
                    JSONObject joDomain = (JSONObject) obj;
                    String domainUri = (String) joDomain.get("domain");
                    String domainName = (String) joDomain.get("domain_name");
                    if (domainName.startsWith("?")) {
                        domainName = null;
                    }
                    DomainDescription domainDesc = new DomainDescription().uri(domainUri)
                                .name(domainName)
                                .edgePoints(new ArrayList());
                    response.add(domainDesc);
                    if (joDomain.containsKey("edge_points")) {
                        JSONArray jaEp = (JSONArray) joDomain.get("edge_points");
                        for (Object objEp : jaEp) {
                            DomainDescriptionEdgePoints ep = new DomainDescriptionEdgePoints();
                            JSONObject joEp = (JSONObject) objEp;
                            ep.setPeerUri((String) joEp.get("peer"));
                            ServiceTerminationPoint stp = new ServiceTerminationPoint();
                            stp.setUri((String) joEp.get("stp"));
                            stp.setType("ethernet/vlan"); // temp hardcoded
                            ep.setStp(stp);
                            domainDesc.getEdgePoints().add(ep);
                        }
                    }
                }
            }
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
        }
        return Response.ok().entity(response).build();
    }

    @GET
    @Produces("application/json")
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
    @Produces("application/json")
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
