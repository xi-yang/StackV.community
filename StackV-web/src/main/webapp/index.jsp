<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<c:redirect url="/ops/catalog.jsp" />
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Login</title>

        <!-- <link rel="stylesheet" href="css/reset.css"> -->
        <link rel="stylesheet" href="css/animate.min.css">
        <link rel="stylesheet" href="css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="css/style.css">

    </head>

    <body>
        <div class="form login-form">
            <!-- Kept for code reference
            <div class='switch'>
                <i class='fa fa-pencil fa-times'></i>
                <div class='tooltip'>Register</div>
            </div>
            -->
            <div class='login'>
                <c:choose>
                    <c:when test="${param.ret == 'auth'}">
                        <div class="error">
                            There was an error during login.<br>
                            Incorrect username and/or password.<br>
                        </div>
                    </c:when>                        
                    <c:when test="${param.ret == 'user'}">
                        <div class="error">
                            There was an error during registration.<br>
                            Username already exists.<br>
                        </div>
                    </c:when>
                    <c:when test="${param.ret == 'fail'}">
                        <div class="error">
                            There was an error during registration.<br>
                            Please try again later.<br>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <h2>VS Frontend</h2>
                    </c:otherwise>
                </c:choose>
                <form name="login" action="login.jsp" method="post"><br/>
                    <input placeholder='Username' type='text' name="username" maxlength="20">
                    <input placeholder='Password' type='password' name="password" maxlength="20">
                    <input id='button-login' type ="submit" value="Login" />
                </form>
            </div>
            <!-- Kept for code reference
            <div class='register'>
                <h2>Register a New Account.</h2>
                <div class='alert'>
                    
                </div>                                 
            </div>
            -->
        </div>    

        <script src='http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js'></script>
        <script src="js/index.js"></script>

    </body>
</html>
