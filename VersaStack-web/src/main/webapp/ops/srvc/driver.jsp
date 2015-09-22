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
                <c:when test="${empty param.ret}">
                    <div id="service-specific">
                        <div id="service-top">
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">
                                    <thead>
                                        <tr>
                                            <th>Driver Type</th>
                                            <th>
                                                <select form="driver-form" name="form_install" onchange="installSelect(this)">
                                                    <option value="uninstall">Uninstall</option>                                                
                                                    <c:choose>
                                                        <c:when test="${param.form_install == 'install'}">
                                                            <option value="install" selected>Install</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="install">Install</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </select>
                                                <c:if test="${param.form_install == 'install'}">
                                                    <select form="driver-form" name="driver_id" onchange="driverSelect(this)">                                                
                                                        <option value="none"></option>
                                                        <option value="stubdriver">Stub</option>
                                                        <option value="awsdriver">AWS</option>
                                                        <option value="versaNSDriver">VersaStack</option>
                                                        <option value="openStackDriver">OpenStack</option>
                                                    </select>
                                                </c:if>
                                            </th>
                                        </tr>
                                    </thead>
                                </table>
                            </div>
                        </div>
                        <div id="service-bottom">
                            <div id="service-fields">
                                <form id="driver-form" action="/VersaStack-web/DriverServlet" method="post">
                                    <input type="hidden" name="driverID" value="${param.driver_id}"/>
                                    <table class="management-table" id="service-form">                                        
                                        <thead>
                                            <tr>
                                                <th>Driver Details</th>
                                                <th style="text-align: right"></th>                            
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <!-- Install Form -->
                                            <c:if test="${param.form_install == 'install'}">
                                                <c:if test="${param.driver_id == 'stubdriver'}">
                                                    <tr>
                                                        <td>Topology URI</td>
                                                        <td><input type="text" name="topologyUri" size="30" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>TTL</td>
                                                        <td>
                                                            <textarea rows="6" cols="50" name="ttlmodel">
                                                                
                                                            </textarea>
                                                        </td>
                                                    </tr> 
                                                </c:if>
                                                <c:if test="${param.driver_id == 'awsdriver'}">
                                                    <tr>
                                                        <td>Topology URI</td>
                                                        <td><input type="text" name="topologyUri" size="30" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>Amazon Access ID</td>
                                                        <td><input type="text" name="aws_access_key_id" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>Amazon Secret Key</td>
                                                        <td><input type="text" name="aws_secret_access_key" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>Region</td>
                                                        <td>
                                                            <select name="region" required>
                                                                <option></option>
                                                                <option value="ap-northeast-1">Asia Pacific (Tokyo)</option>
                                                                <option value="ap-southeast-1">Asia Pacific (Singapore)</option>
                                                                <option value="ap-southeast-2">Asia Pacific (Sydney)</option>
                                                                <option value="eu-central-1">Europe (Frankfurt)</option>
                                                                <option value="eu-west-1">Europe (Ireland)</option>
                                                                <option value="sa-east-1">South America (Sao Paulo)</option>
                                                                <option value="us-east-1">US East (N. Virginia)</option>
                                                                <option value="us-west-1">US West (N. California)</option>
                                                                <option value="us-west-2">US West (Oregon)</option>
                                                            </select>
                                                        </td>
                                                    </tr>                                            
                                                </c:if>
                                                <c:if test="${param.driver_id == 'versaNSDriver'}">
                                                    <tr>
                                                        <td>Topology URI</td>
                                                        <td><input type="text" name="topologyUri" size="30" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>Subsystem Base URL</td>
                                                        <td><input type="text" name="subsystemBaseUrl" required></td>
                                                    </tr>
                                                </c:if>
                                                <c:if test="${param.driver_id == 'openStackDriver'}">
                                                    <tr>
                                                        <td>Topology URI</td>
                                                        <td><input type="text" name="topologyUri" size="30" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>OpenStack Username</td>
                                                        <td><input type="text" name="username" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>OpenStack Password</td>
                                                        <td><input type="password" name="password" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>NAT Server</td>
                                                        <td><input type="checkbox" name="NATServer" value="yes"></td>
                                                    </tr>
                                                    <tr>
                                                        <td>URL</td>
                                                        <td><input type="text" name="url" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>Tenant</td>
                                                        <td><input type="text" name="tenant" required></td>
                                                    </tr>
                                                </c:if>

                                                <c:if test="${not empty param.driver_id}">                                                                                                        
                                                    <tr>
                                                        <td></td>
                                                        <td>
                                                            <input class="button-register" name="install" type="submit" value="Install" />
                                                            <input class="button-register" type="button" 
                                                                   value="Add Additional Properties" onClick="addPropField()">
                                                        </td>
                                                    </tr>
                                                </c:if> 
                                            </c:if>
                                            <!-- Uninstall Form -->
                                            <c:if test="${param.form_install != 'install'}">
                                                <tr>
                                                    <sql:query dataSource="${rains_conn}" sql="SELECT driverEjbPath, topologyUri FROM driver_instance" var="driverlist" />
                                                    <td>Select Driver</td>
                                                    <td>
                                                        <c:if test="${not empty driverlist}">
                                                            <select name="topologyUri" size="10">
                                                                <c:forEach var="driver" items="${driverlist.rows}">
                                                                    <option value="${driver.topologyUri}">${driver.driverEjbPath} - ${driver.topologyUri}</option>
                                                                </c:forEach>
                                                            </select>
                                                        </c:if>
                                                        <c:if test="${empty driverlist}">
                                                            No Drivers Present
                                                        </c:if>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <input class="button-register" name="uninstall" type="submit" value="Uninstall" />
                                                    </td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>
                                </form>
                            </div>
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
                            <br><a href="/VersaStack-web/orch/graphTest.jsp">Return to Graphic Orchestration.</a>

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
