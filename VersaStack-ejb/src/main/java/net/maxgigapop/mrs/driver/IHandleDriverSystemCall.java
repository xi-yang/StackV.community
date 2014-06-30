/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

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

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    public Future<String> commitDelta(Long driverInstanceId, Long targetVIId);

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId);    
}
