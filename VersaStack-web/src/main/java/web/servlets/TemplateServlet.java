package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TemplateServlet extends HttpServlet {

    /**
     *
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

        int retCode = -1;
        //retCode = servBean.template();
        response.sendRedirect("/VersaStack-web/ops/srvc/template.jsp?ret=" + retCode);
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
