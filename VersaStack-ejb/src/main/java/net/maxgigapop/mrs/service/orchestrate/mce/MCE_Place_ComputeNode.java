/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate.mce;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;

/**
 *
 * @author xyang
 * Process abstract nodes annotations to place compute nodes 
 */
@Stateless
public class MCE_Place_ComputeNode implements IModelComputationElement {
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        //$$ place holder
        DeltaBase resultDelta = annotatedDelta;
        return new AsyncResult<DeltaBase>(resultDelta);
    }
}
