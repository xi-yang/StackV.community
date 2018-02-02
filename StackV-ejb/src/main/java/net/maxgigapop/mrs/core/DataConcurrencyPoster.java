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

package net.maxgigapop.mrs.core;

import com.hp.hpl.jena.ontology.OntModel;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.msc.service.ServiceController;
import org.jboss.as.server.CurrentServiceContainer; 
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartException;

/**
 *
 * @author xyang
 */

public class DataConcurrencyPoster {
    boolean SystemModelCoordinator_bootStrapped = false;
    OntModel SystemModelCoordinator_cachedOntModel = null;

    public boolean isSystemModelCoordinator_bootStrapped() {
        return SystemModelCoordinator_bootStrapped;
    }

    public void setSystemModelCoordinator_bootStrapped(boolean SystemModelCoordinator_bootStrapped) {
        this.SystemModelCoordinator_bootStrapped = SystemModelCoordinator_bootStrapped;
    }

    public OntModel getSystemModelCoordinator_cachedOntModel() {
        return SystemModelCoordinator_cachedOntModel;
    }

    public void setSystemModelCoordinator_cachedOntModel(OntModel SystemModelCoordinator_cachedOntModel) {
        this.SystemModelCoordinator_cachedOntModel = SystemModelCoordinator_cachedOntModel;
    }

    static public DataConcurrencyPoster getSingleton() {
        try {
            InitialContext ic = new InitialContext();
            Object haservice =  ic.lookup("java:global/StackV-ear-1.0-SNAPSHOT/StackV-ejb-1.0-SNAPSHOT/HASingletonService");
            return ((net.maxgigapop.mrs.core.HASingletonService) haservice).getValue();
        } catch (NamingException e) {
            return null;
        }
    }
}
