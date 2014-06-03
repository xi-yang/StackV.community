/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import java.util.concurrent.Future;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.system.IHandleDriverSystemCall;

/**
 *
 * @author xyang
 */

//properties: stubModelTtl

@Stateless
public class StubSystemDriver implements IHandleDriverSystemCall {

    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        // apply delta to stubModel and update stubModel into properties
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<String> commitDelta(DriverInstance driverInstance, VersionItem targetVI) {
        // simply return success
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<String> pullModel(DriverInstance driverInstance) {
        // create VI using stubModel in properties
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
