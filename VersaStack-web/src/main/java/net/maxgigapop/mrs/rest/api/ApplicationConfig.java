/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.Set;
import javax.ejb.EJBException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
        resources.add(net.maxgigapop.mrs.rest.api.EJBExceptionMapper.class);
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(net.maxgigapop.mrs.rest.api.DeltaResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.DriverResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.EJBExceptionMapper.class);
        resources.add(net.maxgigapop.mrs.rest.api.ModelResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.ServiceResource.class);
        resources.add(net.maxgigapop.mrs.rest.api.WebResource.class);
    }
}
