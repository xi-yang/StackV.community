<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/Testing/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="loginTest.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>ACL Management</title>
        <script src="/Testing/js/jquery/jquery.js"></script>
        <script src="/Testing/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/Testing/css/animate.min.css">
        <link rel="stylesheet" href="/Testing/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/Testing/css/bootstrap.css">
        <link rel="stylesheet" href="/Testing/css/style.css">     

        <script>
            function aclSelect(sel) {
                $ref = "privileges.jsp?id=" + sel.value + " #acl-tables";
                $("#acl-tables").load($ref);
            }
            ;
        </script>
    </head>

    <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:8889/Frontend"
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
            <form>
                <sql:query dataSource="${front_conn}" sql="SELECT name, service_id FROM service" var="servicelist" />
                <select id="acl-select" onchange="aclSelect(this)">
                    <option value=""></option>
                    <c:forEach var="service" items="${servicelist.rows}">
                        <option value="${service.service_id}">${service.name}</option>
                    </c:forEach>
                </select>
            </form>
            <div id="acl-tables">
                <sql:query dataSource="${front_conn}" sql="SELECT G.usergroup_id, G.title, COUNT(I.user_id) ucount 
                           FROM usergroup G, user_info I, acl A, acl_entry_group E 
                           WHERE G.usergroup_id = E.usergroup_id AND G.usergroup_id = I.usergroup_id AND E.acl_id = A.acl_id AND A.service_id = ? 
                           GROUP BY G.title" var="ugrouplist">
                    <sql:param value="${param.id}"/>
                </sql:query>

                <sql:query dataSource="${front_conn}" sql="SELECT I.user_id, I.username, I.first_name, I.last_name, I.usergroup_id 
                           FROM user_info I, acl A, acl_entry_user U 
                           WHERE I.user_id = U.user_id AND U.acl_id = A.acl_id AND A.service_id = ?" var="userlist">
                    <sql:param value="${param.id}"/>
                </sql:query>

                <table class="management-table" id="service-group-table">
                    <thead>
                        <tr>
                            <th>Usergroup</th>
                            <th>Number of Members</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="group" items="${ugrouplist.rows}">
                            <tr>
                                <td>${group.title}</td>
                                <td>${group.ucount}</td>
                                <td><div class="float-right inline">
                                    <form action="" method="POST">
                                        <input type="hidden" name="usergroup_id" value="${group.usergroup_id}"/>
                                        <input type="submit" value="Remove" />  
                                    </form>
                                </div>
                                <div class="float-right inline">
                                    <form action="user_groups.jsp" method="GET">
                                        <input type="hidden" name="id" value="${group.usergroup_id}"/>
                                        <input type="submit" value="Edit" />    
                                    </form>
                                </div></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <table class="management-table" id="service-user-table">
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Full Name</th>
                            <th></th>
                        </tr>
                    </thead>
                    <c:forEach var="usr" items="${userlist.rows}">
                        <tr>
                            <td>${usr.username}</td>
                            <td>${usr.first_name} ${usr.last_name}</td>
                            <td>
                                <div class="float-right inline">
                                    <form action="" method="POST">
                                        <input type="hidden" name="user_id" value="${usr.user_id}"/>
                                        <input type="submit" value="Remove" />  
                                    </form>
                                </div>
                                <div class="float-right inline">
                                    <form action="user_edit.jsp" method="GET">
                                        <input type="hidden" name="user_id" value="${usr.user_id}"/>
                                        <input type="hidden" name="return" value="groups"/>
                                        <input type="hidden" name="group_id" value="${usr.usergroup_id}"/>
                                        <input type="submit" value="Edit" />    
                                    </form>
                                </div>                                
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                $("#sidebar").load("/Testing/sidebar.html", function () {
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
                });
                $("#nav").load("/Testing/navbar.html");

            });
        </script>        
    </body>
</html>
