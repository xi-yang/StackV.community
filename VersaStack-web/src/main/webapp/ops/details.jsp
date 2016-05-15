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
                       url="jdbc:mysql://localhost:3306/frontend"
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
            <sql:query dataSource="${front_conn}" sql="SELECT S.name, X.super_state FROM service S, service_instance I, service_state X
                       WHERE referenceUUID = ? AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id" var="instancelist">
                <sql:param value="${param.uuid}" />
            </sql:query>

            <c:forEach var="instance" items="${instancelist.rows}">
                <c:if test="${instance.name == 'Dynamic Network Connection'}">
                    <c:redirect url="/ops/details/dncDetails.jsp">
                        <c:param name="uuid" value="${param.uuid}"></c:param>
                    </c:redirect>
                </c:if>
                <c:if test="${instance.name == 'Flow Based Layer2 Protection'}">
                    <c:redirect url="/ops/details/fl2pDetails.jsp">
                        <c:param name="uuid" value="${param.uuid}"></c:param>
                    </c:redirect>
                </c:if>
                
                <c:redirect url="/ops/details/templateDetails.jsp">
                    <c:param name="uuid" value="${param.uuid}"></c:param>
                </c:redirect>
            </c:forEach>

        </div>      
        <div id="tag-panel"> 
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
                $("#tag-panel").load("/VersaStack-web/tagPanel.jsp", null);                        
            });

        </script>        
    </body>
</html>
