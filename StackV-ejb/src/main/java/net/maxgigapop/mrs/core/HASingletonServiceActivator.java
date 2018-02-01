/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

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

package net.maxgigapop.mrs.core;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;
import net.maxgigapop.mrs.common.StackLogger;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.wildfly.clustering.singleton.SingletonServiceName;

/**
 *
 * @author xyang
 */    
public class HASingletonServiceActivator implements ServiceActivator {
    private static final StackLogger logger = new StackLogger(HASingletonServiceActivator.class.getName(), "HASingletonServiceActivator");

    @Override
    public void activate(ServiceActivatorContext context) {
        String method = "activate";
        logger.start(method);
        HASingletonService service = new HASingletonService();
        //ServiceName factoryServiceName = ServiceName.parse("jboss.clustering.singleton.server.default");
        ServiceName factoryServiceName = SingletonServiceName.BUILDER.getServiceName("server", "default");
        ServiceController<?> factoryService = context.getServiceRegistry().getRequiredService(factoryServiceName);
        SingletonServiceBuilderFactory factory;
        SimpleSingletonElectionPolicy policy = new SimpleSingletonElectionPolicy();
        try {
            factory = (SingletonServiceBuilderFactory) factoryService.awaitValue();
            factory.createSingletonServiceBuilder(HASingletonService.SINGLETON_SERVICE_NAME, service)
                    .electionPolicy(policy)
                    .build(context.getServiceTarget())
                    .install();
        } catch (InterruptedException ex) {
            throw new ServiceRegistryException(ex);
        }
        try {
            InitialContext initialContext = new InitialContext();
            WritableServiceBasedNamingStore.pushOwner(HASingletonService.SINGLETON_SERVICE_NAME);
            initialContext.bind("java:global/HASingletonService", service);
        } catch (NamingException ex) {
            Logger.getLogger(HASingletonServiceActivator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        logger.end(method);
    }
}
