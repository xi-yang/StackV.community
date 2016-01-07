package web.async;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.AsyncContext;

public class DriverWorker implements Runnable {

    private AsyncContext asyncContext;
    private HashMap<String, String> paramMap;

    public DriverWorker() {
    }

    public DriverWorker(AsyncContext asyncCtx, HashMap<String, String> paramMap) {
        this.asyncContext = asyncCtx;
        this.paramMap = paramMap;
    }

    @Override
    public void run() {
        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());
        try {
            PrintWriter out = asyncContext.getResponse().getWriter();
            out.write("!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Complete the processing.
        asyncContext.complete();
    }
}
