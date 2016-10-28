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

package net.maxgigapop.mrs.driver.opendaylight;

import net.maxgigapop.mrs.driver.onosystem.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ejb.EJBException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import net.maxgigapop.mrs.common.DriverUtil;

/**
 *
 * @author diogo
 */
public class RestconfConnector {
    //pull network topology
    public JSONObject getNetworkTopology(String subsystemBaseUrl, String username, String password) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/operational/network-topology:network-topology"); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:getNetworkTopology failed with HTTP return code:"+response[1]);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
            return jsonObject;
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:getNetworkTopology failed.", ex);
        }
    }
    
    //pull configured flows
    public JSONObject getConfigFlows(String subsystemBaseUrl, String username, String password) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes"); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:getNetworkTopology failed with HTTP return code:"+response[1]);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
            return jsonObject;
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:getConfigFlows failed.", ex);
        }
    }
}
