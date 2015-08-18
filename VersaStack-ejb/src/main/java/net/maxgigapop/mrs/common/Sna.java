/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;
import com.hp.hpl.jena.rdf.model.*;

/**
 *
 * @author onos-versastack
 */
public class Sna {
    
    private static Model m_model = ModelFactory.createDefaultModel();
    
    public static final String NS = "http://schemas.ogf.org/sna/2015/08/network#";
    
    public static String getURI() { return NS;}
    
    public static final Resource NAMESPACE = m_model.createResource(NS);
    
    public static final Property severity = m_model.createProperty("http://schemas.ogf.org/sna/2015/08/network#severity");
    
    public static final Property occurenceProbability = m_model.createProperty("http://schemas.ogf.org/sna/2015/08/network#occurenceProbability");
    
    public static final Resource SRRG = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#SRRG");
    
    public static final Resource protectionSwitchingService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionSwitchingService");
    
    public static final Resource protectionRoutingService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionRoutingService");
    
    public static final Resource protectionOpenflowService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionOpenflowService");
    
    
}
