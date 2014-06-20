/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.web.test;

import static java.lang.Thread.sleep;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class IntegrationTest1 {

    @EJB
    HandleSystemCall systemCallHandler;
    
    @PostConstruct
    public void init() {
        try {
            Context ejbCxt = new InitialContext();
            //HandleSystemCall systemCallHandler = (HandleSystemCall) ejbCxt.lookup("java:module/HandleSystemCall");
            sleep(90000L);
            Long refId = 1L;
            VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        } catch (Exception ex) {
            Logger.getLogger(IntegrationTest1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
