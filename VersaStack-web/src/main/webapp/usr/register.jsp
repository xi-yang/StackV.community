<%@page import="java.security.MessageDigest"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/Testing/index.jsp" %>
<jsp:useBean id="user" class="loginTest.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<!DOCTYPE html>
<html >
    <head>
        <meta charset="UTF-8">
        <title>Registration</title>

        <link rel="stylesheet" href="Testing/css/style.css">
        <!-- <link rel="stylesheet" href="css/reset.css"> -->
        <link rel="stylesheet" href="Testing/css/animate.min.css">
        <link rel="stylesheet" href="Testing/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
    </head>

    <body>
        <!-- Eventually update this to 2.0 EL spec -->
        <%
            int retCode = loginTest.userBeans.register(
                    request.getParameter("username"),
                    request.getParameter("password"),
                    request.getParameter("firstname"),
                    request.getParameter("lastname"),
                    request.getParameter("usergroup"),
                    request.getParameter("email"));
            if (retCode == 0) {
                response.sendRedirect("user_registration.jsp?ret=succ");
            } 
            if (retCode == 3){
                response.sendRedirect("user_registration.jsp?ret=user");
            } else response.sendRedirect("user_registration.jsp?ret=fail");
        %>          
    </body>
</html>
