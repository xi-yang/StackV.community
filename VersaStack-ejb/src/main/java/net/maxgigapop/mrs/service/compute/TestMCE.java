/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import static java.lang.Thread.sleep;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;


/**
 *
 * @author xyang
 */
@Stateless
public class TestMCE implements IModelComputationElement {
    private static Logger log = Logger.getLogger(TestMCE.class.getName());
    private static int sn = 1;
    
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        log.info("TceMCE::process #" + sn++);
        try {
            sleep(sn*1000); // sleep sn seconds
        } catch (InterruptedException ex) {
            Logger.getLogger(TestMCE.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeltaBase delta = new DeltaBase();
        return new AsyncResult(delta);
    }
}
