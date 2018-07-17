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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author xyang
 */
public class DriverUtil {
    static private final Logger logger = Logger.getLogger(DriverUtil.class.getName());

    static public class SSLSkipSNIHostnameVerifier implements HostnameVerifier {
        public SSLSkipSNIHostnameVerifier() {
        }
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // Return true so that we implicitly trust hostname mismatch
            return true;
        }
    }

    public static String[] executeHttpMethod(String username, String password, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        if (username != null && !username.isEmpty()) {
            String userPassword=username+":"+password;
            byte[] encoded=Base64.encodeBase64(userPassword.getBytes());
            String stringEncoded=new String(encoded);
            conn.setRequestProperty("Authorization", "Basic "+stringEncoded);
        }
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

    public static String[] executeHttpMethod(HttpURLConnection conn, String method, String body) throws IOException {
        return executeHttpMethod(null, null, conn, method, body);
    }
    
    /*
     * This wrapper class overwrites the default behavior of a X509KeyManager and
     * always render a specific certificate whose alias matches that provided in the constructor
     */
    private static class AliasForcingKeyManager implements X509KeyManager {

        X509KeyManager baseKM = null;
        String alias = null;

        public AliasForcingKeyManager(X509KeyManager keyManager, String alias) {
            baseKM = keyManager;
            this.alias = alias;
        }

        //Always render the specific alias provided in the constructor
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return alias;
        }

        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return baseKM.chooseServerAlias(keyType, issuers, socket);
        }

        public X509Certificate[] getCertificateChain(String alias) {
            return baseKM.getCertificateChain(alias);
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return baseKM.getClientAliases(keyType, issuers);
        }

        public PrivateKey getPrivateKey(String alias) {
            return baseKM.getPrivateKey(alias);
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return baseKM.getServerAliases(keyType, issuers);
        }
    }
    
    private static SSLSocketFactory getSSLFactory(File pKeyFile, String pKeyPassword, String certAlias) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream keyInput = new FileInputStream(pKeyFile);
        keyStore.load(keyInput, pKeyPassword.toCharArray());
        keyInput.close();
        keyManagerFactory.init(keyStore, pKeyPassword.toCharArray());
        //Replace the original KeyManagers with the AliasForcingKeyManager
        KeyManager[] kms = keyManagerFactory.getKeyManagers();
        for (int i = 0; i < kms.length; i++) {
            if (kms[i] instanceof X509KeyManager) {
                kms[i] = new AliasForcingKeyManager((X509KeyManager) kms[i], certAlias);
            }
        }
        //Trust all!
        TrustManager tms[] = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        SSLContext context = SSLContext.getInstance("TLSv1.1");
        context.init(kms, tms, null);
        return context.getSocketFactory();
    }

    public static String[] executeHttpMethodWithClientCert(HttpURLConnection conn, String method, String body, String clientStoreAlias, String clientStorePass) throws IOException {
        try {
            String configDir = System.getProperty("jboss.server.config.dir");
            File keystoreFile = new File(configDir + "/client.jks");
            if (!keystoreFile.exists()) {
                keystoreFile = new File(configDir + "/wildfly.jks");
            }
            if (!keystoreFile.exists()) {
                File dir = new File(configDir);
                FileFilter fileFilter = new WildcardFileFilter("*.jks");
                File[] files = dir.listFiles(fileFilter);
                if (files.length == 0) {
                    throw new IOException(String.format("No keystore file (wildfly.jks or *.jks) available under %s for SSL client cert.", configDir));
                }
                keystoreFile = files[0];
            }
            if (clientStorePass == null || clientStorePass.isEmpty()) {
                clientStorePass = "password";
            }
            SSLSocketFactory sslSocketFactory = getSSLFactory(keystoreFile, clientStorePass, clientStoreAlias);
            ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        return executeHttpMethod(null, null, conn, method, body);
    }
    
    public static String[] executeHttpMethod(String username, String password, URL url, String method, String body) throws IOException {
        HttpURLConnection conn;
        if (url.toString().startsWith("https:")) {
            conn = (HttpsURLConnection) url.openConnection();
            ((HttpsURLConnection) conn).setHostnameVerifier(new SSLSkipSNIHostnameVerifier());
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        //conn.setConnectTimeout(5*1000);
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


