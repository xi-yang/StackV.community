package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CORSRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {        
        requestContext.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        requestContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        requestContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        requestContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
    }
}