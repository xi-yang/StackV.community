/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class WorkflowTestStarter {
    @PostConstruct
    void runTests() {
        try {
            sleep(90000L);
        } catch (InterruptedException ex) {
            Logger.getLogger(WorkflowTestStarter.class.getName()).log(Level.SEVERE, null, ex);
        }
        WorkerBase testWorker = WorkerFactory.createWorker("net.maxgigapop.mrs.service.orchestrate.SimpleWorker");
        testWorker.run();
    }
}
