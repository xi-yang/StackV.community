/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.system.HandleSystemPushCall;
import net.maxgigapop.mrs.system.IHandleDriverSystemCall;

/**
 *
 * @author xyang
 */

//properties: driverSystemEjbPath which translates to calls in driverSystemEjbPath.HandleSystemPushCall and driverSystemEjbPath.HandleSystemCall

@Stateless
public class StackSystemDriver implements IHandleDriverSystemCall{
    private static Map<VersionItem, HandleSystemPushCall> driverSystemSessionMap = new HashMap<VersionItem, HandleSystemPushCall>();
            
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        // get subSystemPushHandler session
        //driverSystemSessionMap.put(aDelta.getTargetVersionItem(), subSystemPushHandler);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<String> commitDelta(DriverInstance driverInstance, VersionItem targetVI) {
        // subSystemPushHandler = driverSystemSessionMap.get(targetVI);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<String> pullModel(DriverInstance driverInstance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
