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
public class MCE_MPVlanConnection implements IModelComputationElement {
    private static final Logger log = Logger.getLogger(MCE_MPVlanConnection.class.getName());
    /*
    ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
    */
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {        
        log.log(Level.INFO, "MCE_MPVlanConnection::process {0}", annotatedDelta);
        try {
            log.log(Level.INFO, "\n>>>MCE_MPVlanConnection--DeltaAddModel=\n" + ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeltaBase outputDelta = annotatedDelta.clone();
        
        //$$ TODO: importPolicyData : List<Resource> of terminal Node/Topology (Map policyAction:->List<Resource>)

        //$$ TODO: computeConnection : return a List<Model> of MPVlan connections
        //$$        Filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        //$$        Transform into bidirectional connected graph by explicit interence
        //$$        KSP-MP path computation on the connected graph model
        //$$        Verify TE constraints (switching label and ?adaptation?)

        //$$ pick a connection and add to outputDelta.additionOntModel
        
        //$$ TODO: exportPolicyData
        //$$ TODO: removeResolvedAnnotation
        
        return new AsyncResult(outputDelta);
    }
}
