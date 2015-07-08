<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>VM Service</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
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
            <c:choose>                
                <c:when test="${empty param.sub && empty param.ret}">
                    <div id="service-specific"> 
                        <c:if test="${not empty param.self}">
                            <button type="button" id="button-service-return">Cancel</button>
                        </c:if>
                        <form action="/VersaStack-web/ops/srvc/vmadd.jsp" method="post">
                            <input type="hidden" name="sub" value="true" />
                            <table class="management-table" id="service-form">                    
                                <thead>
                                    <tr>
                                        <th>VM Details</th>
                                        <th style="text-align: right"></th>                            
                                    </tr>
                                </thead>
                                <tbody>                    
                                    <tr>
                                        <td>VM Type</td>
                                        <td>

                                        </td>
                                    </tr>
                                    <tr>
                                        <td></td>
                                        <td>
                                            <input class="button-register" name="install" type="submit" value="Install" />
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </form>
                    </div>
                </c:when>

                <c:when test="${param.sub == 'true'}">
                    <div id="service-process">
                        <c:if test="${not empty param.install}">
                            <c:redirect url="/ops/srvc/vmadd.jsp?ret=${serv.vmInstall()}" />
                        </c:if>
                    </div>
                </c:when>

                <c:otherwise>
                    <div class="form-result" id="service-result">
                        <c:choose>
                            <c:when test="${param.ret == '0'}">
                                Success
                            </c:when>                            
                        </c:choose>                        

                        <br><a href="/VersaStack-web/ops/srvc/vmadd.jsp?self=true">Add Another VM.</a>                                
                        <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
                    if (${user.isAllowed(1)}) {
                        var element = document.getElementById("service1");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(2)}) {
                        var element = document.getElementById("service2");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(3)}) {
                        var element = document.getElementById("service3");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(4)}) {
                        var element = document.getElementById("service4");
                        element.classList.remove("hide");
                    }
                });
                $("#nav").load("/VersaStack-web/navbar.html");
            });
        </script>        
    </body>
</html>
