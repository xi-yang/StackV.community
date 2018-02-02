/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.core;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import net.maxgigapop.mrs.common.StackLogger;

/**
 *
 * @author xyang
 */
public class HASingletonService implements Service<String> {
    private static final StackLogger logger = new StackLogger(HASingletonService.class.getName(), "HASingletonService");
    public static final ServiceName SINGLETON_SERVICE_NAME = ServiceName.JBOSS.append("stackv", "ha", "singleton", "service");

    /**
     * A flag whether the service is started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void start(StartContext arg0) throws StartException {
        String method = "start";
        logger.start(method);
        if (!started.compareAndSet(false, true)) {
            throw new StartException("The service is still started!");
        }
        final String node = System.getProperty("jboss.node.name");
        logger.message(method, "Start HASingleton DriverModelPuller service '" + this.getClass().getName() + "' on Node: "+node);
        while(true) {
            try {
                InitialContext ic = new InitialContext();
                ((net.maxgigapop.mrs.core.DriverModelPuller) ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/DriverModelPuller"))
                        .start();
                break;
            } catch (NamingException e) {
                logger.warning(method, "DriverModelPuller not ready for JNDI - waiting for 10 secs");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    ;
                }
            }
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
        }
        DataConcurrencyPoster dataConcurrencyPoster;
        try {
            Context ejbCxt = new InitialContext();
            dataConcurrencyPoster = (DataConcurrencyPoster) ejbCxt.lookup("java:module/DataConcurrencyPoster");
        } catch (NamingException e) {
            throw logger.error_throwing("hasSystemBootStrapped", "failed to lookup DataConcurrencyPoster --" + e);
        }
        dataConcurrencyPoster.setSystemModelCoordinator_bootStrapped(false);
        dataConcurrencyPoster.setSystemModelCoordinator_cachedOntModel(null);
        logger.end(method);
    }

    @Override
    public String getValue() throws IllegalStateException, IllegalArgumentException {
        return System.getProperty("jboss.node.name");
    }
}