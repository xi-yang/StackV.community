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
        <title>ACL Management</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">     
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
            <sql:query dataSource="${front_conn}" sql="SELECT acl_id FROM acl WHERE service_id = ?" var="acllist">
                <sql:param value="${param.id}"/>
            </sql:query>
            <c:if test="${not empty param.add_usergroup_id}">            
                <c:forEach var="acl" items="${acllist.rows}">
                    <sql:update dataSource="${front_conn}" sql="INSERT INTO acl_entry_group VALUES (?, ?)" var="count">
                        <sql:param value="${acl.acl_id}" />
                        <sql:param value="${param.add_usergroup_id}" />
                    </sql:update>
                </c:forEach>
            </c:if>
            <c:if test="${not empty param.remove_usergroup_id}">
                <c:forEach var="acl" items="${acllist.rows}">
                    <sql:update dataSource="${front_conn}" sql="DELETE FROM acl_entry_group WHERE acl_id = ? AND usergroup_id = ?" var="count">
                        <sql:param value="${acl.acl_id}" />
                        <sql:param value="${param.remove_usergroup_id}" />
                    </sql:update>
                </c:forEach>
            </c:if>
            <c:if test="${not empty param.add_user_id}">            
                <c:forEach var="acl" items="${acllist.rows}">
                    <sql:update dataSource="${front_conn}" sql="INSERT INTO acl_entry_user VALUES (?, ?)" var="count">
                        <sql:param value="${acl.acl_id}" />
                        <sql:param value="${param.add_user_id}" />
                    </sql:update>
                </c:forEach>
            </c:if>
            <c:if test="${not empty param.remove_user_id}">
                <c:forEach var="acl" items="${acllist.rows}">
                    <sql:update dataSource="${front_conn}" sql="DELETE FROM acl_entry_user WHERE acl_id = ? AND user_id = ?" var="count">
                        <sql:param value="${acl.acl_id}" />
                        <sql:param value="${param.remove_user_id}" />
                    </sql:update>
                </c:forEach>
            </c:if>

            <form>
                <sql:query dataSource="${front_conn}" sql="SELECT name, service_id FROM service" var="servicelist" />
                <select id="acl-select" onchange="aclSelect(this)">
                    <option value=""></option>
                    <c:forEach var="service" items="${servicelist.rows}">
                        <c:choose>
                            <c:when test="${param.id == service.service_id}">
                                <option value="${service.service_id}" selected>${service.name}</option>
                            </c:when>
                            <c:otherwise>
                                <option value="${service.service_id}">${service.name}</option>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </select>
            </form>
            <div id="acl-tables">
                <sql:query dataSource="${front_conn}" sql="SELECT G.usergroup_id, G.title, COUNT(I.user_id) ucount 
                           FROM usergroup G, user_info I, acl A, acl_entry_group E 
                           WHERE G.usergroup_id = E.usergroup_id AND G.usergroup_id = I.active_usergroup AND E.acl_id = A.acl_id AND A.service_id = ? 
                           GROUP BY G.title" var="ugrouplist">
                    <sql:param value="${param.id}"/>
                </sql:query>

                <sql:query dataSource="${front_conn}" sql="SELECT I.user_id, I.username, I.first_name, I.last_name, I.active_usergroup, G.title 
                           FROM user_info I, acl A, acl_entry_user U, usergroup G 
                           WHERE I.user_id = U.user_id AND U.acl_id = A.acl_id AND I.active_usergroup = G.usergroup_id AND A.service_id = ?" var="userlist">
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
                                        <form action="privileges.jsp?id=${param.id}" method="POST">
                                            <input type="hidden" name="remove_usergroup_id" value="${group.usergroup_id}"/>
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
                <c:if test="${not empty param.id}">
                    <form id="button-add-groups" action="privileges.jsp?id=${param.id}" method="post">                    
                        <sql:query dataSource="${front_conn}" sql="SELECT G.usergroup_id, G.title 
                                   FROM usergroup G WHERE G.usergroup_id NOT IN 
                                   (SELECT G.usergroup_id FROM usergroup G, acl A, acl_entry_group E 
                                   WHERE G.usergroup_id = E.usergroup_id AND E.acl_id = A.acl_id AND A.service_id = ?)
                                   GROUP BY G.title" var="ugrouplist">
                            <sql:param value="${param.id}"/>
                        </sql:query>
                        <select name="add_usergroup_id">
                            <c:forEach var="group" items="${ugrouplist.rows}">
                                <option value="${group.usergroup_id}">${group.title}</option>
                            </c:forEach>
                        </select>
                        <input type="submit" value="Add" /> 
                    </form>
                </c:if>
                <table class="management-table" id="service-user-table">
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Full Name</th>
                            <th>Usergroup</th>
                            <th></th>
                        </tr>
                    </thead>
                    <c:forEach var="usr" items="${userlist.rows}">
                        <tr>
                            <td>${usr.username}</td>
                            <td>${usr.first_name} ${usr.last_name}</td>
                            <td>${usr.title}</td>
                            <td>
                                <div class="float-right inline">
                                    <form action="privileges.jsp?id=${param.id}" method="POST">
                                        <input type="hidden" name="remove_user_id" value="${usr.user_id}"/>
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
                <c:if test="${not empty param.id}">
                    <form id="button-add-users" action="privileges.jsp?id=${param.id}" method="post">                    
                        <sql:query dataSource="${front_conn}" sql="SELECT I.user_id, I.username, I.first_name, I.last_name, G.title
                                   FROM user_info I, usergroup G WHERE I.user_id NOT IN (SELECT I.user_id FROM user_info I, acl A, acl_entry_user U 
                                   WHERE I.user_id = U.user_id AND U.acl_id = A.acl_id AND A.service_id = ?) AND I.active_usergroup = G.usergroup_id" var="users">
                            <sql:param value="${param.id}" />
                        </sql:query>

                        <select name="add_user_id">
                            <c:forEach var="usr" items="${users.rows}">
                                <option value="${usr.user_id}">${usr.username} (${usr.first_name} ${usr.last_name}) [${usr.title}]</option>
                            </c:forEach>
                        </select>
                        <input type="submit" value="Add" /> 
                    </form>
                </c:if>
            </div>
        </div>        
    </body>
</html>
