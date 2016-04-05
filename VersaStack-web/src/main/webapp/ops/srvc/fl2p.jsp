<%-- 
    Document   : fl2p
    Created on : Mar 3, 2016, 10:39:58 AM
    Author     : ranjitha
--%>

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
        <title>Flow Based Layer2 Service</title>
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

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

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
                <c:when test="${empty param.ret}">  <!-- Display this section when no return value supplied -->
                    <div id="service-specific">                        
                        <div id="service-top">
                            <div id="service-title">
                                Flow Based Layer 2 Protection 
                            </div>
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">

                                </table>
                            </div>
                        </div>
                        <div id="service-bottom">
                            <div id="service-fields">
                                <table class="management-table" id="service-form" style="margin-bottom: 0px;"> 
                                    <thead>
                                        <tr>
                                            <th>Templates</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td>1 Link</td>
                                            <td><button onclick="applyFL2PTemplate(1)">Apply</button></td>
                                        </tr>
                                    </tbody>
                                </table>
                                <form id="custom-form" action="/VersaStack-web/ServiceServlet" method="post">
                                    <input type="hidden" name="userID" value="${user.getId()}"/>
                                    <table class="management-table" id="net-custom-form">
                                        <thead>
                                            <tr>
                                                <th>Custom</th>
                                                <th><div id="custom-toggle">Display</div></th>
                                        </tr>
                                        </thead>
                                        <tbody id="custom-fields">
                                            <tr id="flow1">
                                                <td>TopoUri</td>
                                                <td>
                                                    <div>
                                                        <input type="text" name="topUri" size="60" placeholder="Topo-URI">
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>Eth_src_port</td>
                                                <td>
                                                    <div>
                                                        <input type="text" name="eth-src" size="60" placeholder="Source">
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>Eth_des_port</td>
                                                <td>
                                                    <div>
                                                        <input type="text" name="eth-des" size="60" placeholder="Destination">
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td></td>
                                                <td><input type="submit" name="custom" value="Submit" /></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <input type="hidden" name="fl2pCreate" value="true">
                                </form>
                            </div>
                        </div>
                    </div> 
                </c:when>

                <c:otherwise>                       <!-- Display this section when return value supplied -->
                    <div class="form-result" id="service-result">
                        <c:choose>
                            <c:when test="${param.ret == '0'}">
                                Installation Success!
                            </c:when>
                            <c:when test="${param.ret == '1'}">
                                Error 1.
                            </c:when>    
                            <c:when test="${param.ret == '2'}">
                                Error 2.
                            </c:when>    
                            <c:when test="${param.ret == '3'}">
                                Error 3.
                            </c:when>                                      
                        </c:choose>                        

                        <br><a href="/VersaStack-web/ops/srvc/template.jsp?self=true">Repeat.</a>                                
                        <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                        <br><a href="/VersaStack-web/orch/graphTest.jsp">Return to Graphic Orchestration.</a>
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
            });
        </script>        
    </body>
</html>

