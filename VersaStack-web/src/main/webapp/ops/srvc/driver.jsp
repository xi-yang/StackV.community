<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Driver Service></title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>

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
                <!-- Form View -->
                <c:when test="${param.ret != 'sub'}">
                    <div id="service-specific">                
                        <form action="/VersaStack-web/ops/srvc/driver.jsp" method="post">
                            <input type="hidden" name="ret" value="sub" />
                            <table class="management-table" id="service-form">                    
                                <thead>
                                    <tr>
                                        <th>Driver Details</th>
                                        <th style="text-align: right"></th>                            
                                    </tr>
                                </thead>
                                <tbody>                    
                                    <tr>
                                        <td>Driver ID/Name</td>
                                        <td><input name="driver-id" type="text" placeholder='Driver ID'/></td>
                                    </tr>
                                    <tr>
                                        <td></td>
                                        <td><input id="button-register" name="change" type="submit" value="Submit" /></td>
                                    </tr>
                                </tbody>
                            </table>

                        </form>
                    </div>
                </c:when>
                <!-- Result View -->
                <c:otherwise>
                    <div id="service-result">
                        <!-- ${serv.driverInstall(driver-id)} -->
                        
                        <c:forEach begin="1" end="${param.count}" varStatus="loop">
                            <p style="color: ${param.color}">${user.getFirstName()} ${user.getLastName()}</p>
                        </c:forEach>
                        <br><br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
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
                });
                $("#nav").load("/VersaStack-web/navbar.html");

                $("#button-service-cancel").click(function (evt) {
                    $("#service-specific").load("/VersaStack-web/ops/catalog.jsp");

                    evt.preventDefault();
                });
            });
        </script>        
    </body>
</html>
