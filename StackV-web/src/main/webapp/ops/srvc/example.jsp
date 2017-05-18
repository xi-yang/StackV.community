 <%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Example></title>

        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <c:choose>
                <c:when test="${param.ret != 'sub'}">
                    <div id="service-specific">
                        <form action="/StackV-web/ops/srvc/example.jsp" method="post">
                            <input type="hidden" name="ret" value="sub" />
                            <table class="management-table" id="service-form">
                                <thead>
                                    <tr>
                                        <th>Form Header</th>
                                        <th style="text-align: right"></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>How many times do you want to print the user's name?</td>
                                        <td><input type="number" name="count" required/></td>
                                    </tr>
                                    <tr>
                                        <td>What color should it be?</td>
                                        <td><select name="color">
                                                <option value="red">Red</option>
                                                <option value="green">Green</option>
                                                <option value="blue">Blue</option>
                                            </select></td>
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
                        <c:forEach begin="1" end="${param.count}" varStatus="loop">
                            <p style="color: ${param.color}">${user.getFirstName()} ${user.getLastName()}</p>
                        </c:forEach>
                        <br><br><a href="/StackV-web/ops/catalog.jsp">Return to Services.</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <!-- JS -->
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
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
                $("#tag-panel").load("/StackV-web/tagPanel.jsp", null);
            });
        </script>
    </body>
</html>
