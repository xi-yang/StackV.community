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

package net.maxgigapop.mrs.rest.api;

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
        resources.add(net.maxgigapop.mrs.rest.api.exception.EJBExceptionMapper.class);
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(net.maxgigapop.mrs.rest.api.CORSResponseFilter.class);
        resources.add(net.maxgigapop.mrs.rest.api.DeltaResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.DriverResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.IPAResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.ModelResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.SecurityInterceptor.class);
        resources.add(net.maxgigapop.mrs.rest.api.SenseDiscoveryApi.class);
        resources.add(net.maxgigapop.mrs.rest.api.SenseServiceApi.class);
        resources.add(net.maxgigapop.mrs.rest.api.ServiceResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.WebResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.exception.EJBExceptionMapper.class);
        resources.add(net.maxgigapop.mrs.rest.api.exception.IOExceptionMapper.class);
        resources.add(net.maxgigapop.mrs.rest.api.exception.ParseExceptionMapper.class);
        resources.add(net.maxgigapop.mrs.rest.api.exception.SQLExceptionMapper.class);
    }
}
