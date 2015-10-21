/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import net.maxgigapop.mrs.rest.api.model.ApiDriverInstance;
import net.maxgigapop.mrs.rest.api.model.ApiVMInstall;
import net.maxgigapop.mrs.system.HandleSystemCall;
import web.beans.serviceBeans;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("web")
public class WebResource {

    protected serviceBeans serv = new serviceBeans();

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public WebResource() {
    }

    @GET
    @Path("/users")
    @Produces("application/json")
    public ArrayList<String> getUsers() throws SQLException {
        String front_db_user = "front_view";
        String front_db_pass = "frontuser";
        ArrayList<String> retList = new ArrayList<>();

        Connection front_conn;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT username FROM user_info");
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            retList.add(rs1.getString(1));
        }

        return retList;
    }

    @POST
    @Path("/installVM")
    @Consumes({"application/xml", "application/json"})
    public String installVM(ApiVMInstall input) {
        try {
            Map<String, String> propMap = input.getProperties();

            int retCode = serv.vmInstall(propMap);
            switch (retCode) {
                case 4:
                    return "Parsing parameter error.\n";
                case 3:
                    return "Connection error.\n";
                case 2:
                    return "Plugin error.\n";
                case 1:
                    return "Requesting System Instance UUID error.\n";
                default:
                    return "Success.\n";
            }
        } catch (EJBException e) {
            return e.getMessage();
        }
    }
}
