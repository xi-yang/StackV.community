<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html lang="en">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta charset="utf-8">
        <title>Users Management View</title>
        <meta name="viewport" content="initial-scale=1">
        <link href="css/navigation.css" rel="stylesheet">
        <link href="css/styles.css" rel="stylesheet">

        <style>
            .floatTL {
                position: fixed;
                top: 0px;
                left: 0px;
            }
        </style>
    </head>
    <body>
        <div class="floatTL" style="float: left">
            <ul>
                <li><a href="overview.html">Overview</a></li>
                <li><a href="display.html">Networks</a></li>
                <li><a href="users">Users</a></li>
                <li><a href="settings.html">Settings</a></li>
                <li><a href="logout.jsp">Logout</a></li>
            </ul> 
        </div>

        <div style="margin-left: 150px">
            <table border="1" style="width:60%">
                <tr>
                    <th>ID</th>
                    <th>Email</th>
                    <th>Salt</th>
                    <th>Language</th>
                    <th>Last login</th>
                </tr>
                <c:forEach var="user" items="${userList}">
                    <tr>
                        <td><c:out value="${user.getID()}" /></td>
                        <td><c:out value="${user.getEmail()}" /></td>
                        <td><c:out value="${user.getSalt()}" /></td>
                        <td><c:out value="${user.getLanguage()}" /></td>
                        <td><c:out value="${user.getLastLogin()}" /><br /></td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </body>
</html>