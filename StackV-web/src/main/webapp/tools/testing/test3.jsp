<%-- 
    Document   : ranjitha_test
    Created on : Nov 16, 2015, 2:15:41 PM
    Author     : max
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.security.MessageDigest"%>
<%@page errorPage = "errorPage.jsp" %>

<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />

<jsp:setProperty name="user" property="*" /> 

<c:if test="${user.loggedIn == false}">
    <c:redirect url="login.jsp" />
</c:if>
    
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Overview</title>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/bootstrap.css">
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>
    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <br><br>
            Welcome ${user.firstName} ${user.lastName}.<br>
            
            <c:choose>
                <c:when test="${param.ret != 'sub'}">
                    <div id="service-specific">       
                        <form action="/StackV-web/tools/testing/ranjitha_test.jsp" method="post">
                            <input type="hidden" name="ret" value="sub" />
                            <table class="management-table" id="service-form">                    
                                <thead>
                                    <tr>
                                        <th>Please Select the Security Questions</th>
                                        <th style="text-align: right"></th>                            
                                    </tr>
                                </thead>
                                <tbody>                    
                                    <tr>
                                        <td>1</td>
                                        <td>Which city were you born ?</td>
                                        <td><input type="text" name="city" required/></td>
                                        <td><select name="color">
                                                <option value="Maryland">Maryland</option>
                                                <option value="Virginia">Virginia</option>
                                                <option value="NewYork">NewYork</option>
                                            </select></td>
                                    </tr>
                                    <tr>
                                        <td>2</td>
                                        <td>What is your pet name?</td>
                                        <td><input type="text" name="pet" required/></td>
                                    </tr>
                                    <tr>
                                        <td>3</td>
                                        <td>What is your mothers maiden name ?</td>
                                        <td><input type="text" name="mothername" required/></td>
                                    </tr>    
                                    <tr>
                                        <td></td>
                                        <td><input class="button-register" name="change" type="submit" value="Submit" /></td>
                                    </tr>
                                </tbody>
                            </table>

                        </form>
                    </div>
                </c:when>

            </c:choose>
             
           
                
                
                
                
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                $("#sidebar").load("/StackV-web/sidebar.html", function () {
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
                $("#nav").load("/StackV-web/navbar.html");
            });
        </script>          
    </body>
</html>
