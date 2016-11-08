/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

@Provider
@ServerInterceptor
public class SecurityInterceptor implements PreProcessInterceptor {

    private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource.\n", 401, new Headers<Object>());
    private static final ServerResponse SERVER_ERROR = new ServerResponse("INTERNAL SERVER ERROR\n", 500, new Headers<Object>());

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethodInvoker method)
            throws Failure, WebApplicationException {
        ServerResponse response;

        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        

        if (false) {
            return ACCESS_DENIED;
        }
        return null;
    }
}
