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
        <title>Driver Service</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
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
                        <div id="service-top">
                            <c:if test="${not empty param.self}">
                                <button type="button" id="button-service-return">Cancel</button>
                            </c:if>
                            <table class="management-table">
                                <thead>
                                    <tr>
                                        <th>Driver Type</th>
                                        <th>
                                            <select form="driver-form" name="driver_id" onchange="driverSelect(this)">                                                
                                                <option value="none"></option>
                                                <option value="stubdriver">Stub</option>
                                                <option value="awsdriver">AWS</option>
                                                <option value="versaNSDriver">VersaStack</option>
                                                <option value="openStackDriver">OpenStack</option>
                                            </select>
                                        </th>
                                    </tr>
                                </thead>
                            </table>
                        </div>
                        <div id="service-bottom">                             
                            <form id="driver-form" action="/VersaStack-web/ops/srvc/driver.jsp" method="post">
                                <input type="hidden" name="sub" value="true" />
                                <table class="management-table" id="service-form">                    
                                    <thead>
                                        <tr>
                                            <th>Driver Details</th>
                                            <th style="text-align: right"></th>                            
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:if test="${param.driver_id == 'stubdriver'}">
                                            <tr>
                                                <td>Stub</td>
                                                <td>Driver</td>
                                            </tr>
                                        </c:if>
                                        <c:if test="${param.driver_id == 'awsdriver'}">
                                            <tr>
                                                <td>AWS</td>
                                                <td>Driver</td>
                                            </tr>                                            
                                        </c:if>
                                        <c:if test="${param.driver_id == 'versaNSDriver'}">
                                            <tr>
                                                <td>VersaStack</td>
                                                <td>Driver</td>
                                            </tr>                                            
                                        </c:if>
                                        <c:if test="${param.driver_id == 'openStackDriver'}">
                                            <tr>
                                                <td>OpenStack</td>
                                                <td>Driver</td>
                                            </tr>                                            
                                        </c:if>

                                        <c:if test="${param.driver_id != 'none'}">
                                            <tr>
                                                <td></td>
                                                <td>
                                                    <input class="button-register" name="install" type="submit" value="Install" />
                                                    <input class="button-register" name="uninstall" type="submit" value="Uninstall" />
                                                </td>
                                            </tr>
                                        </c:if>    
                                    </tbody>
                                </table>
                            </form>
                        </div>
                    </c:when>

                    <c:when test="${param.sub == 'true'}">
                        <div id="service-process">
                            <c:if test="${not empty param.install}">
                                <c:redirect url="/ops/srvc/driver.jsp?ret=${serv.driverInstall(param.driver_id)}" />
                            </c:if>
                            <c:if test="${not empty param.uninstall}">
                                <c:redirect url="/ops/srvc/driver.jsp?ret=${serv.driverUninstall(param.driver_id)}" />
                            </c:if>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="form-result" id="service-result">
                            <c:choose>
                                <c:when test="${param.ret == '0'}">
                                    Success
                                </c:when>
                                <c:when test="${param.ret == '1'}">
                                    Invalid Driver ID
                                </c:when>
                                <c:when test="${param.ret == '2'}">
                                    Error (Un)Installing Driver
                                </c:when>
                                <c:when test="${param.ret == '3'}">
                                    Connection Error
                                </c:when>
                            </c:choose>                        

                            <br><a href="/VersaStack-web/ops/srvc/driver.jsp?self=true">(Un)Install Another Driver.</a>                                
                            <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>       
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
            });
        </script>        
    </body>
</html>
