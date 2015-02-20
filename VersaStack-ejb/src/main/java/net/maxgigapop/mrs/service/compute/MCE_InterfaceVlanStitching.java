/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import static java.lang.Thread.sleep;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_InterfaceVlanStitching implements IModelComputationElement {
    private static final Logger log = Logger.getLogger(MCE_InterfaceVlanStitching.class.getName());
    
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        log.log(Level.INFO, "MCE_InterfaceVlanStitching::process {0}", annotatedDelta);
        DeltaBase outputDelta = new DeltaBase();
        //$$ TODO: do computation and create outputDelta
        return new AsyncResult(outputDelta);
    }
}
