/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.session;

import java.util.concurrent.Future;
import javax.ejb.Local;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;

/**
 *
 * @author xyang
 */
@Local
public interface HandleSystemLocal {

    public SystemInstance createInstance();
    
    public void terminateInstance(SystemInstance systemInstance);

    public void propagateDelta(SystemInstance systemInstance, SystemDelta aDelta);

    public Future<String> provisionDelta(SystemInstance systemInstance, SystemDelta aDelta);
}
