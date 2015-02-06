package net.maxgigapop.mrs.servlets;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.maxgigapop.mrs.db.UsersDBAccess;
import net.maxgigapop.mrs.users.VersaStackUser;

@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String loginJSP = "/WEB-INF/jsp/login.jsp";
        UsersDBAccess userDatabase = new UsersDBAccess();
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        VersaStackUser u = null;

        try {
            u = userDatabase.findUserByEmail(email);
        } catch (ClassNotFoundException | SQLException ignore) {
        }

        if (request.getParameter("login") != null) {
            try {
                if (u == null) {
                    request.setAttribute("success", "false");
                    request.setAttribute("reason", "ERROR: User does not exist, check entered login information");
                } else if (userDatabase.authenticateUser(u, password)) {
                    request.setAttribute("success", "true");
                } else {
                    request.setAttribute("success", "false");
                    request.setAttribute("reason", "ERROR: Incorrect password, check entered login information");
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                request.setAttribute("success", "false");
                request.setAttribute("reason", "ERROR: Password hashing error, try again with a different password");
            } finally {
                request.setAttribute("action", "login");
            }
        } else if (request.getParameter("create") != null) {
            if (u != null) {
                request.setAttribute("success", "false");
                request.setAttribute("reason", "ERROR: User already exists, try again with a different email");
            } else {
                try {
                    userDatabase.addUser(email, password);
                    request.setAttribute("success", "true");
                } catch (ClassNotFoundException | SQLException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                    request.setAttribute("success", "false");
                    request.setAttribute("reason", "ERROR: A database failure has occured, contact the administrator");
                }
            }

            request.setAttribute("action", "create");
        } else {
            request.setAttribute("action", "error");
        }

        request.getRequestDispatcher(loginJSP).forward(request, response);
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Login/signup request dispatcher";
    }

}
