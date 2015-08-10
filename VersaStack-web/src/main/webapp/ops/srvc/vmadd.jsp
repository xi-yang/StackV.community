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
                                    <table class="management-table" id="service-form" style="margin-bottom: 0px;"> 
                                        <c:if test="${param.vm_type == 'aws'}">
                                            <thead>
                                                <tr>
                                                    <th>AWS Details</th>
                                                    <th style="text-align: right"></th>                            
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <c:if test="${not empty param.topo}">
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
                                                    </c:if>
                                                </tr> 
                                                <tr>
                                                    <td>VPC ID</td>
                                                    <td><input type="text" name="vpcID" required></td>
                                                    <td>
                                                    <select name="vpcID" required>
                                                            <option></option>
                                                            <option value="urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9">vpc-8c5f22e9</option>
                                                        </select>
                                                    </td>
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
                                                        <select name="instanceType" required onchange="instanceSelect(this)">
                                                            <option></option>
                                                            <option value="instance1">cpu:1, ram:512 MB</option>
                                                            <option value="instance2">cpu:2, ram:1 GB</option>
                                                            <option value="instance3">cpu:4, ram:4 GB</option>
                                                        </select>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>Number of VMs</td>
                                                    <td><input type="number" name="vmQuantity" required></td>
                                                </tr>
                                                <tr>
                                                    <td>VM Subnets</td>
                                                    <td>
                                                        <select name="subnets" required multiple size="5">
                                                            <option value="urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16,10.0.0.0">aws-cloud / subnet-2cd6ad16,10.0.0.0</option>
                                                            <option value="urn:ogf:network:aws.amazon.com:aws-cloud:subnet-85135bbf,10.0.1.0">aws-cloud / subnet-85135bbf,10.0.1.0</option>
                                                        </select>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>Volumes</td>
                                                    <td>
                                                        <table id="volume-table">
                                                            <thead>
                                                                <tr>
                                                                    <th>Name</th>
                                                                    <th>Device Path</th>
                                                                    <th>Snapshot</th>
                                                                    <th>Size</th>
                                                                    <th>Type</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>                                                                
                                                                <tr>
                                                                    <td>Root</td>
                                                                    <td>
                                                                        <input type="text" name="root-path" style="width: 8em;" readonly required/>
                                                                    </td>
                                                                    <td>
                                                                        <input type="text" name="root-snapshot" style="width: 8em;" readonly required/>
                                                                    </td>
                                                                    <td>
                                                                        <input type="number" name="root-size" style="width: 4em; text-align: center;" required/>
                                                                    </td>
                                                                    <td>
                                                                        <select name="root-type" required>
                                                                            <option></option>
                                                                            <option value="standard">Standard</option>
                                                                            <option value="io1">io1</option>
                                                                            <option value="gp2">gp2</option>
                                                                        </select>                                                        
                                                                    </td>
                                                                </tr>
                                                            </tbody>
                                                        </table>
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <input class="button-register" name="install" type="submit" value="Install" />
                                                        <input class="button-register" type="button" 
                                                               value="Add Volume" onClick="addVolume()">
                                                        <input type="hidden" name="graphTopo" value="none"/>
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
                                    Plugin Failure.
                                </c:when>    
                                <c:when test="${param.ret == '3'}">
                                    Connection Error.
                                </c:when>    
                                <c:when test="${param.ret == '4'}">
                                    Error Parsing Parameters.
                                </c:when>                                        
                            </c:choose>                        

                            <br><a href="/VersaStack-web/ops/srvc/vmadd.jsp?self=true">Install Another VM.</a>                                
                            <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                            <br><a href="/VersaStack-web/orch/graphTest.html">Return to Graphic Orchestration.</a>
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
