/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.openstack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author james
 */
@Singleton
@LocalBean
@Startup
public class OpenStackTest {
    
    private @PersistenceContext(unitName = "RAINSAgentPU")
    EntityManager entityManager;

    //@PostConstruct
    public void init() {
        
        if (PersistenceManager.getEntityManager() == null) {
            PersistenceManager.initialize(entityManager);
        }
        
        try {
            Context ejbCxt = new InitialContext();
            HandleSystemCall systemCallHandler = (HandleSystemCall) ejbCxt.lookup("java:module/HandleSystemCall");
            
            Map<String, String> driverProperties = new HashMap<>();
            driverProperties.put("topologyUri", "dragon.charon.maxgigapop.net");
            
            driverProperties.put("adminTenant", "admin");
            driverProperties.put("adminUser", "admin");
            driverProperties.put("adminPasswd", "admin");
            
            driverProperties.put("cloudTenant", "demo");
            driverProperties.put("cloudUser", "demo");
            driverProperties.put("cloudPasswd", "demo");
            
            driverProperties.put("authenticationPort", "5000");
            driverProperties.put("keystonePort", "35357");
            driverProperties.put("novaPort", "8774");
            driverProperties.put("neutronPort", "9696");
            
            driverProperties.put("driverEjbPath", "java:module/OpenStackDriver");
            
            systemCallHandler.plugDriverInstance(driverProperties);        
            
            Logger.getLogger(OpenStackTest.class.getName()).log(Level.INFO, "OpenStack driver instance started");
            
        } catch (Exception ex) {
            Logger.getLogger(OpenStackTest.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
}
