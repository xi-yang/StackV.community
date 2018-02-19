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
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import net.maxgigapop.mrs.bean.persist.GlobalPropertyPersistenceManager;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class DataConcurrencyPoster {
    // per-node singleton to hold the cachedOntModel from SystemModelCoordinator for lock-free access
    OntModel SystemModelCoordinator_cachedOntModel = null;

    public OntModel getSystemModelCoordinator_cachedOntModel() {
        return SystemModelCoordinator_cachedOntModel;
    }

    public void setSystemModelCoordinator_cachedOntModel(OntModel SystemModelCoordinator_cachedOntModel) {
        this.SystemModelCoordinator_cachedOntModel = SystemModelCoordinator_cachedOntModel;
    }
    
    // persisted global boot_strapped flag
    public boolean isSystemModelCoordinator_bootStrapped() {
        String bootStrapped = GlobalPropertyPersistenceManager.getProperty("system.boot_strapped");
        if (bootStrapped != null && bootStrapped.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    public void setSystemModelCoordinator_bootStrapped(boolean bootStrapped) {
        GlobalPropertyPersistenceManager.setProperty("system.boot_strapped", bootStrapped ? "true" : "false");
    }
}