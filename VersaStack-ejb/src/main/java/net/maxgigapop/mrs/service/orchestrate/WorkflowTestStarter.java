/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

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
        WorkerBase testWorker = WorkerFactory.createWorker("net.maxgigapop.mrs.service.orchestrate.SimpleWorker");
        testWorker.run();
    }
}
