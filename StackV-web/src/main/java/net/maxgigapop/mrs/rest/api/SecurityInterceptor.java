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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

@Provider
@ServerInterceptor
public class SecurityInterceptor implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    private final Logger logger = LogManager.getLogger(SecurityInterceptor.class.getName());
    // private static final ServerResponse ACCESS_DENIED = new
    // ServerResponse("Access denied for this resource.\n", 401, new
    // Headers<Object>());
    // private static final ServerResponse SERVER_ERROR = new
    // ServerResponse("INTERNAL SERVER ERROR\n", 500, new Headers<Object>());

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ThreadContext.clearMap();
        UriInfo uri = requestContext.getUriInfo();

        if ((uri.getPath()).startsWith("/app/")) {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) requestContext
                    .getProperty(KeycloakSecurityContext.class.getName());
            Set<String> roleSet;
            AccessToken accessToken = securityContext.getToken();
            String method = resourceInfo.getResourceMethod().getName();
            RolesAllowed rolesAnnotation = resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class);
            String role;

            // Ban lists
            List<String> quietRoles = Arrays.asList("F_Drivers-R");

            if (rolesAnnotation == null) {
                logger.trace("Authenticated Freely.");
                return;
            } else {
                role = Arrays.asList(rolesAnnotation.value()).get(0);
            }

            ThreadContext.put("username", accessToken.getPreferredUsername());
            ThreadContext.put("method", method);
            ThreadContext.put("role", role);
            roleSet = accessToken.getRealmAccess().getRoles();

            if (roleSet.contains(role) && (quietRoles.contains(role) || method.equals("subStatus"))) {
                return;
            }

            logger.trace("API Request Received: {}.", uri.getPath());
            if (!accessToken.isActive()) {
                logger.warn("Token is not active.");
                return;
            }

            if (!roleSet.contains(role)) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("User is not allowed to access the resource:" + method).build());
                logger.warn("Denied.");
                return;
            }

            logger.info("Authenticated.");
        }
    }
}
