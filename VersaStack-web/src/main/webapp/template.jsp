<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/Testing/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="user" class="loginTest.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/Testing/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Overview</title>
        <script src="/Testing/js/jquery/jquery.js"></script>
        <script src="/Testing/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/Testing/css/animate.min.css">
        <link rel="stylesheet" href="/Testing/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/Testing/css/bootstrap.css">
        <link rel="stylesheet" href="/Testing/css/style.css">
    </head>
    
    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            
        </div>        
        <!-- JS -->
        <script>
            $(function(){
                $("#sidebar").load("/Testing/sidebar.html"); 
                $("#nav").load("/Testing/navbar.html"); 
            });
        </script>        
    </body>
</html>
