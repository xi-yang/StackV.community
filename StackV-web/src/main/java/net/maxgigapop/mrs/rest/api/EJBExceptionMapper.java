/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
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
