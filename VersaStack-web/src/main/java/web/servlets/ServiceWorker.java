package web.servlets;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ServiceWorker implements ServletContextListener {

    private static final BlockingQueue queue = new LinkedBlockingQueue();
    private volatile Thread thread;

    public static void add(AsyncContext c) {
        queue.add(c);
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                        AsyncContext context;
                        while ((context = (AsyncContext) queue.poll()) != null) {
                            try {
                                ServletRequest request = context.getRequest();
                                ServletResponse response = context.getResponse();
                                
                                response.setContentType("text/plain");
                                PrintWriter out = response.getWriter();
                                out.printf("Thread %s completed the task", Thread.currentThread().getName());
                                out.flush();
                            } catch (Exception e) {
                                throw new RuntimeException(e.getMessage(), e);
                            } finally {
                                context.complete();
                            }
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        thread.start();
    }
    
    @Override
        public void contextDestroyed(ServletContextEvent sce) {
        thread.interrupt();
    }

}
