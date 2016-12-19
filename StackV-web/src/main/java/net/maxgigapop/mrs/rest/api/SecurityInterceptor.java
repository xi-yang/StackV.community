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
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

@Provider
@ServerInterceptor
public class SecurityInterceptor implements PreProcessInterceptor {

    private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource.\n", 401, new Headers<Object>());
    private static final ServerResponse SERVER_ERROR = new ServerResponse("INTERNAL SERVER ERROR\n", 500, new Headers<Object>());
    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethodInvoker method) {
        if ((request.getUri().getPath()).startsWith("/app/")) {
            // Ban list
            List<String> supplierNames = Arrays.asList("loadWizard", "loadEditor", "loadInstances",
                    "loadInstanceDetails", "loadInstanceDelta", "loadInstanceVerification", 
                    "loadObjectACL", "loadSubjectACL", "subStatus", "getProfile", "getLabels", 
                    "executeProfile", "deleteProfile");
            String methodName = method.getMethod().getName();
            if (supplierNames.contains(methodName)) {
                return null;
            }

            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
            Set<String> roleSet;
            AccessToken accessToken = securityContext.getToken();
            System.out.println("TOKEN ACTIVE: " + accessToken.isActive());
            roleSet = accessToken.getResourceAccess("StackV").getRoles();
            if (!accessToken.isActive()) {

            }

            System.out.println("Method: " + methodName);
            if (roleSet.contains(methodName)) {
                return null;
            } else {
                System.out.println("Not Authorized!");
                return ACCESS_DENIED;
            }
        } else {
            return null;
        }
    }
}
