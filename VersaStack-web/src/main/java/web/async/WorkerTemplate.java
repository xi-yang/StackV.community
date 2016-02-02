package web.async;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import javax.servlet.AsyncContext;
import web.beans.serviceBeans;

public class WorkerTemplate implements Runnable {

    private AsyncContext asyncContext;
    private serviceBeans servBean = new serviceBeans();
    private HashMap<String, String> paraMap;
    private final String host = "http://localhost:8080/VersaStack-web/restapi";

    public WorkerTemplate() {
    }

    public WorkerTemplate(AsyncContext asyncCtx) {
        this.asyncContext = asyncCtx;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println("[service] Worker Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());
        try {
            PrintWriter out = asyncContext.getResponse().getWriter();

        } catch (IOException e) {
            System.out.println("[service] Worker IO Exception!");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[service] Worker End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");

        asyncContext.complete();
    }
}
