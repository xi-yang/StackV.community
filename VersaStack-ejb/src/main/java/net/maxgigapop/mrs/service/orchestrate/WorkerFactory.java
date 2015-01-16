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
public class WorkerFactory {
    static public WorkerBase createWorker(String workerClassStr) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        WorkerBase worker = null;
        try {
            Class<?> aClass = cl.loadClass(workerClassStr);
            worker = (WorkerBase) aClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new EJBException("WorkerFactory failed to create "+workerClassStr, ex);
        }
        return worker;
    }
}
