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
            
        } 
        // VM installation
        else if (request.getParameter("install") != null) {
            HashMap<String, String> paramMap = new HashMap<>();
            Enumeration paramNames = request.getParameterNames();

            // Collate named elements
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                String paramValue = paramValues[0];
                if (paramValue.length() != 0) {
                    paramMap.put(paramName, paramValue);
                }
            }

            /*
            // Connect dynamically generated elements
            for (int i = 1; i <= 5; i++) {
                if (paramMap.containsKey("apropname" + i)) {
                    paramMap.put(paramMap.get("apropname" + i), paramMap.get("apropval" + i));

                    paramMap.remove("apropname" + i);
                    paramMap.remove("apropval" + i);
                }
            }
            */           

            int retCode = -1;
            //retCode = servBean.vmInstall(paramMap);

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
