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
        <title>Service Catalog</title>
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
            <div id="service-overview">
                <sql:query dataSource="${front_conn}" sql="SELECT DISTINCT S.name, S.filename, S.description FROM service S JOIN acl A, acl_entry_group G, acl_entry_user U 
                           WHERE S.atomic = 0 AND A.service_id = S.service_id 
                           AND ((A.acl_id = G.acl_id AND G.usergroup_id = ?) OR (A.acl_id = U.acl_id AND U.user_id = ?))" var="servlist">
                    <sql:param value="${user.getActiveUsergroup()}" />
                    <sql:param value="${user.getId()}" />
                </sql:query>

                <table class="management-table" id="service-table">                    
                    <thead>
                        <tr>
                            <th>Service Name</th>
                            <th>Description</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>                    
                        <c:forEach var="service" items="${servlist.rows}">
                            <tr>
                                <td>${service.name}</td>
                                <td>${service.description}</td>
                                <td>
                                    <jsp:element name="button">
                                        <jsp:attribute name="class">button-service-select</jsp:attribute>
                                        <jsp:attribute name="id">${service.filename}</jsp:attribute>                                           
                                        <jsp:body>Select</jsp:body>
                                    </jsp:element>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <br>
            <button type="button" class="hide" id="button-service-cancel">Cancel</button>
            <div id="service-specific">                
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
