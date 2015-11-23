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
        <title>Network Creation Service</title>
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
        <script>
            $(document).ready(function () {
                $("#custom-toggle").click(function (evt) {
                    $("#custom-fields").toggleClass("hide");

                    evt.preventDefault();
                });
            });
        </script>

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
                                Network Creation
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
                                <form id="service-template-form" action="/VersaStack-web/ServiceServlet" method="post">
                                    <input type="hidden" name="userID" value="${user.getId()}"/>
                                    <table class="management-table" id="net-template-form" style="margin-bottom: 0px;"> 
                                        <thead>
                                            <tr>
                                                <th>Templates</th>
                                                <th></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td>Basic</td>
                                                <td><input type="submit" name="template1" value="Select" /></td>
                                            </tr>                                            
                                            <tr>
                                                <td>Advanced</td>
                                                <td><input type="submit" name="template2" value="Select" /></td>
                                            </tr>
                                        </tbody>
                                    </table>    
                                    <input type="hidden" name="netCreate" value="true"/>
                                </form>  
                                <form id="service-custom-form" action="/VersaStack-web/ServiceServlet" method="post">
                                    <input type="hidden" name="userID" value="${user.getId()}"/>    
                                    <table class="management-table" id="net-custom-form">
                                        <thead>
                                            <tr>
                                                <th>Custom</th>
                                                <th><div id="custom-toggle">Display</div></th>
                                        </tr>
                                        </thead>
                                        <tbody id="custom-fields">
                                            <tr>
                                                <td>Topology URI</td>
                                                <td>
                                                    <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance" var="driverlist" />
                                                    <select name="topoUri" >
                                                        <option></option>
                                                        <c:forEach var="driver" items="${driverlist.rows}">
                                                            <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                                        </c:forEach>
                                                    </select>   
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>Network Type</td>
                                                <td><input type="text" name="netType" /></td>
                                            </tr>
                                            <tr>
                                                <td>Network CIDR</td>
                                                <td><input type="text" name="netCidr" /></td>
                                            </tr>
                                            <tr>
                                                <td>Route Table</td>
                                                <td>
                                                    <div id="route-block">
                                                        <div>
                                                            <input type="text" name="route1-from" placeholder="From"/>
                                                            <input type="text" name="route1-to" placeholder="To"/>
                                                            <input type="text" name="route1-next" placeholder="Next Hop"/>
                                                        </div>
                                                        <div>
                                                            <input type="text" name="route-from" placeholder="From"/>
                                                            <input type="text" name="route-to" placeholder="To"/>
                                                            <input type="text" name="route-next" placeholder="Next Hop"/>
                                                        </div>
                                                    </div>
                                                    <div>
                                                        <input type="checkbox" name="route-prop" value="true"/>   Enable VPN Routes Propogation
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td></td>
                                                <td></td>
                                            </tr>
                                            <tr>
                                                <td></td>
                                                <td><input type="submit" name="custom" value="Submit" /></td>
                                            </tr>
                                        </tbody>
                                    </table>                                    
                                    <input type="hidden" name="netCreate" value="true"/>
                                </form>
                            </div>
                        </div>
                    </div> 
                </c:when>

                <c:otherwise>                       <!-- Display this section when return value supplied -->
                    <div class="form-result" id="service-result">
                        <c:choose>
                            <c:when test="${param.ret == '0'}">
                                Creation Success!
                            </c:when>
                            <c:when test="${param.ret == '1'}">
                                Connection Error.
                            </c:when>    
                            <c:when test="${param.ret == '2'}">
                                Back-end Interaction Error.
                            </c:when>    
                            <c:when test="${param.ret == '3'}">
                                Failed to Create Network.
                            </c:when>                                      
                        </c:choose>                        

                        <br><a href="/VersaStack-web/ops/srvc/netcreate.jsp?self=true">Create Another Network.</a>                                
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
