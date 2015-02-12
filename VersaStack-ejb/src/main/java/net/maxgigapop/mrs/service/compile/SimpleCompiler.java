/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compile;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;

/**
 *
 * @author xyang
 */
public class SimpleCompiler extends CompilerBase {
    List<DeltaBase> terminalActionDeltas = new ArrayList<>();
    
    public SimpleCompiler(DeltaBase spaDelta) {
        super(spaDelta);
    }
    
    @Override
    public void compile() {        
        //$$ assemble workflow for reduction
        List<OntModel> reduceParts = this.decomposeByPolicyActions(this.spaDelta.getModelReduction().getOntModel());
        
        //$$ assemble workflow for addition
        List<OntModel> addParts = this.decomposeByPolicyActions(this.spaDelta.getModelAddition().getOntModel());
        // $$ start with terminal actions / leaves
        // $$ traverse upwards to add parent action
        // $$ identify existing parent action and merge lower (branch or leaf) action
    }
}
