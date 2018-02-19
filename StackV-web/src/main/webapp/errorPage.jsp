<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>    
    <head>
        <meta charset="UTF-8">
        <title>Error</title>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/bootstrap.css">
        <link rel="stylesheet" href="/StackV-web/css/style.css">
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
            <br><br>
            Fatal Error!<br>

            <table width="75%" border="1">
                <tr valign="top">
                    <td width="40%"><b>Error:</b></td>
                    <td>${pageContext.exception}</td>
                </tr>
                <tr valign="top">
                    <td><b>URI:</b></td>
                    <td>${pageContext.errorData.requestURI}</td>
                </tr>
                <tr valign="top">
                    <td><b>Status code:</b></td>
                    <td>${pageContext.errorData.statusCode}</td>
                </tr>
                <tr valign="top">
                    <td><b>Stack trace:</b></td>
                    <td>
                        <c:forEach var="trace"
                                   items="${pageContext.exception.stackTrace}">
                            <p>${trace}</p>
                        </c:forEach>
                    </td>
                </tr>
            </table>
        </div>        
    </body>
</html>
