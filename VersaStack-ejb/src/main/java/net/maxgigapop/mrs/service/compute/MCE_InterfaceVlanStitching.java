/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;

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
        try {
            log.log(Level.INFO, "\n>>>MCE_InterfaceVlanStitching--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeltaBase outputDelta = annotatedDelta;
        //$$ TODO: do computation and create outputDelta
        return new AsyncResult(outputDelta);
    }
}
