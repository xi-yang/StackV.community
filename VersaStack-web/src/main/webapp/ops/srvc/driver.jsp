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

    <!-- Temp Connection -->
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
                <c:when test="${empty param.sub && empty param.ret}">
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
                                                    <option>Install</option>                                                
                                                    <c:choose>
                                                        <c:when test="${param.form_install == 'uninstall'}">
                                                            <option value="uninstall" selected>Uninstall</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="uninstall">Uninstall</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </select>
                                                <c:if test="${param.form_install != 'uninstall'}">
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
                                            <!-- Install Form -->
                                            <c:if test="${param.form_install != 'uninstall'}">
                                                <c:if test="${param.driver_id == 'stubdriver'}">
                                                    <tr>
                                                        <td>Stub</td>
                                                        <td>Driver</td>
                                                    </tr>
                                                </c:if>
                                                <c:if test="${param.driver_id == 'awsdriver'}">
                                                    <tr>
                                                        <td>Name</td>
                                                        <td><input type="text" name="par1" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>Amazon Access ID</td>
                                                        <td><input type="text" name="par2" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>Amazon Secret Key</td>
                                                        <td><input type="password" name="par3" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>Region</td>
                                                        <td>
                                                            <select name="par4" required>
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
                                                        <td></td>
                                                        <td></td>
                                                    </tr>   
                                                    <tr>
                                                        <td></td>
                                                        <td></td>
                                                    </tr>                                            
                                                </c:if>
                                                <c:if test="${param.driver_id == 'openStackDriver'}">
                                                    <tr>
                                                        <td>Name</td>
                                                        <td><input type="text" name="par1" required></td>
                                                    </tr>
                                                    <tr>
                                                        <td>OpenStack Username</td>
                                                        <td><input type="text" name="par2" required></td>
                                                    </tr>   
                                                    <tr>
                                                        <td>OpenStack Password</td>
                                                        <td><input type="password" name="par3" required></td>
                                                    </tr>                                            
                                                </c:if>

                                                <c:if test="${param.driver_id != 'none'}">
                                                    <tr>
                                                        <td></td>
                                                        <td>
                                                            <input class="button-register" name="install" type="submit" value="Install" />

                                                        </td>
                                                    </tr>
                                                </c:if> 
                                            </c:if>
                                            <!-- Uninstall Form -->
                                            <c:if test="${param.form_install == 'uninstall'}">
                                                <tr>
                                                    <sql:query dataSource="${rains_conn}" sql="SELECT driverEjbPath, topologyUri FROM driver_instance" var="driverlist" />
                                                    <td>Select Driver</td>
                                                    <td>                                                        
                                                        <select>
                                                            <c:forEach var="driver" items="${driverlist.rows}">
                                                                <option value="${driver.topologyUri}">${driver.driverEjbPath} - ${driver.topologyUri}</option>
                                                            </c:forEach>
                                                        </select>
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

                    <c:when test="${param.sub == 'true'}">
                        <div id="service-process">
                            <c:if test="${not empty param.install}">
                                <c:redirect url="/ops/srvc/driver.jsp?ret=${serv.driverInstall(
                                                                            param.driver_id,
                                                                            param.par1,
                                                                            param.par2,
                                                                            param.par3,
                                                                            param.par4)}" />
                            </c:if>
                            <c:if test="${not empty param.uninstall}">
                                <c:redirect url="/ops/srvc/driver.jsp?ret=${serv.driverInstall(
                                                                            param.driver_id,
                                                                            param.par1,
                                                                            param.par2,
                                                                            param.par3,
                                                                            param.par4)}" />
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
