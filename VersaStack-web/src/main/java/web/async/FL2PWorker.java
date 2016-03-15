/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web.async;

import java.util.HashMap;
import javax.servlet.AsyncContext;
import web.beans.serviceBeans;

/**
 *
 * @author ranjitha
 */


public class FL2PWorker implements Runnable {

    private AsyncContext asyncContext;
    private serviceBeans servBean = new serviceBeans();
    private HashMap<String, String> paraMap;
    private final String host = "http://localhost:8080/VersaStack-web/restapi";

    public FL2PWorker() {
    }

    public FL2PWorker(AsyncContext asyncCtx, HashMap<String, String> paraMap) {
        this.asyncContext = asyncCtx;
        this.paraMap = paraMap;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println("[service] Worker Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());
        
        servBean.createflow(paraMap);

        long endTime = System.currentTimeMillis();
        System.out.println("[service] Worker End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");

        asyncContext.complete();
    }
}
