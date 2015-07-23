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
        <title>Virtual Machine Service</title>
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
                       url="jdbc:mysql://localhost:8889/rainsdb"
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
                <c:when test="${empty param.ret}">
                    <div id="service-specific">
                        <div id="service-top">
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">
                                    <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance" var="driverlist" />
                                    <thead>
                                        <tr>
                                            <th>Select Topology</th>
                                            <th>
                                                <select form="vm-form" name="topologyUri" onchange="topoSelect(this)">                                                                                                  
                                                    <option></option>
                                                    <c:forEach var="driver" items="${driverlist.rows}">
                                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                                    </c:forEach>
                                                </select>                                                
                                            </th>
                                        </tr>
                                    </thead>
                                </table>
                            </div>
                        </div>
                        <div id="service-bottom">
                            <div id="service-fields">
                                <form id="vm-form" action="/VersaStack-web/VMServlet" method="post">
                                    <table class="management-table" id="service-form"> 
                                        <c:if test="${param.vm_type == 'aws'}">
                                            <thead>
                                                <tr>
                                                    <th>AWS Details</th>
                                                    <th style="text-align: right"></th>                            
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <sql:query dataSource="${rains_conn}" sql="SELECT value FROM driver_instance_property P, driver_instance I 
                                                               WHERE property = 'region' AND I.id = P.driverInstanceId AND I.topologyUri = ?" var="regionlist">
                                                        <sql:param value="${param.topo}" />
                                                    </sql:query>

                                                    <td>Region</td>
                                                    <td>
                                                        <c:forEach var="reg" items="${regionlist.rows}">
                                                            <input type="text" name="region" value="${reg.value}" readonly />
                                                        </c:forEach>
                                                    </td>
                                                </tr> 
                                                <tr>
                                                    <td>VPC ID</td>
                                                    <td><input type="text" name="vpcID" required></td>
                                                </tr>  
                                                <tr>
                                                    <td>OS Type</td>
                                                    <td>
                                                        <select name="ostype" required>
                                                            <option></option>
                                                            <option value="windows">Windows 7</option>
                                                            <option value="ubuntu">Ubuntu</option>
                                                        </select>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>Instance Type</td>
                                                    <td>
                                                        <select name="instanceType" required>
                                                            <option></option>
                                                            <option>cpu:1, ram:512 MB</option>
                                                            <option>cpu:2, ram:1 GB</option>
                                                            <option>cpu:4, ram:4 GB</option>
                                                        </select>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>Number of VMs</td>
                                                    <td><input type="number" name="vmQuantity" required></td>
                                                </tr>   
                                                <tr>
                                                    <td>VM Subnets</td>
                                                    <td><textarea rows="3" cols="50" name="subnets"></textarea></td>
                                                </tr>
                                                <tr>
                                                    <td>Number of volume created</td>
                                                    <td><textarea rows="3" cols="50" name="volumes"></textarea></td>
                                                </tr>                                            
                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <input class="button-register" name="install" type="submit" value="Install" />
                                                        <!-- <input class="button-register" type="button" 
                                                               value="Add Additional Properties" onClick="addPropField()"> -->
                                                    </td>
                                                </tr> 
                                            </tbody>
                                        </c:if>
                                    </table>
                                </form>
                            </div>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="form-result" id="service-result">
                            <c:choose>
                                <c:when test="${param.ret == '0'}">
                                    Installation Success!
                                </c:when>
                                <c:when test="${param.ret == '1'}">
                                    Error Requesting System Instance UUID.
                                </c:when>    
                                <c:when test="${param.ret == '2'}">
                                    Failure while Unplugging.
                                </c:when>    
                                <c:when test="${param.ret == '3'}">
                                    Connection Error.
                                </c:when>    
                                <c:when test="${param.ret == '4'}">
                                    Error Building Model.
                                </c:when>                                        
                            </c:choose>                        

                            <br><a href="/VersaStack-web/ops/srvc/vmadd.jsp?self=true">(Un)Install Another Driver.</a>                                
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
