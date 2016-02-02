package web.async;

import java.util.HashMap;
import javax.servlet.AsyncContext;
import web.beans.serviceBeans;

public class NetCreateWorker implements Runnable {

    private AsyncContext asyncContext;
    private serviceBeans servBean = new serviceBeans();
    private HashMap<String, String> paraMap;
    private final String host = "http://localhost:8080/VersaStack-web/restapi";

    public NetCreateWorker() {
    }

    public NetCreateWorker(AsyncContext asyncCtx, HashMap<String, String> paraMap) {
        this.asyncContext = asyncCtx;
        this.paraMap = paraMap;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println("Network Creation Worker Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());

        servBean.createNetwork(paraMap);

        long endTime = System.currentTimeMillis();
        System.out.println("Network Creation Worker End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");

        asyncContext.complete();
    }
}