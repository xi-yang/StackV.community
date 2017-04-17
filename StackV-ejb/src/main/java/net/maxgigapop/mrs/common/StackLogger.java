/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.net.Severity;
/**
 *
 * @author xyang
 */
public class StackLogger {
    private Logger logger = null;
    private String moduleName = "";
    
    private StackLogger() {
        //never
    }
    
    public StackLogger(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public StackLogger(String loggerName, String moduleName) {
        this.logger = LogManager.getLogger(loggerName);
        this.moduleName = moduleName;
    }
    
    public Logger getLogger() {
        return this.logger;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void refuuid(String refuuid) {
        // refUUID will not change through the thread / session
        if (!ThreadContext.containsKey("refuuid")) {
            ThreadContext.put("refuuid", refuuid);
        }
        // targetID may change, refuuid also serve to clean it up
        ThreadContext.remove("targetid");
    }
    
    public void targetid(String targetid) {
        ThreadContext.push(targetid);
        ThreadContext.put("targetid", targetid);
    }
    
    public void init() {
        ThreadContext.put("module", moduleName);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.initiate\"}", moduleName));
    }

    public void init(Object entity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.%s.initiate\"}", entity, moduleName));
    }

    public void start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.%s.start\"}", moduleName, method));        
    }
    
    public void start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.%s.start\", \"status\":\"%s\"}", moduleName, method, status));
    }
    
    public void end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        String targetid = ThreadContext.pop();
        ThreadContext.put("targetid", targetid);
        logger.info(String.format("{\"event\":\"%s.%s.end\"}", moduleName, method));        
    }

    public void end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        String targetid = ThreadContext.pop();
        ThreadContext.put("targetid", targetid);
        logger.info(String.format("{\"event\":\"%s.%s.end\", \"status\":\"%s\"}", moduleName, method, status));
    }
    
    public void status(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.%s.status\", \"status\":\"%s\"}", moduleName, method, status));
    }
    
    public void message(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.info(String.format("{\"event\":\"%s.%s.message\", \"message\":\"%s\"}", moduleName, method, message));
    }
    
    public void warning(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", Severity.WARNING.name());
        logger.warn(String.format("{\"event\":\"%s.%s.warning\", \"message\":\"%s\"}", moduleName, method, message));        
    }

    public void error(String method, String message, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        logger.error(String.format("{\"event\":\"%s.%s.error\", \"message\":\"%s\", \"severity\":\"%s\"}", moduleName, method, message, severity.name()));        
    }
    
    public void error(String method, String message) {
        error(method, message, Severity.ERROR); 
    }

    public void critical(String method, String message) {
        error(method, message, Severity.CRITICAL);       
    }

    public void alert(String method, String message) {
        error(method, message, Severity.ALERT);       
    }  

    public void emerg(String method, String message) {
        error(method, message, Severity.EMERG);       
    }

    public EJBException error_throwing(String method, String message, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        String errMsg = String.format("{\"event\":\"%s.%s.error\", \"message\":\"%s\", \"severity\":\"%s\"}", moduleName, method, message, severity.name());
        logger.error(errMsg);
        String refUUID = ThreadContext.get("refuuid");
        String targetID = ThreadContext.get("targetid");
        return new EJBException(String.format("%s-%s-%s", moduleName, method, (refUUID == null ? "" : refUUID), errMsg, (targetID == null ? "" : ":"+targetID)));
    }

    public EJBException error_throwing(String method, String message) {
        return error_throwing(method, message, Severity.ERROR);
    }

    // differing log4j.catching (no marker)
    public void catching(String method, Exception ex, Severity severity) {
        error(method, "catching "+ex, severity);
    }
    
    public void catching(String method, Exception ex) {
        catching(method, ex, Severity.ERROR);        
    }
    
    // differing log4j.throwing (no marker)
    public EJBException throwing(String method, String message, Exception ex, Severity severity) {
        error(method, message, severity);
        if (ex instanceof  EJBException) {
            return (EJBException)(ex);
        } else {
            return new EJBException(message, ex);
        }
    }

    public EJBException throwing(String method, Exception ex, Severity severity) {
        return throwing(method, "catching "+ex, ex, Severity.ERROR);
    }
    
    public EJBException throwing(String method, String message, Exception ex) {
        error(method, message, Severity.ERROR);
        if (ex instanceof  EJBException) {
            return (EJBException)(ex);
        } else {
            return new EJBException(message, ex);
        }
    }
    
    public EJBException throwing(String method, Exception ex) {
        return throwing(method, ex, Severity.ERROR);
    }

    public void debug(String method, String message, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        if (status == null || status.isEmpty()) {
            logger.debug(String.format("{\"event\":\"%s.%s.debug\", \"status\":\"%s\"}", moduleName, method, message));               
        } else {
            logger.debug(String.format("{\"event\":\"%s.%s.debug\", \"message\":\"%s\", \"status\":\"%s\"}", moduleName, method, message, status));   
        }
    }
    
    public void debug(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.debug(String.format("{\"event\":\"%s.%s.debug\", \"status\":\"%s\"}", moduleName, method, status));      
    }

    public void debug_start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.debug(String.format("{\"event\":\"%s.%s.start\"}", moduleName, method));
    }

    public void debug_start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.debug(String.format("{\"event\":\"%s.%s.start\", \"status\":\"%s\"}", moduleName, method, status));
    }

    public void debug_end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.debug(String.format("{\"event\":\"%s.%s.end\"}", moduleName, method));
    }

    public void debug_end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.debug(String.format("{\"event\":\"%s.%s.end\", \"status\":\"%s\"}", moduleName, method, status));
    }
    
    public void trace(String method, String message, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        if (status == null || status.isEmpty()) {
            logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"status\":\"%s\"}", moduleName, method, message));               
        } else {
            logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"message\":\"%s\", \"status\":\"%s\"}", moduleName, method, message, status));   
        }
    }
    
    public void trace(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"status\":\"%s\"}", moduleName, method, status));      
    }

    public void trace_start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.trace(String.format("{\"event\":\"%s.%s.start\"}", moduleName, method));
    }

    public void trace_start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.trace(String.format("{\"event\":\"%s.%s.start\", \"status\":\"%s\"}", moduleName, method, status));
    }

    public void trace_end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.trace(String.format("{\"event\":\"%s.%s.end\"}", moduleName, method));
    }

    public void trace_end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        logger.trace(String.format("{\"event\":\"%s.%s.end\", \"status\":\"%s\"}", moduleName, method, status));
    }
}
