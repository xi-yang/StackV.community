package web.async;

import java.util.HashMap;
import javax.servlet.AsyncContext;
import web.beans.serviceBeans;

public class DNCWorker implements Runnable {

    private AsyncContext asyncContext;
    private serviceBeans servBean = new serviceBeans();
    private HashMap<String, String> paraMap;
    private final String host = "http://localhost:8080/VersaStack-web/restapi";

    public DNCWorker() {
    }

    public DNCWorker(AsyncContext asyncCtx) {
        this.asyncContext = asyncCtx;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println("[service] Worker Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());
        
        servBean.createConnection(paraMap);

        long endTime = System.currentTimeMillis();
        System.out.println("[service] Worker End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");

        asyncContext.complete();
    }
}
