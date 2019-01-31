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

package net.maxgigapop.mrs.system;


import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.persist.GlobalPropertyPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONObject;


/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleConfigCall {
    
    private static final StackLogger logger = new StackLogger(HandleConfigCall.class.getName(), "HandleConfigCall");

    public String getConfigProperty(String property) {
        return GlobalPropertyPersistenceManager.getProperty(property);
    }

    public String getAllConfig() {
        return GlobalPropertyPersistenceManager.getAll();
    }

    public void setConfigProperty(String property, String value) {
        GlobalPropertyPersistenceManager.setProperty(property, value);
    }
    
    public void deleteConfigProperty(String property) {
        GlobalPropertyPersistenceManager.deleteProperty(property);
    }
}
