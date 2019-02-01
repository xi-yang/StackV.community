package net.maxgigapop.mrs.common;

import java.net.URL;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GlobalHandler {
    private static final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource",
            "GlobalHandler");
    private static final OkHttpClient client = new OkHttpClient();
    private static JSONParser parser = new JSONParser();

    public static String subscribe(Object visitor, String key) {
        String method = "subscribe<-" + visitor.getClass().getName();
        try {
            URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/config/" + key);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            logger.trace_end(method);
            return response.body().string();
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
    }

    public static JSONObject subscribe(Object visitor) {
        String method = "subscribe<-" + visitor.getClass().getName();
        try {
            URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/config/");
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();

            Object obj = parser.parse(responseStr);
            JSONObject props = (JSONObject) obj;

            logger.trace_end(method);
            return props;
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
    }

    public static String get(String key) {
        String method = "get";
        try {
            URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/config/" + key);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            logger.trace_end(method);
            return response.body().string();
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
    }
}