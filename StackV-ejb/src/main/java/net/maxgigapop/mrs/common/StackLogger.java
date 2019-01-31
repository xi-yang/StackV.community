/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import javax.ejb.EJBException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.net.Severity;

/**
 *
 * @author xyang
 */
public class StackLogger {

    private Logger logger = null;
    private String moduleName = "";

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
        ThreadContext.put("event", String.format("%s.initiate", moduleName));
        logger.info("{}");
    }

    public void init(Object entity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.initiate", entity, moduleName));
        logger.info("{}");
    }

    public void start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.info("{}");
    }

    public void start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.info(String.format("{\"status\":\"%s\"}", status));
    }

    public void end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        String targetid = ThreadContext.pop();
        ThreadContext.put("targetid", targetid);
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.info("{}");
    }

    public void end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        String targetid = ThreadContext.pop();
        ThreadContext.put("targetid", targetid);
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.info(String.format("{\"status\":\"%s\"}", status));
    }

    public void status(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.status", moduleName, method));
        logger.info(String.format("{\"status\":\"%s\"}", status));
    }

    public void message(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.message", moduleName, method));
        logger.info(String.format("{\"message\":\"%s\"}", message));
    }

    public void warning(String method, String message) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", Severity.WARNING.name());
        ThreadContext.put("event", String.format("%s.%s.warning", moduleName, method));
        logger.warn(String.format("{\"message\":\"%s\"}", message));
    }

    public void error(String method, String message, Severity severity) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        ThreadContext.put("event", String.format("%s.%s.error", moduleName, method));
        logger.error(String.format("{\"message\":\"%s\", \"severity\":\"%s\"}", message, severity.name()));
    }

    public void error(String method, String message, Severity severity, Exception ex) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.put("severity", severity.name());
        ThreadContext.put("event", String.format("%s.%s.error", moduleName, method));
        logger.error(String.format("{\"message\":\"%s\", \"severity\":\"%s\"}", message, severity.name()), ex);
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
        ThreadContext.put("event", String.format("%s.%s.error", moduleName, method));
        String errMsg = String.format("{\"message\":\"%s\", \"severity\":\"%s\"}", message, severity.name());
        logger.error(errMsg);
        String refUUID = ThreadContext.get("refuuid");
        String targetID = ThreadContext.get("targetid");
        return new EJBException(String.format("%s-%s-%s", moduleName, method, (refUUID == null ? "" : refUUID), errMsg,
                (targetID == null ? "" : ":" + targetID)));
    }

    public EJBException error_throwing(String method, String message) {
        return error_throwing(method, message, Severity.ERROR);
    }

    // differing log4j.catching (no marker)
    public void catching(String method, Exception ex, Severity severity) {
        error(method, "catching " + ex, severity, ex);
    }

    public void catching(String method, Exception ex) {
        catching(method, ex, Severity.ERROR);
    }

    // differing log4j.throwing (no marker)
    public EJBException throwing(String method, String message, Exception ex, Severity severity) {
        error(method, message, severity);
        if (ex instanceof EJBException) {
            return (EJBException) (ex);
        } else {
            return new EJBException(message, ex);
        }
    }

    public EJBException throwing(String method, Exception ex, Severity severity) {
        return throwing(method, "catching " + ex, ex, Severity.ERROR);
    }

    public EJBException throwing(String method, String message, Exception ex) {
        error(method, message, Severity.ERROR, ex);
        if (ex instanceof EJBException) {
            return (EJBException) (ex);
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
        ThreadContext.put("event", String.format("%s.%s.debug", moduleName, method));
        if (status == null || status.isEmpty()) {
            logger.debug(String.format("{\"status\":\"%s\"}", message));
        } else {
            logger.debug(String.format("{\"message\":\"%s\", \"status\":\"%s\"}", message, status));
        }
    }

    public void debug(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.debug", moduleName, method));
        logger.debug(String.format("{\"status\":\"%s\"}", status));
    }

    public void debug_start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.debug("{}");
    }

    public void debug_start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.debug(String.format("{\"status\":\"%s\"}", status));
    }

    public void debug_end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.debug("{}");
    }

    public void debug_end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.debug(String.format("{\"status\":\"%s\"}", status));
    }

    public void trace(String method, String message, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.trace", moduleName, method));
        if (status == null || status.isEmpty()) {
            logger.trace(String.format("{\"status\":\"%s\"}", message));
        } else {
            logger.trace(String.format("{\"message\":\"%s\", \"status\":\"%s\"}", message, status));
        }
    }

    public void trace(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.trace", moduleName, method));
        logger.trace(String.format("{\"status\":\"%s\"}", status));
    }

    public void trace_start(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.trace("{}");
    }

    public void trace_start(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.start", moduleName, method));
        logger.trace(String.format("{\"status\":\"%s\"}", status));
    }

    public void trace_end(String method) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.trace("{}");
    }

    public void trace_end(String method, String status) {
        ThreadContext.put("module", moduleName);
        ThreadContext.put("method", method);
        ThreadContext.remove("severity"); // cleanup severity
        ThreadContext.put("event", String.format("%s.%s.end", moduleName, method));
        logger.trace(String.format("{\"status\":\"%s\"}", status));
    }

    public void cleanup() {
        ThreadContext.clearAll();
    }
}
