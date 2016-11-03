<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/vxstack-web/errorPage.jsp" %>
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
        <title>Usergroup Management</title>
        <script src="/vxstack-web/js/jquery/jquery.js"></script>
        <script src="/vxstack-web/js/bootstrap.js"></script>
        <script src="/vxstack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/vxstack-web/css/animate.min.css">
        <link rel="stylesheet" href="/vxstack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/vxstack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/vxstack-web/css/style.css">
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
            <c:if test="${not empty param.add_user_id}">              
                <sql:update dataSource="${front_conn}" sql="INSERT INTO user_belongs (`user_id`, `usergroup_id`) VALUES (?, ?)" var="count">
                    <sql:param value="${param.add_user_id}" />
                    <sql:param value="${param.id}" />
                </sql:update>                               
            </c:if>
            <c:if test="${not empty param.remove_user_id}">              
                <sql:update dataSource="${front_conn}" sql="DELETE FROM user_belongs WHERE user_id = ? AND usergroup_id = ?" var="count">
                    <sql:param value="${param.remove_user_id}" />
                    <sql:param value="${param.id}" />
                </sql:update>                              
            </c:if>

            <div id="tables">
                <sql:query dataSource="${front_conn}" sql="SELECT G.title, G.usergroup_id, COUNT(B.user_id) user_count FROM usergroup G 
                           JOIN user_belongs B WHERE B.usergroup_id = G.usergroup_id GROUP BY G.title" var="ugrouplist" />
                <div id="group-overview">
                    <table class="management-table" id="group-overview-table">                    
                        <thead>
                            <tr>
                                <th>Usergroup</th>
                                <th># of Members</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>                    
                            <c:forEach var="row" items="${ugrouplist.rows}">
                                <tr>
                                    <td>${row.title}</td>
                                    <td>${row.user_count}</td>
                                    <td class="text-right">
                                        <jsp:element name="button">
                                            <jsp:attribute name="class">button-group-select</jsp:attribute>
                                            <jsp:attribute name="id">select${row.usergroup_id}</jsp:attribute>                                           
                                            <jsp:body>Select</jsp:body>
                                        </jsp:element>
                                    </td>
                                </tr>                              
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <br>
                <sql:query dataSource="${front_conn}" sql="SELECT DISTINCT I.user_id, I.username, I.first_name, I.last_name, I.email, G.title 
                           FROM user_info I, user_belongs B, usergroup G
                           WHERE B.usergroup_id = ? AND B.user_id = I.user_id AND G.usergroup_id = ?" var="ulist">
                    <sql:param value="${param.id}" />
                    <sql:param value="${param.id}" />
                </sql:query>
                <div id="group-specific">
                    <table class="management-table">                    
                        <thead>
                            <tr>
                                <th>Usergroup</th>
                                <th>Username</th>
                                <th>Name</th>
                                <th>Email</th>
                            </tr>
                        </thead>

                        <tbody>                        
                            <c:forEach var="row" items="${ulist.rows}">
                                <tr>
                                    <td>${row.title}</td>
                                    <td>${row.username}</td>
                                    <td>${row.first_name} ${row.last_name}</td>
                                    <td>${row.email}
                                        <div class="float-right inline">
                                            <form action="user_groups.jsp?id=${param.id}" method="POST">
                                                <input type="hidden" name="remove_user_id" value="${row.user_id}"/>
                                                <input type="submit" value="Remove" />  
                                            </form>
                                        </div>
                                        <div class="float-right inline">
                                            <form action="user_edit.jsp" method="GET">
                                                <input type="hidden" name="user_id" value="${row.user_id}"/>
                                                <input type="hidden" name="return" value="groups"/>
                                                <input type="hidden" name="group_id" value="${param.id}"/>
                                                <input type="submit" value="Edit" />    
                                            </form>
                                        </div>
                                    </td>
                                </tr>                              
                            </c:forEach>
                        </tbody>
                    </table>                        
                    <c:if test="${param.id != '0'}">    
                        <form id="button-add-users" action="user_groups.jsp?id=${param.id}" name="add-user" method="POST">
                            <sql:query dataSource="${front_conn}" sql="SELECT I.user_id, I.username, I.first_name, I.last_name FROM user_info I 
                                       WHERE I.user_id NOT IN (SELECT user_id FROM user_belongs WHERE usergroup_id = ?)" var="users">
                                <sql:param value="${param.id}" />
                            </sql:query>

                            <select name="add_user_id">
                                <c:forEach var="usr" items="${users.rows}">
                                    <option value="${usr.user_id}">${usr.username} (${usr.first_name} ${usr.last_name})</option>
                                </c:forEach>
                            </select>
                            <input type="submit" value="Add User to Group" />
                        </form>                        
                    </c:if>
                </div>
            </div>
        </div>              
    </body>
</html>
