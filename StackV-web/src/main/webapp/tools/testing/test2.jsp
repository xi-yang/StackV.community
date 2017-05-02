<%-- 
    Document   : test2
    Created on : Nov 19, 2015, 12:58:06 PM
    Author     : max
--%>


<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.security.MessageDigest"%>
<%@page errorPage = "errorPage.jsp" %>

<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />

<jsp:setProperty name="user" property="*" /> 


<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  



<c:if test="${user.loggedIn == false}">
    <c:redirect url="/login.jsp" />
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
                <c:when test ="${param.ret1 != 'sub1'}">
                    <div id="service-specific">
                    <form action ="/StackV-web/tools/testing/test2.jsp" method ="post">
                        <input type="hidden" name="ret1" value ="sub1"/>
                        <table class ="management-table" id ="service-form">
                            <thead>
                                <tr>
                                    <th>Please select the number of Security Questions</th>
                                    <th style="text-align: right"></th>>
                                </tr>    
                            </thead>
                            <tbody>
                                <tr>
                                    <td><input type ="radio" name="number" value="2" > 2
                                        <br>
                                        <input type ="radio" name="number" value="3"> 3
                                    </td>
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
                <c:otherwise>
                    <div id="service-result">
                        
                        <form action ="/StackV-web/home.jsp" method ="post">
                            <table>
                                <tbody>
                                    <c:forEach begin="1" end="${param.number}" varStatus="loop">
                                    <tr>
                                        <td><select name="questions">
                                                <option value="q1">Which city were you born ?</option>
                                                <option value="q2">What is your pet name?</option>
                                                <option value="q3">What is your mothers maiden name ?</option>
                                            </select>
                                        </td>
                                        <td><input type="text" name="ans" required/>
                                        </td>
                                    </tr>
                                    </c:forEach> 
                                    <tr>
                                        <td></td>
                                        <td><input class="button-register" name="change" type="submit" value="Submit" /></td>
                                    </tr>
                                </tbody>
                            </table>    
                        </form>    
                        
                    </div>
                </c:otherwise>
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
                $("#nav").load("/StackV-web/nav/navbar.html");
            });
        </script>          
    </body>
</html>

