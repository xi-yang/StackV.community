/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.session;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionItem;

/**
 *
 * @author xyang
 */
@Local
public interface IHandleDriverSystemCall {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta);

    @Asynchronous
    public Future<String> commitDelta(DriverInstance driverInstance, VersionItem targetVI);

    @Asynchronous
    public Future<String> pullModel(DriverInstance driverInstance);
}
