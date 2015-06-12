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
        <title>Usergroup Management</title>
        <script src="/Testing/js/jquery/jquery.js"></script>
        <script src="/Testing/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/Testing/css/animate.min.css">
        <link rel="stylesheet" href="/Testing/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/Testing/css/bootstrap.css">
        <link rel="stylesheet" href="/Testing/css/style.css">
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
            <c:if test="${not empty param.add_user_id}">              
                <sql:update dataSource="${front_conn}" sql="UPDATE user_info SET usergroup_id = ? WHERE user_id = ?" var="count">
                    <sql:param value="${param.id}" />
                    <sql:param value="${param.add_user_id}" />
                </sql:update>                               
            </c:if>

            <div id="tables">
                <sql:query dataSource="${front_conn}" sql="SELECT G.title, G.usergroup_id, COUNT(I.user_id) user_count FROM usergroup G 
                           JOIN user_info I WHERE I.usergroup_id = G.usergroup_id GROUP BY G.title" var="ugrouplist" />
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
                <sql:query dataSource="${front_conn}" sql="SELECT DISTINCT user_id, username, first_name, last_name, email, G.title FROM user_info I
                           JOIN usergroup G WHERE I.usergroup_id = ? AND G.usergroup_id = ?" var="ulist">
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
                                        <div class="float-right">
                                            <form action="user_edit.jsp" method="GET">
                                                <jsp:element name="input">
                                                    <jsp:attribute name="type">hidden</jsp:attribute>
                                                    <jsp:attribute name="name">user_id</jsp:attribute>
                                                    <jsp:attribute name="value">${row.user_id}</jsp:attribute>
                                                </jsp:element>
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
                            <sql:query dataSource="${front_conn}" sql="SELECT DISTINCT user_id, first_name, last_name, username FROM user_info I WHERE I.usergroup_id <> ?" var="users">
                                <sql:param value="${param.id}" />
                            </sql:query>

                            <select name="add_user_id">
                                <c:forEach var="user" items="${users.rows}">
                                    <option value="${user.user_id}">${user.username} (${user.first_name} ${user.last_name})</option>
                                </c:forEach>
                            </select>
                            <input type="submit" value="Switch User to Current Group" />
                        </form>                        
                    </c:if>
                </div>
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
            
                $(".button-group-select").click(function (evt) {
                    $ref = "user_groups.jsp?id=" + this.id;
                    $ref = $ref.replace('select', '') + " #group-specific";
                    // console.log($ref);
                    $("#group-specific").load($ref);
                    evt.preventDefault();
                });
            });
        </script>        
    </body>
</html>
