/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import javax.ejb.EJBException;
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
    
    public StackLogger(String className, String moduleName) {
        this.logger = LogManager.getLogger(className);
        this.moduleName = moduleName;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void refuuid(String refuuid) {
        ThreadContext.put("refuuid", refuuid);
    }
    
    public void targetid(String targetid) {
        ThreadContext.put("objectid", targetid);
    }
    
    public void init() {
        ThreadContext.put("module", moduleName);
        logger.info(String.format("{\"event\":\"%s.initiate\"}", moduleName));
    }

    public void init(String entity) {
        ThreadContext.put("module", moduleName);
        logger.info(String.format("{\"event\":\"%s.%s.initiate\"}", entity, moduleName));
    }

    public void start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.start\"}", moduleName, method));        
    }
    
    public void start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.start\", \"status\"=\"%s\"}", moduleName, method, status));
    }
    
    public void end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.end\"}", moduleName, method));        
    }

    public void end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.end\", \"status\"=\"%s\"}", moduleName, method, status));
    }
    
    public void status(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.status\", \"status\"=\"%s\"}", moduleName, method, status));
    }
    
    public void message(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.info(String.format("{\"event\":\"%s.%s.message\", \"message\"=\"%s\"}", moduleName, method, message));
    }
    
    public void trace(String method, String message, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        if (status == null || status.isEmpty()) {
            logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"status\"=\"%s\"}", moduleName, method, message));               
        } else {
            logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"message\"=\"%s\", \"status\"=\"%s\"}", moduleName, method, message, status));   
        }
    }
    
    public void trace(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        logger.trace(String.format("{\"event\":\"%s.%s.trace\", \"status\"=\"%s\"}", moduleName, method, status));      
    }
 
    public void warning(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", Severity.WARNING.name());
        logger.warn(String.format("{\"event\":\"%s.%s.warning\", \"message\"=\"%s\"}", moduleName, method, message));        
    }

    public void error(String method, String message, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        logger.error(String.format("{\"event\":\"%s.%s.error\", \"message\"=\"%s\", \"severity\"=\"%s\"}", moduleName, method, message, severity.name()));        
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
        String errMsg = String.format("{\"event\":\"%s.%s.error\", \"message\"=\"%s\", \"severity\"=\"%s\"}", moduleName, method, message, severity.name());
        logger.error(errMsg);
        return new EJBException(errMsg);
    }

    public EJBException error_throwing(String method, String message) {
        return error_throwing(method, message, Severity.ERROR);
    }

    public void catching(String method, Exception ex, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        logger.catching(ex);        
    }
    
    public void catching(String method, Exception ex) {
        catching(method, ex, Severity.ERROR);        
    }
    
    public EJBException throwing(String method, Exception ex, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        logger.catching(ex);
        if (ex instanceof  EJBException) {
            return (EJBException)(ex);
        } else {
            return new EJBException(ex);
        }
    }
    
    public EJBException throwing(String method, Exception ex) {
        return throwing(method, ex, Severity.ERROR);
    }
}
