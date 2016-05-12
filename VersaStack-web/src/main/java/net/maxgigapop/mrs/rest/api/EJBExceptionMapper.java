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
import org.json.simple.JSONObject;

/**
 *
 * @author xyang
 */
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {
    public Response toResponse(EJBException exception) {
        JSONObject jsonErrResponse = new JSONObject();
        jsonErrResponse.put("exception", exception.getMessage());
        StackTraceElement[] stackTrace = null;
        if (exception.getCausedByException() != null) {
            String causedMessage = exception.getCausedByException().getMessage();
            if (causedMessage != null && !causedMessage.isEmpty()) {
                jsonErrResponse.put("causedby", causedMessage);
            }
            // get stack trace for causedby
            stackTrace = exception.getCausedByException().getStackTrace();
        } else {
            // get stack trace for exception
            stackTrace = exception.getStackTrace();
        }
        if (stackTrace != null) {
            String trace = "";
            for (StackTraceElement elem: stackTrace) {
                if (!trace.isEmpty()) {
                    trace += ";";
                }
                trace += elem.toString();
            }
            jsonErrResponse.put("stacktrace", trace);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonErrResponse.toJSONString()).build();
    }
}
