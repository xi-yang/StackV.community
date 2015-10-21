package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DriverServlet extends HttpServlet {

    /**
     * Collects parameters from Driver forms and collates into HashMap, before
     * passing the new map into the serviceBean for model modification.
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

        // Connect dynamically generated elements
        for (int i = 1; i <= 5; i++) {
            if (paramMap.containsKey("apropname" + i)) {
                paramMap.put(paramMap.get("apropname" + i), paramMap.get("apropval" + i));

                paramMap.remove("apropname" + i);
                paramMap.remove("apropval" + i);
            }
        }

        paramMap.remove("driver_id");
        paramMap.remove("form_install");

        serviceBeans servBean = new serviceBeans();

        int retCode = -1;
        // Call appropriate driver control method
        if (paramMap.containsKey("install")) {
            paramMap.remove("install");
            retCode = servBean.driverInstall(paramMap);
        } else if (paramMap.containsKey("uninstall")) {
            retCode = servBean.driverUninstall(request.getParameter("topologyUri"));
        }

        response.sendRedirect("/VersaStack-web/ops/srvc/driver.jsp?ret=" + retCode);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Collects parameters from Driver forms and collates into HashMap, "
                + "before passing the new map into the serviceBean for model modification. "
                + "Upon completion, servlet redirects to service page with error code.";
    }

}
