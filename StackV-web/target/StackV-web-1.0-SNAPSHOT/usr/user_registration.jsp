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
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>User Registration</title>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/bootstrap.css">
        <link rel="stylesheet" href="/StackV-web/css/style.css">

        <script type="text/javascript">
            window.onload = function () {
                document.getElementById("password1").onchange = validatePassword;
                document.getElementById("password2").onchange = validatePassword;
            };
            function validatePassword() {
                var pass2 = document.getElementById("password2").value;
                var pass1 = document.getElementById("password1").value;
                if (pass1 !== pass2)
                    document.getElementById("password2").setCustomValidity("Passwords Don't Match");
                else
                    document.getElementById("password2").setCustomValidity('');
                //empty string means no validation error
            }


        </script>

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
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">                        
            <div id="tables">
                <c:choose>                    
                    <c:when test="${param.ret == 'succ'}">
                        <div class="form-result">
                            Registration Successful<br>
                            <a href="/StackV-web/usr/user_registration.jsp">Return</a>
                        </div>
                    </c:when>
                    <c:when test="${param.ret == 'user'}">
                        <div class="form-result">
                            Error - Username Already Exists<br>
                            <a href="/StackV-web/usr/user_registration.jsp">Return</a>
                        </div>
                    </c:when><c:when test="${param.ret == 'fail'}">
                        <div class="form-result">
                            Error - Invalid State, Please Try Again Later<br>
                            <a href="/StackV-web/usr/user_registration.jsp">Return</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div id="user-overview">
                            <sql:query dataSource="${front_conn}" sql="SELECT usergroup_id, title FROM usergroup" var="ugrouplist" />
                            <form action="register.jsp" method="POST" name="register-user">
                                <table class="management-table" id="registration-table">                    
                                    <thead>
                                        <tr>
                                            <th>Registration</th>
                                        </tr>
                                    </thead>
                                    <tbody>                    
                                        <tr><td><input placeholder='Username' type='text' name="username" size="40" maxlength="20" required /></td></tr>
                                        <tr><td><input placeholder='Password' name="password" id="password1" type='password' size="40" maxlength="20" required /></td></tr>
                                        <tr><td><input placeholder='Confirm Password' type='password' id="password2" size="40" maxlength="20" required /></td></tr>
                                        <tr><td><input placeholder='First Name' type='text' name="firstname" size="40" maxlength="20" required /></td></tr>
                                        <tr><td><input placeholder='Last Name' type='text' name="lastname" size="40" maxlength="20" required /></td></tr>
                                        <tr><td><div class="select-text">Usergroup: </div><select name="usergroup" required>
                                                    <c:forEach var="ugroup" items="${ugrouplist.rows}">
                                                        <option value="<c:out value="${ugroup.usergroup_id}" />"><c:out value="${ugroup.title}" /></option>
                                                    </c:forEach>
                                                </select></td></tr>
                                        <tr><td><input placeholder='Email Address' name='email' type='email' size="40" /></td></tr>                   
                                        <tr><td><input class="button-register" name="registration" type="submit" value="Register" /></td></tr>
                                    </tbody>
                                </table>
                            </form>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>              
    </body>
</html>
