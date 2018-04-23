/*
 * Copyright (c) 2013-2018 University of Maryland
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
package net.maxgigapop.mrs.rest.api;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import net.maxgigapop.mrs.common.StackLogger;

public class JNDIFactory {

    private final StackLogger logger = new StackLogger(WebResource.class.getName(), "JNDIFactory");

    public Connection getConnection(String tag) {
        String method = "getConnection";
        String jndi = System.getProperty(tag + "_jndi");
        if (jndi == null || jndi.isEmpty()) {
            if (tag.equals("rainsdb")) {
                jndi = "java:jboss/datasources/MysqlDS";
            } else if (tag.equals("frontend")) {
                jndi = "java:jboss/datasources/FrontendDS";
            }
        }
        Connection result = null;
        try {
            Context initialContext = new InitialContext();
            DataSource datasource = (DataSource) initialContext.lookup(jndi);
            if (datasource != null) {
                result = datasource.getConnection();
            } else {
                logger.error(method, "Datasource not found.");
            }
        } catch (SQLException ex) {
            int attempt = 0;
            while (++attempt <= 10) {
                try {
                    Thread.sleep(1000);
                    Context initialContext = new InitialContext();
                    DataSource datasource = (DataSource) initialContext.lookup(jndi);
                    if (datasource != null) {
                        result = datasource.getConnection();
                    } else {
                        logger.error(method, "Datasource not found.");
                    }

                } catch (SQLException ex2) {
                    if (attempt == 10) {
                        throw logger.throwing(method, ex2);
                    } else {
                        logger.error(method, "Datasource connection failed. Attempt #" + attempt + ".");
                    }
                } catch (InterruptedException | NamingException ex3) {
                    throw logger.throwing(method, ex);
                }
            }
        } catch (NamingException ex) {
            throw logger.throwing(method, ex);
        }
        return result;
    }
}
