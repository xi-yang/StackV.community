package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServiceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            serviceBeans servBean = new serviceBeans();
            
            HashMap<String, String> jobs = servBean.getJobStatuses();
            request.setAttribute("jobs", jobs);
            request.getRequestDispatcher("/VersaStack-web/orch/graphTest.jsp").forward(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(ServiceServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
