/*
* Copyright (c) 2013-2019 University of Maryland
* Created by: Alberto Jimenez 2015
* Modified by: Tao-Hung Yang 2016
* Modified by: Xi Yang 2016
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and/or hardware specification (the “Work”) to deal in the
* Work without restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
* the Work, and to permit persons to whom the Work is furnished to do so,
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Work.
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
* IN THE WORK.
 */
package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

import javax.ejb.EJB;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.UnhandledException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import net.maxgigapop.mrs.common.AuditService;
import net.maxgigapop.mrs.common.KeycloakHandler;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * Logging Resource
 *
 * @author rikenavadur
 */
@Path("logging")
public class LoggingResource {
    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "LoggingResource");
    private static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private static JSONParser parser = new JSONParser();

    private static final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();
    private static final OkHttpClient client = new OkHttpClient();

    private final JNDIFactory factory = new JNDIFactory();

    @Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

    static {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

        } };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // set the allTrusting verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {

        }
    }

    @GET
    @Path("/logs")
    @Produces("application/json")
    public String getLogs(@QueryParam("refUUID") String refUUID, @QueryParam("level") String level)
            throws SQLException {
        String method = "getLogs";
        try (Connection front_conn = factory.getConnection("frontend");) {
            String prepString = filterPrepString(refUUID, level);
            try (PreparedStatement prep = front_conn.prepareStatement(prepString)) {
                try (ResultSet rs = prep.executeQuery();) {
                    JSONObject retJSON = new JSONObject();
                    JSONArray logArr = new JSONArray();
                    while (rs.next()) {
                        JSONObject logJSON = new JSONObject();

                        logJSON.put("referenceUUID", rs.getString("referenceUUID"));
                        logJSON.put("marker", rs.getString("marker"));
                        logJSON.put("timestamp", rs.getString("timestamp"));
                        logJSON.put("level", rs.getString("level"));
                        logJSON.put("logger", rs.getString("logger"));
                        logJSON.put("message", rs.getString("message"));
                        logJSON.put("event", rs.getString("event"));
                        logJSON.put("exception", rs.getString("exception"));
                        logJSON.put("targetID", rs.getString("targetID"));

                        logArr.add(logJSON);
                    }
                    retJSON.put("data", logArr);

                    return retJSON.toJSONString();
                }
            }
        } catch (UnhandledException ex) {
            logger.trace(method, "Logging connection lost?");
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        }
    }

    @GET
    @Path("/logs/serverside")
    @Produces("application/json")
    public String getLogsServerSide(@Context UriInfo uriInfo) throws SQLException {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String level = queryParams.getFirst("level");
        String refUUID = queryParams.getFirst("refUUID");
        String search = queryParams.getFirst("search[value]");
        int draw = Integer.parseInt(queryParams.getFirst("draw"));
        int start = Integer.parseInt(queryParams.getFirst("start"));
        int length = Integer.parseInt(queryParams.getFirst("length"));

        String method = "getLogsServerSide";
        try (Connection front_conn = factory.getConnection("frontend");) {
            String prepString = filterPrepString(refUUID, level);
            if (!search.equals("")) {
                if (prepString.contains("WHERE")) {
                    prepString = prepString + " AND (logger LIKE '%" + search + "%' " + "OR module LIKE '%" + search
                            + "%' " + "OR method LIKE '%" + search + "%' " + "OR event LIKE '%" + search + "%' "
                            + "OR message LIKE '%" + search + "%') ";
                } else {
                    prepString = prepString + " WHERE (logger LIKE '%" + search + "%' " + "OR module LIKE '%" + search
                            + "%' " + "OR method LIKE '%" + search + "%' " + "OR event LIKE '%" + search + "%' "
                            + "OR message LIKE '%" + search + "%') ";
                }
            }

            String prepStringCount = prepString.replace("SELECT *", "SELECT COUNT(*)");
            int count;
            try (PreparedStatement prep = front_conn.prepareStatement(prepStringCount);) {
                if (refUUID != null) {
                    prep.setString(1, refUUID);
                }
                try (ResultSet rs = prep.executeQuery();) {
                    rs.next();
                    count = rs.getInt(1);
                }
            }

            prepString = prepString + " ORDER BY log_id DESC LIMIT ?,?";
            try (PreparedStatement prep = front_conn.prepareStatement(prepString);) {
                if (refUUID != null) {
                    prep.setString(1, refUUID);
                    prep.setInt(2, start);
                    prep.setInt(3, length);
                } else {
                    prep.setInt(1, start);
                    prep.setInt(2, length);
                }

                try (ResultSet rs = prep.executeQuery();) {
                    JSONObject retJSON = new JSONObject();
                    JSONArray logArr = new JSONArray();
                    while (rs.next()) {
                        JSONObject logJSON = new JSONObject();

                        logJSON.put("referenceUUID", rs.getString("referenceUUID"));
                        logJSON.put("marker", rs.getString("marker"));
                        logJSON.put("timestamp", rs.getString("timestamp"));
                        logJSON.put("level", rs.getString("level"));
                        logJSON.put("logger", rs.getString("logger"));
                        logJSON.put("message", rs.getString("message"));
                        logJSON.put("event", rs.getString("event"));
                        logJSON.put("exception", rs.getString("exception"));
                        logJSON.put("targetID", rs.getString("targetID"));

                        logArr.add(logJSON);
                    }

                    retJSON.put("data", logArr);
                    retJSON.put("draw", draw);
                    retJSON.put("recordsTotal", count);
                    retJSON.put("recordsFiltered", count);

                    return retJSON.toJSONString();
                }
            }
        } catch (UnhandledException ex) {
            logger.trace(method, "Logging connection lost?");
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        }
    }

    @GET
    @Path("/instances")
    @Produces("application/json")
    public String loadInstanceData() throws SQLException, IOException {
        String method = "loadInstanceData";
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                .getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        String username = accessToken.getPreferredUsername();
        boolean isAdmin = KeycloakHandler.verifyUserRole(httpRequest, "A_Admin");

        try (Connection front_conn = factory.getConnection("frontend");) {
            String prepString;
            if (isAdmin) {
                prepString = "SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state, I.creation_time, I.last_state, I.username, V.verification_state "
                        + "FROM service_instance I, service_verification V "
                        + "WHERE V.service_instance_id = I.service_instance_id " + "ORDER BY I.creation_time";
            } else {
                prepString = "SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state, I.creation_time, I.last_state, I.username, V.verification_state "
                        + "FROM service_instance I, acl A, service_verification V "
                        + "WHERE I.referenceUUID = A.object AND V.service_instance_id = I.service_instance_id AND (A.subject = ? OR I.username = ?) "
                        + "ORDER BY I.creation_time";
            }

            try (PreparedStatement prep = front_conn.prepareStatement(prepString)) {
                if (!isAdmin) {
                    prep.setString(1, username);
                    prep.setString(2, username);
                }
                try (ResultSet rs = prep.executeQuery();) {
                    JSONObject retJSON = new JSONObject();
                    JSONArray logArr = new JSONArray();
                    while (rs.next()) {
                        JSONObject logJSON = new JSONObject();
                        String instanceUUID = rs.getString("referenceUUID");
                        String instanceState = rs.getString("super_state");
                        try {
                            URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                            Request request = new Request.Builder().url(url).header("Authorization", auth).build();
                            Response response = client.newCall(request).execute();
                            String responseStr = response.body().string();

                            instanceState = instanceState + " - " + responseStr;
                        } catch (IOException ex) {
                            AuditService.cleanInstances("frontend");
                            logger.catching(method, ex);
                        }

                        logJSON.put("alias", rs.getString("alias_name"));
                        logJSON.put("type", WebResource.Services.get(rs.getString("type")).get(0));
                        logJSON.put("referenceUUID", instanceUUID);
                        logJSON.put("state", instanceState);
                        logJSON.put("owner", rs.getString("username"));
                        logJSON.put("lastState", rs.getString("last_state"));
                        logJSON.put("verification", rs.getString("verification_state"));
                        logJSON.put("timestamp", rs.getString("creation_time"));

                        logArr.add(logJSON);
                    }
                    retJSON.put("data", logArr);

                    return retJSON.toJSONString();
                }
            }
        } catch (SQLException ex) {
            // AuditService.cleanInstances("frontend");
            logger.catching(method, ex);
            throw ex;
        }
    }

    private String filterPrepString(String UUID, String level) {
        String prepString = "SELECT * FROM log";
        // Filtering by UUID alone
        if (UUID != null && (level == null || level.equalsIgnoreCase("TRACE"))) {
            prepString = prepString + " WHERE referenceUUID = ?";
        } // Filtering by level alone
        else if (UUID == null && level != null) {
            switch (level) {
            case "INFO":
                prepString = prepString + " WHERE level != 'TRACE'";
                break;
            case "WARN":
                prepString = prepString + " WHERE level != 'TRACE' AND level != 'INFO'";
                break;
            case "ERROR":
                prepString = prepString + " WHERE level = 'ERROR'";
                break;
            }
        } // Filtering by both
        else if (UUID != null && level != null) {
            switch (level) {
            case "TRACE":
                prepString = prepString + " WHERE referenceUUID = ?";
                break;
            case "INFO":
                prepString = prepString + " WHERE referenceUUID = ? AND level != 'TRACE'";
                break;
            case "WARN":
                prepString = prepString + " WHERE referenceUUID = ? AND level != 'TRACE' AND level != 'INFO'";
                break;
            case "ERROR":
                prepString = prepString + " WHERE referenceUUID = ? AND level = 'ERROR'";
                break;
            }
        }

        return prepString;
    }
}