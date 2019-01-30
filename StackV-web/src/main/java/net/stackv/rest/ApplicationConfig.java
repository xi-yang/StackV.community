/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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
package net.stackv.rest;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 *
 * @author xyang
 */
@ApplicationPath("/restapi/")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        addExceptionMapperClasses(resources);
        return resources;
    }

    private void addExceptionMapperClasses(Set<Class<?>> resources) {
        resources.add(net.stackv.rest.exception.EJBExceptionMapper.class);
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        resources.add(net.stackv.rest.CORSResponseFilter.class);
        resources.add(net.stackv.rest.ConfigResource.class);
        resources.add(net.stackv.rest.DeltaResource.class);
        resources.add(net.stackv.rest.DriverResource.class);
        resources.add(LoggingResource.class);
        resources.add(MD2Resource.class);
        resources.add(net.stackv.rest.ModelResource.class);
        resources.add(net.stackv.rest.SecurityInterceptor.class);
        resources.add(net.stackv.rest.SenseDiscoveryApi.class);
        resources.add(net.stackv.rest.SenseServiceApi.class);
        resources.add(net.stackv.rest.ServiceResource.class);
        resources.add(net.stackv.rest.WebResource.class);
        resources.add(net.stackv.rest.exception.EJBExceptionMapper.class);
        resources.add(net.stackv.rest.exception.IOExceptionMapper.class);
        resources.add(net.stackv.rest.exception.ParseExceptionMapper.class);
        resources.add(net.stackv.rest.exception.SQLExceptionMapper.class);
    }
}
