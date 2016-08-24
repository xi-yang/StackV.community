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

package net.maxgigapop.mrs.service.orchestrate;

import javax.ejb.EJBException;

public class TestWorker extends WorkerBase {

    @Override
    public void run() {
        try {
            ActionBase root = new ActionBase("root", "java:module/TestMCE");
            this.addRooAction(root);
            ActionBase child1 = new ActionBase("child1", "java:module/TestMCE");
            ActionBase child2 = new ActionBase("child2", "java:module/TestMCE");
            this.addDependency(root, child1);
            this.addDependency(root, child2);
            ActionBase grandchild1 = new ActionBase("grandchild1", "java:module/TestMCE");
            ActionBase grandchild2 = new ActionBase("grandchild2", "java:module/TestMCE");
            this.addDependency(child1, grandchild1);
            this.addDependency(child1, grandchild2);
            this.runWorkflow();
        } catch (Exception ex) {
            throw new EJBException(TestWorker.class.getName() + "caught exception", ex);
        }
    }
}
