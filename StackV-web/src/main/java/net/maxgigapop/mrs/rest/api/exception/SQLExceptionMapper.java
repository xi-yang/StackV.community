/*
 * Copyright (c) 2013-201 University of Maryland
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
package net.maxgigapop.mrs.rest.api.exception;

import java.sql.SQLException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONObject;

/**
 *
 * @author rikenavadur
 */
@Provider
public class SQLExceptionMapper implements ExceptionMapper<SQLException> {
    @Override
    public Response toResponse(SQLException exception) {
        JSONObject jsonErrResponse = new JSONObject();
        jsonErrResponse.put("type", exception.toString());
        jsonErrResponse.put("exception", exception.getMessage());
        jsonErrResponse.put("cause", exception.getCause());
        jsonErrResponse.put("stacktrace", ExceptionUtils.getStackTrace(exception));                
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonErrResponse.toJSONString()).build();
    }
}
