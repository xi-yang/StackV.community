/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import javax.ejb.EJBException;

/**
 *
 * @author xyang
 */
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
