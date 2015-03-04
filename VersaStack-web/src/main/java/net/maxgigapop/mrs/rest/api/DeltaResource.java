/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
public class DeltaResource {
    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;
    
    public DeltaResource(){
    }
    
    
    
}
