/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author xyang
 */
public class DriverUtil {
    private static final Logger logger = Logger.getLogger(DriverUtil.class.getName());
    
    public static String[] executeHttpMethod(String username, String password, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        String userPassword=username+":"+password;
        byte[] encoded=Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded=new String(encoded);
        conn.setRequestProperty("Authorization", "Basic "+stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        logger.log(Level.FINE, "Sending {0} request to URL : {1}", new Object[]{method, conn.getURL()});
        int responseCode = conn.getResponseCode();
        logger.log(Level.FINE, "Response Code : {0}", responseCode);

        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        String strArray[] = {responseStr.toString(), Integer.toString(responseCode)};
        return strArray;
    }
    
    public static String[]  executeHttpMethod(String username, String password, URL url, String method, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        return executeHttpMethod(username, password, conn, method, body);
    }
    
    public static String[]  executeHttpMethod(String username, String password, String url, String method, String body) throws IOException {
        URL urlObj = new URL(url);
        return executeHttpMethod(username, password, urlObj, method, body);
    }
    
    public static String addressUriEscape(String addr) {
        return addr.replace(":", "_").replace("/", "_");
    } 
}


