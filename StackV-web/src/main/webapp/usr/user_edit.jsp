<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<c:if test="${empty param.user_id}">
    <c:redirect url="user_edit.jsp?user_id=${user.id}" />
</c:if>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>User Details</title>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/bootstrap.css">
        <link rel="stylesheet" href="/StackV-web/css/style.css">

        <script src='http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js'></script>
        <script src="/StackV-web/js/index.js"></script>

    </head>

    <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/frontend"
                       user="front_view"  password="frontuser"/>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <div id="tables">
                <div id="user-overview">
                    <c:choose>
                        <c:when test="${user.isAllowed(1) == true || user.id == param.user_id}">
                            <sql:query dataSource="${front_conn}" sql="SELECT first_name, last_name, username, email, active_usergroup FROM user_info I WHERE I.user_id = ?" var="users">
                                <sql:param value="${param.user_id}" />
                            </sql:query>
                            <c:forEach var="edit_user" items="${users.rows}">
                                <form action="update.jsp" method="POST" name="update-user">
                                    <input type="hidden" name="return" value="${param.return}" />
                                    <input type="hidden" name="group_id" value="${param.group_id}" />
                                    <input type="hidden" name="username" value="${edit_user.username}" />
                                    <table class="management-table" id="edit-table">
                                        <thead>
                                            <tr>
                                                <th></th>
                                                <th>Editing</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><b>Username</b></td>
                                                <td>${edit_user.username}</td>
                                            </tr>
                                            <tr>
                                                <td><b>Email</b></td>
                                                <td><input placeholder='${edit_user.email}' name='email' type='email' size="40" /></td>
                                            </tr>
                                            <tr>
                                                <td><b>First Name</b></td>
                                                <td><input placeholder='${edit_user.first_name}' type='text' name="firstname" size="40" maxlength="20" /></td>
                                            </tr>
                                            <tr>
                                                <td><b>Last Name</b></td>
                                                <td><input placeholder='${edit_user.last_name}' type='text' name="lastname" size="40" maxlength="20" /></td>
                                            </tr>
                                            <tr>
                                                <td><b>Password</b></td>
                                                <td><input placeholder='********' name="password" id="password1" type='password' size="40" maxlength="20" /></td>
                                            </tr>
                                            <tr>
                                                <td><b>Confirm Password</b></td>
                                                <td><input placeholder='********' type='password' id="password2" size="40" maxlength="20" /></td>
                                            </tr>
                                            <tr>
                                                <sql:query dataSource="${front_conn}" sql="SELECT G.usergroup_id, G.title FROM usergroup G, user_belongs B
                                                           WHERE G.usergroup_id = B.usergroup_id AND B.user_id = ?" var="ugroups">
                                                    <sql:param value="${param.user_id}" />
                                                </sql:query>
                                                <td><b>Active Usergroup</b></td>
                                                <td>
                                                    <select name="activegroup">
                                                        <c:forEach var="ugroup" items="${ugroups.rows}">
                                                            <c:choose>
                                                                <c:when test="${ugroup.usergroup_id == edit_user.active_usergroup}">
                                                                    <option value="${ugroup.usergroup_id}" selected>${ugroup.title}</option>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <option value="${ugroup.usergroup_id}">${ugroup.title}</option>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:forEach>
                                                    </select>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td></td>
                                                <td><input class="button-register" name="change" type="submit" value="Submit Changes" /></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </form>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="form-result">Operation Not Allowed.<br>
                                <a href="/StackV-web/index.jsp">Return to Home.</a></div>
                            </c:otherwise>
                        </c:choose>
                </div>
            </div>
        </div>
        <!-- JS -->
    </body>
</html>
