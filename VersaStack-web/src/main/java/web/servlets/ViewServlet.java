/* 
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.
 * 
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import web.beans.userBeans;

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
        userBeans user = (userBeans) request.getSession().getAttribute("user");

        if (request.getParameter("viewName") != null) {
            List<String> filters = new ArrayList<>();

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

            String retView = servBean.createModelView(filters.toArray(new String[filters.size()]));
            if (retView != null) {
                user.addModel(name, retView);
                response.sendRedirect("/VersaStack-web/ops/srvc/viewcreate.jsp?ret=0");
            } else {
                response.sendRedirect("/VersaStack-web/ops/srvc/viewcreate.jsp?ret=3");
            }
        } else if (request.getParameter("newModel") != null) {
            String newModel = request.getParameter("newModel"); // this is your data sent from client

            user.addModel("base", newModel);
        } else if (request.getParameter("filterName") != null) {
            user.setCurr(request.getParameter("filterName"), request.getParameter("filterModel"));
        } else if (request.getParameter("modelName") != null) {
            user.removeModel(request.getParameter("modelName"));

            response.sendRedirect("/VersaStack-web/ops/srvc/viewcreate.jsp?self=true");
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
