<%@page import="java.security.MessageDigest"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
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
            web.beans.userBeans.update(
                    request.getParameter("username"),
                    request.getParameter("password"),
                    request.getParameter("firstname"),
                    request.getParameter("lastname"),
                    request.getParameter("email"));
                       
            if (request.getParameter("return").equals("groups")) {                
                response.sendRedirect("user_groups.jsp?id=" + request.getParameter("group_id"));
            } else response.sendRedirect("home.jsp");
        %>          
    </body>
</html>
