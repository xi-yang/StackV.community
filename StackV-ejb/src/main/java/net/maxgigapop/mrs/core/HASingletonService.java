/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.core;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Remote;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import net.maxgigapop.mrs.common.StackLogger;
import org.jboss.as.naming.WritableServiceBasedNamingStore;

/**
 *
 * @author xyang
 */
public class HASingletonService implements Service<DataConcurrencyPoster> {
    private static final StackLogger logger = new StackLogger(HASingletonService.class.getName(), "HASingletonService");
    public static final ServiceName SINGLETON_SERVICE_NAME = ServiceName.JBOSS.append("stackv", "ha", "singleton", "service");

    /**
     * A flag whether the service is started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    private DataConcurrencyPoster dataPoster = new DataConcurrencyPoster();

    @Override
    public DataConcurrencyPoster getValue() throws IllegalStateException, IllegalArgumentException {
        return dataPoster;
    }

    @Override
    public void start(StartContext arg0) throws StartException {
        String method = "start";
        logger.start(method);
        if (!started.compareAndSet(false, true)) {
            throw new StartException("The service is still started!");
        }
        dataPoster.setSystemModelCoordinator_bootStrapped(false);
        dataPoster.setSystemModelCoordinator_cachedOntModel(null);
        try {
            Thread.sleep(30000); // sleep for 10 secs, waiting for JNDI
        } catch (InterruptedException ex) {
            ;
        }
        final String node = System.getProperty("jboss.node.name");
        logger.message(method, "Start HASingleton DriverModelPuller service '" + this.getClass().getName() + "' on Node: "+node);
        try {
            InitialContext ic = new InitialContext();
            ((net.maxgigapop.mrs.core.DriverModelPuller) ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/DriverModelPuller"))
                    .start();
        } catch (NamingException e) {
            throw new StartException("Could not initialize DriverModelPuller", e);
        }
        try {
            InitialContext ic = new InitialContext();
            ((net.maxgigapop.mrs.core.SystemModelCoordinator) ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/SystemModelCoordinator"))
                    .start();
        } catch (NamingException e) {
            throw new StartException("Could not initialize SystemModelCoordinator", e);
        }
        try {
            InitialContext initialContext = new InitialContext();
            WritableServiceBasedNamingStore.pushOwner(HASingletonService.SINGLETON_SERVICE_NAME);
            initialContext.bind("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/DataConcurrencyPoster", dataPoster);
        } catch (NamingException ex) {
            Logger.getLogger(HASingletonServiceActivator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        logger.end(method);
    }

    @Override
    public void stop(StopContext arg0) {
        String method = "stop";
        logger.start(method);
        if (!started.compareAndSet(true, false)) {
            logger.warning(method, "The service '" + this.getClass().getName() + "' is not active!");
        } else {
            logger.message(method, "Stop HASingleton timer service '" + this.getClass().getName() + "'");
            try {
                InitialContext ic = new InitialContext();
                ((net.maxgigapop.mrs.core.DriverModelPuller) ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/DriverModelPuller"))
                        .stop();
            } catch (NamingException e) {
                logger.error(method, "Could not stop DriverModelPuller:" + e.getMessage());
            }
            try {
                InitialContext ic = new InitialContext();
                ((net.maxgigapop.mrs.core.SystemModelCoordinator) ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/SystemModelCoordinator"))
                        .stop();
            } catch (NamingException e) {
                logger.error(method, "Could not stop SystemModelCoordinator:" + e.getMessage());
            }
        }
        try {
            InitialContext initialContext = new InitialContext();
            WritableServiceBasedNamingStore.pushOwner(HASingletonService.SINGLETON_SERVICE_NAME);
            initialContext.unbind("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/DataConcurrencyPoster");
        } catch (NamingException ex) {
            Logger.getLogger(HASingletonServiceActivator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            WritableServiceBasedNamingStore.popOwner();
        }
        logger.end(method);
    }
}