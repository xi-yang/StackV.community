/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import javax.ejb.EJBException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author xyang
 */
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {
    public Response toResponse(EJBException exception) {
        String errResponse = "";
        if (exception.getCausedByException() == null) {
            errResponse = exception.getMessage();
        } else {
            errResponse = exception.getCausedByException().getMessage();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errResponse).build();
    }
}
