package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ViewServlet extends HttpServlet {

    /**
     * Collects parameters from View Creation form and collates filters into
     * array, before passing to serviceBean.
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
        List<String> filters =new ArrayList<>();

        String name = request.getParameter("viewName");
        // Concatenate query information
        for (int i = 1; i <= 10; i++) {
            if (request.getParameter("sparquery" + i) != null) {                

                String view;
                if (request.getParameter("viewInclusive" + i) == null) {
                    view = "false";
                } else {
                    view = "true";
                }
                String sub;
                if (request.getParameter("subRecursive" + i) == null) {
                    sub = "false";
                } else {
                    sub = "true";
                }
                String sup;
                if (request.getParameter("supRecursive" + i) == null) {
                    sup = "false";
                } else {
                    sup = "true";
                }
                
                filters.add(request.getParameter("sparquery" + i) + "\r\n" + view + "\r\n" + sub + "\r\n" + sup);
            }
        }
        
        int retCode = servBean.createModelView(name, filters.toArray(new String[filters.size()]));
        response.sendRedirect("/VersaStack-web/ops/srvc/vmadd.jsp?ret=" + retCode);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "";
    }

}
