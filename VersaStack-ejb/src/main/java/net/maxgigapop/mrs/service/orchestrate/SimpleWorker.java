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
    private final String spaAddModel = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
            + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
            + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
            + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
            + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
            + "@prefix spa:   <http://schemas.ogf.org/mrs/2015/02/spa#> .\n"
            + "\n"
            + "## abstract topology with policy annotations\n"
            + "\n"
            + "<urn:ogf:network:domain=vo1.versastack.org:node=left>\n"
            + "    a                         nml:Node ;\n"
            + "    nml:belongsTo             <urn:ogf:network:domain=vo1.versastack.org:network> ;\n"
            + "    nml:hasBidirectionalPort  <urn:ogf:network:domain=vo1.versastack.org:node=left:port=if0> ;\n"
            + "    nml:isAlias               <urn:ogf:network:domain=vo1.versastack.org:node=right:port=if0> ;\n"
            + "    nml:name                  \"VM-left\" ;\n"
            + "    spa:dependOn <x-policy-annotation:action:place-left>.\n"
            + "\n"
            + "<urn:ogf:network:domain=vo1.versastack.org:node=left:port=if0>\n"
            + "    a                         nml:BidirectionalPort ;\n"
            + "    nml:name                  \"VM-left-data-port\" ;\n"
            + "    spa:dependOn <x-policy-annotation:action:stitch-left-if0>.\n"
            + "\n"
            + "<urn:ogf:network:domain=vo1.versastack.org:node=right>\n"
            + "    a                         nml:Node ;\n"
            + "    nml:belongsTo             <urn:ogf:network:domain=vo1.versastack.org:network> ;\n"
            + "    nml:hasBidirectionalPort  <urn:ogf:network:domain=vo1.versastack.org:node=right:port=if0> ;\n"
            + "    nml:isAlias               <urn:ogf:network:domain=vo1.versastack.org:node=left:port=if0> ;\n"
            + "    nml:name                  \"VM-right\" ;\n"
            + "    spa:dependOn <x-policy-annotation:action:place-right>.\n"
            + "\n"
            + "<urn:ogf:network:domain=vo1.versastack.org:node=right:port=if0>\n"
            + "    a                         owl:NamedIndividual , nml:BidirectionalPort ;\n"
            + "    nml:name                  \"VM-right-data-port\" ;\n"
            + "    spa:dependOn <x-policy-annotation:action:stitch-right-if0>.\n"
            + "        \n"
            + "\n"
            + "<urn:ogf:network:domain=vo1.versastack.org:link=link1>\n"
            + "    a          owl:NamedIndividual, nml:Link ;\n"
            + "    nml:hasSource <urn:ogf:network:domain=vo1.versastack.org:node=left> ;\n"
            + "    nml:hasSink <urn:ogf:network:domain=vo1.versastack.org:node=right> ;\n"
            + "    nml:name                  \"A link between VM-left and VM-right\" ;\n"
            + "    spa:dependOn <x-policy-annotation:action:connect-link1> .\n"
            + "\n"
            + "## policy actions\n"
            + "\n"
            + "<x-policy-annotation:action:place-left>\n"
            + "    a            spa:Placement ;\n"
            + "    spa:matchAttribute <x-policy-annotation:data:left-filter-criteria> ;\n"
            + "    spa:importFrom <x-policy-annotation:data:left-filter-criteria> ;\n"
            + "    spa:exportTo <x-policy-annotation:data:left-location> .\n"
            + "\n"
            + "<x-policy-annotation:action:stitch-left-if0>\n"
            + "    a            spa:Stitching ;\n"
            + "    spa:stitchType <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n"
            + "    spa:dependOn <x-policy-annotation:action:connect-link1>, <x-policy-annotation:action:place-left> ;\n"
            + "    spa:importFrom <x-policy-annotation:data:left-location>, <x-policy-annotation:data:link1-vlan> .\n"
            + "      \n"
            + "<x-policy-annotation:action:place-right>\n"
            + "    a            spa:Placement ;\n"
            + "    spa:matchAttribute <x-policy-annotation:data:right-filter-criteria> ;\n"
            + "    spa:importFrom <x-policy-annotation:data:right-filter-criteria> ;\n"
            + "    spa:exportTo <x-policy-annotation:data:right-location> .\n"
            + "\n"
            + "<x-policy-annotation:action:stitch-right-if0>\n"
            + "    a            spa:Stitching ;\n"
            + "    spa:stitchType <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n"
            + "    spa:dependOn <x-policy-annotation:action:connect-link1>, <x-policy-annotation:action:place-right> ;\n"
            + "    spa:importFrom <x-policy-annotation:data:right-stitching-type>, <x-policy-annotation:data:right-location>, <x-policy-annotation:data:link1-vlan> .\n"
            + "\n"
            + "<x-policy-annotation:action:connect-link1>\n"
            + "    a            spa:Connection ;\n"
            + "    spa:connectType <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n"
            + "    spa:dependOn <x-policy-annotation:action:place-left>, <x-policy-annotation:action:place-right> ;\n"
            + "    spa:importFrom <x-policy-annotation:data:left-location>, <x-policy-annotation:data:right-location> ;\n"
            + "    spa:exportTo <x-policy-annotation:data:link1-vlan> .\n"
            + "\n"
            + "## Policy data\n"
            + "\n"
            + "<x-policy-annotation:data:left-filter-criteria>\n"
            + "    a            spa:PolicyData;\n"
            + "    spa:type     nml:Topology;\n"
            + "    spa:value    \"urn:ogf:network:rains.maxgigapop.net:2013:topology:left-domain\".\n"
            + "\n"
            //+ "<x-policy-annotation:data:left-filter-criteria2>\n"
            //+ "    a            spa:PolicyData;\n"
            //+ "    spa:type     nml:Node;\n"
            //+ "    spa:value    \"urn-for-aws-east-region-node1\".\n"
            //+ "\n"
            + "<x-policy-annotation:data:right-filter-criteria>\n"
            + "    a            spa:PolicyData;\n"
            + "    spa:type     nml:Topology;\n"
            + "    spa:value    \"urn:ogf:network:rains.maxgigapop.net:2013:topology:right-domain\".\n"
            + "    \n"
            + "<x-policy-annotation:data:left-location>\n"
            + "    a            spa:PolicyData.\n"
            //+ "    spa:type     nml:Node;\n"
            //+ "    spa:value    xsd:string.\n"
            + "\n"
            + "<x-policy-annotation:data:right-location>\n"
            + "    a            spa:PolicyData.\n"
            //+ "    spa:type     nml:Node;\n"
            //+ "    spa:value    xsd:string.\n"
            + "    \n"
            + "<x-policy-annotation:data:link1-vlan>\n"
            + "    a            spa:PolicyData.\n"
            //+ "    spa:type     nml:labeltype;\n"
            //+ "    spa:value    <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>.\n"
            + "\n"
            + "<x-policy-annotation:data:right-stitching-type>\n"
            + "    a            spa:PolicyData;\n"
            + "    spa:type     \"InterfaceVlanStitching:StitchingType\";\n"
            + "    spa:value    \"AWS_VPC\".\n"
            + "\n";
    
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
