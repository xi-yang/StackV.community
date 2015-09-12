/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.compile.CompilerBase;
import net.maxgigapop.mrs.service.compile.CompilerFactory;
import net.maxgigapop.mrs.service.compute.MCE_InterfaceVlanStitching;

/**
 *
 * @author xyang
 */
public class SimpleWorker extends WorkerBase {
    private static final Logger log = Logger.getLogger(SimpleWorker.class.getName());

    @Override
    public void run() {
        retrieveSystemModel();
        try {
            CompilerBase simpleCompiler = CompilerFactory.createCompiler("net.maxgigapop.mrs.service.compile.SimpleCompiler");
            if (this.annoatedModelDelta == null) {
                throw new EJBException(SimpleWorker.class.getName() + " encounters null annoatedModelDelta");
            }
            simpleCompiler.setSpaDelta(this.annoatedModelDelta);
            simpleCompiler.compile(this);
            this.runWorkflow();
        } catch (Exception ex) {
            throw new EJBException(SimpleWorker.class.getName() + " caught exception", ex);
        }
    }
}
