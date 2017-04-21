/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.system;

import static java.lang.Thread.sleep;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static junit.framework.Assert.assertFalse;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.driver.StubSystemDriver;
import net.maxgigapop.mrs.core.DriverModelPuller;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author xyang
 */
@RunWith(Arquillian.class)
public class HandleSystemCallTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(HandleSystemCall.class)
                .addPackages(true, "net.maxgigapop.mrs.bean")
                .addPackages(true, "net.maxgigapop.mrs.bean.persist")
                .addPackages(true, "net.maxgigapop.mrs.common")
                .addPackages(true, "net.maxgigapop.mrs.system")
                .addPackages(true, "net.maxgigapop.mrs.driver")
                .addPackages(true, "com.hp.hpl.jena")
                .addPackages(true, "org.apache.xerces")
                .addPackages(true, "org.json.simple")
                .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml");
    }

    @EJB
    HandleSystemCall systemCallHandler;

    @PersistenceContext(unitName = "RAINSAgentPU")
    EntityManager entityManager;

    /**
     * Test of createHeadVersionGroup method, of class HandleSystemCall.
     */
    @Test
    public void testCreateHeadVersionGroup() throws Exception {
        System.out.println("###createHeadVersionGroup###");
        if (PersistenceManager.getEntityManager() == null) {
            PersistenceManager.initialize(entityManager);
        }
        sleep(90000);
        VersionGroup expResult = null;
        VersionGroup result = systemCallHandler.createHeadVersionGroup_API(UUID.randomUUID().toString());
        System.out.println("createHeadVersionGroup result=" + result);
        assertFalse("createHeadVersionGroup results in null VersionGroup", expResult == null);
    }
}
