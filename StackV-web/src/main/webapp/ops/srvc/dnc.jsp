<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<!DOCTYPE html>
<html >
    <head>
        <meta charset="UTF-8">
        <title>Dynamic Network Connection Service</title>
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <c:choose>
                <c:when test="${empty param.ret}">  <!-- Display this section when no return value supplied -->
                    <div id="service-specific">
                        <div id="service-top">
                            <div id="service-title">
                                Dynamic Network Connection
                            </div>
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">

                                </table>
                            </div>
                        </div>
                        <div id="service-bottom">
                            <div id="service-fields">
                                <table class="management-table" id="service-form" style="margin-bottom: 0px;">
                                    <thead>
                                        <tr>
                                            <th>Templates</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td>1 Link</td>
                                            <td><button onclick="applyDNCTemplate(1)">Apply</button></td>
                                        </tr>
                                        <tr>
                                            <td>2 Links</td>
                                            <td><button onclick="applyDNCTemplate(2)">Apply</button></td>
                                        </tr>
                                    </tbody>
                                </table>
                                <form id="custom-form" action="/StackV-web/ServiceServlet" method="post">
                                    <input type="hidden" name="username" value="${user.getUsername()}"/>
                                    <table class="management-table" id="net-custom-form">
                                        <thead>
                                            <tr>
                                                <th>Service Alias</th>
                                                <th>
                                                    <input class="header-input" type="text" name="alias" required />
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody id="custom-fields">
                                            <tr id="link1">
                                                <td>Link 1</td>
                                                <td>
                                                    <div>
                                                        <input type="text" name="linkUri1" size="60" placeholder="Link-URI">
                                                    </div>
                                                    <div>
                                                        <input type="text" name="link1-src" size="60" placeholder="Source">
                                                        <input type="text" name="link1-src-vlan" placeholder="Vlan-tag">
                                                    </div>
                                                    <div>
                                                        <input type="text" name="link1-des" size="60" placeholder="Destination">
                                                        <input type="text" name="link1-des-vlan" placeholder="Vlan-tag">
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td></td>
                                                <td><input type="submit" name="custom" value="Submit" /><input class="button-register" type="button" value="Add Link" onclick="addLink()"></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <input type="hidden" name="dncCreate" value="true">
                                </form>
                            </div>
                        </div>
                    </div>
                </c:when>

                <c:otherwise>                       <!-- Display this section when return value supplied -->
                    <div class="form-result" id="service-result">
                        <c:choose>
                            <c:when test="${param.ret == '0'}">
                                Installation Success!
                            </c:when>
                            <c:when test="${param.ret == '1'}">
                                Error 1.
                            </c:when>
                            <c:when test="${param.ret == '2'}">
                                Error 2.
                            </c:when>
                            <c:when test="${param.ret == '3'}">
                                Error 3.
                            </c:when>
                        </c:choose>

                        <br><a href="/StackV-web/ops/srvc/template.jsp?self=true">Repeat.</a>
                        <br><a href="/StackV-web/ops/catalog.jsp">Return to Services.</a>
                        <br><a href="/StackV-web/orch/graphTest.jsp">Return to Graphic Orchestration.</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
    </body>
</html>
