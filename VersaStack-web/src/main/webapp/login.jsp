<%@page import="java.security.MessageDigest"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "errorPage.jsp" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<!DOCTYPE html>
<html >
    <head>
        <meta charset="UTF-8">
        <title>Login</title>

        <link rel="stylesheet" href="css/style.css">
        <!-- <link rel="stylesheet" href="css/reset.css"> -->
        <link rel="stylesheet" href="css/animate.min.css">
        <link rel="stylesheet" href="css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
    </head>

    <body>
        <!-- Eventually update this to 2.0 EL spec -->
        <%
            session.setMaxInactiveInterval(900);  // 15 minute time out 
            user.login(request.getParameter("username"),
                    request.getParameter("password"));
        %>          
        <p style="text-align:center;vertical-align:middle">
            <%
                if (user.isLoggedIn()) {
                    out.println("Hello " + user.getFirstName() + " " + user.getLastName()
                            + "<br/>Please wait while you're transferred.<br/>");
                    response.sendRedirect("home.jsp");
                } else {
                    response.sendRedirect("index.jsp?ret=auth");
                }
            %>
        </p>
    </body>
</html>
