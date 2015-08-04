package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VMServlet extends HttpServlet {

    /**
     * Collects parameters from VM Installation form and collates into HashMap,
     * before passing the new map into the serviceBean for model modification.
     * <br/>
     * Upon completion, servlet redirects to service page with error code.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        serviceBeans servBean = new serviceBeans();

        // Bi-directional search function
        if (request.getParameter("search") != null) {

        } // VM installation
        else if (request.getParameter("install") != null) {
            HashMap<String, String> paramMap = new HashMap<>();
            Enumeration paramNames = request.getParameterNames();

            // Collate named elements
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);                
                if (paramValues.length == 1) {
                    String paramValue = paramValues[0];
                    paramMap.put(paramName, paramValue);
                } else if (paramValues.length > 1) {
                    String fullValue = "";
                    for (String paramValue : paramValues) {
                        fullValue += paramValue + "\r\n";
                    }
                    fullValue = fullValue.substring(0, fullValue.length() - 4);
                    paramMap.put(paramName, fullValue);
                }                
            }

            /*
             Map<String, String> vmMap = new HashMap<>();
             vmMap.put("versionGroup", "55e0cb18-2b2d-4aa1-968c-a975e34edca3");
             vmMap.put("topologyUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
             vmMap.put("region", "us-east-1");
             vmMap.put("vpcID", "urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9");
             vmMap.put("subnets", "urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16,10.0.0.0\r\nurn:ogf:network:aws.amazon.com:aws-cloud:subnet-85135bbf,10.0.1.0");
             vmMap.put("volumes", "8,standard,/dev/xvda,snapshot\r\n8,standard,/dev/sdb,snapshot");
             */
            
            // Format volumes
            String volString = "";

            // Include root
            volString += paramMap.get("root-size") + ",";
            volString += paramMap.get("root-type") + ",";
            volString += paramMap.get("root-path") + ",";
            volString += paramMap.get("root-snapshot") + "\r\n";
            paramMap.remove("root-size");
            paramMap.remove("root-type");
            paramMap.remove("root-path");
            paramMap.remove("root-snapshot");

            for (int i = 1; i <= 10; i++) {
                if (paramMap.containsKey(i + "-path")) {
                    volString += paramMap.get(i + "-size") + ",";
                    volString += paramMap.get(i + "-type") + ",";
                    volString += paramMap.get(i + "-path") + ",";
                    volString += paramMap.get(i + "-snapshot") + "\r\n";
                    paramMap.remove(i + "-size");
                    paramMap.remove(i + "-type");
                    paramMap.remove(i + "-path");
                    paramMap.remove(i + "-snapshot");                    
                }
            }
            paramMap.put("volumes", volString);

            paramMap.remove("install");
            int retCode = -1;
            for (int i = 0; i < Integer.parseInt(paramMap.get("vmQuantity")); i++) {
                retCode = servBean.vmInstall(paramMap);
            }

            response.sendRedirect("/VersaStack-web/ops/srvc/vmadd.jsp?ret=" + retCode);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Collects parameters from VM Installation form and collates into HashMap, "
                + "before passing the new map into the serviceBean for model modification. "
                + "Upon completion, servlet redirects to service page with error code.";
    }

}
