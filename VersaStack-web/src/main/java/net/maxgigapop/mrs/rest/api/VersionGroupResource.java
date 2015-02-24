/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * REST Web Service
 *
 * @author max
 */

@Path("VersionGroup")
@RequestScoped
public class VersionGroupResource {

    @Context
    private UriInfo context;
    
    @EJB
    HandleSystemCall systemCallHandler;
    
    private String VersionGroup;

    /**
     * Creates a new instance of VersionGroupResource
     */
    private VersionGroupResource(String VersionGroup) {
        this.VersionGroup = VersionGroup;
    }

    /**
     * Get instance of the VersionGroupResource
     */
    public static VersionGroupResource getInstance(String VersionGroup) {
        // The user may use some kind of persistence mechanism
        // to store and restore instances of VersionGroupResource class.
        return new VersionGroupResource(VersionGroup);
    }

    /**
     * Retrieves representation of an instance of net.maxgigapop.mrs.rest.api.VersionGroupResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces({"application/xml","application/json"})
    public String pull() {
        //TODO return proper representation object
        throw new UnsupportedOperationException();
    }

    /**
     * PUT method for updating or creating an instance of VersionGroupResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/xml")
    public void putXml(String content) {
    }

    /**
     * DELETE method for resource VersionGroupResource
     */
    @DELETE
    public void delete() {
    }
}
