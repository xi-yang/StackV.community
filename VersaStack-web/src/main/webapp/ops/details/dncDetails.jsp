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
        <title>Service Details</title>
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

    <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/Frontend"
                       user="front_view"  password="frontuser"/>

    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">      
            <button type="button" id="button-service-return">Back to Catalog</button>
            <sql:query dataSource="${front_conn}" sql="SELECT S.name, X.superState FROM service S, service_instance I, service_state X
                       WHERE referenceUUID = ? AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id" var="instancelist">
                <sql:param value="${param.uuid}" />
            </sql:query>

            <div id="instance-pane">
                <c:forEach var="instance" items="${instancelist.rows}">            
                    <table class="management-table" id="details-table">
                        <thead>
                            <tr>
                                <th>${instance.name} Service Details</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>Service Reference UUID</td>
                                <td>${param.uuid}</td>
                            </tr>
                            <tr>
                                <td>Service State</td>
                                <td>${instance.superState}</td>
                            </tr>
                            <tr>
                                <td>Operation Status</td>
                                <td id="instance-status"></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td>
                                    <div class="service-instance-panel">
                                        <button onClick="cancelInstance('${param.uuid}')">Cancel</button>
                                        <button onClick="deleteInstance('${param.uuid}')">Delete</button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>


                    <sql:query dataSource="${front_conn}" sql="SELECT D.delta, S.superState 
                               FROM service_delta D, service_instance I, service_state S 
                               WHERE I.referenceUUID = ? AND I.service_instance_id = D.service_instance_id AND I.service_state_id = S.service_state_id" var="deltalist">
                        <sql:param value="${param.uuid}" />
                    </sql:query>

                    <c:forEach var="delta" items="${deltalist.rows}">
                        <table class="management-table" id="delta-table">
                            <thead>
                                <tr>
                                    <th>Delta Details</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Delta State</td>
                                    <td>${delta.superState}</td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td>${delta.delta}</td>
                                </tr>                        
                            </tbody>
                        </table>
                    </c:forEach>
                </c:forEach>
            </div>  
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                var uuid = getUrlParameter('uuid');
                checkInstance(uuid);

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
