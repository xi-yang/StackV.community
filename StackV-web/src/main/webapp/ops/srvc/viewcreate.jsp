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
<html >
    <head>
        <meta charset="UTF-8">
        <title>View Creation Service</title>
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
                <c:when test="${empty param.ret}">
                    <div id="service-specific">
                        <div id="service-top">
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">
                                    <thead>
                                        <tr>
                                            <th>Mode</th>
                                            <th>
                                                <select name="mode" onchange="viewmodeSelect(this)">
                                                    <option value="manage">Manage</option>
                                                    <c:choose>
                                                        <c:when test="${param.mode == 'create'}">
                                                            <option value="create" selected>Create</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="create">Create</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </select>
                                            </th>
                                        </tr>
                                    </thead>
                                </table>
                            </div>
                        </div>
                        <div id="service-bottom">
                            <div id="service-fields">
                                <form id="view-form" action="/StackV-web/ViewServlet" method="post">
                                    <!-- Management Form -->
                                    <c:if test="${param.mode != 'create'}">
                                        <table class="management-table" id="manage-table">
                                            <thead><tr><th></th><th></th></tr></thead>
                                            <tbody>
                                                <tr>
                                                    <td>Select Filter</td>
                                                    <td>
                                                        <c:if test="${not empty user.modelNames}">
                                                            <select id="model-select" name="modelName" size="5" required>
                                                                <c:forEach var="model" items="${user.modelNames}">
                                                                    <c:if test="${model != 'base'}">
                                                                        <option value="${model}">${model}</option>
                                                                    </c:if>
                                                                </c:forEach>
                                                            </select>
                                                        </c:if>
                                                        <c:if test="${empty user.modelNames}">
                                                            No Filters Present
                                                        </c:if>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <c:if test="${not empty user.modelNames}"><input class="button-register" name="uninstall" type="submit" value="Uninstall" /></c:if>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                    </c:if>
                                    <!-- Creation Form -->
                                    <c:if test="${param.mode == 'create'}">
                                        <!-- View Creation Table -->

                                        <table class="management-table" id="query-table">
                                            <thead>
                                                <tr>
                                                    <th>View Name</th>
                                                    <th>
                                                        <span style="color:black">
                                                            <input type="text" name="viewName" size="30" required />
                                                        </span>
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>Queries</td>
                                                    <td></td>
                                                </tr>
                                                <tr>
                                                    <td><input type="text" id="sparquery1" name="sparquery1" size="70" /></td>
                                                    <td>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="inc1" name="viewInclusive1"/>
                                                            <label for="inc1">Inclusive</label>
                                                        </div>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="sub1" name="subRecursive1"/>
                                                            <label for="sub1">Subtree Rec.</label>
                                                        </div>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="sup1" name="supRecursive1"/>
                                                            <label for="sup1">Supertree Rec.</label>
                                                        </div>
                                                        <div>
                                                            <input type="button" id="wizard-1" value="Wizard" onClick="openWizard(this)" />
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <input class="button-register" name="create" type="submit" value="Submit" />
                                                        <input class="button-register" type="button" value="Add Query" onClick="addQuery()">
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>

                                        <!-- Old Query Creation Table
                                        <table class="management-table hide" class="hide" id="wizard-table">
                                            <thead>
                                                <tr>
                                                    <th>Query Wizard</th>
                                                    <th><input type="hidden" id="queryNumber" value=""/></th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>I like</td>
                                                    <td>
                                                        <input type="hidden" id="likeTemplate" value="I like "/>
                                                        <input type="text" class="spar-template" id="likeInput"/>
                                                        <button type="button" class="button-service-apply" onClick="applyTextTemplate('like')">Apply</button>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>I hate</td>
                                                    <td>
                                                        <input type="hidden" id="hateTemplate" value="I hate "/>
                                                        <input type="text" class="spar-template" id="hateInput"/>
                                                        <button type="button" class="button-service-apply" onClick="applyTextTemplate('hate')">Apply</button>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>I'm neutral about</td>
                                                    <td>
                                                        <input type="hidden" id="neutralTemplate" value="I'm neutral about "/>
                                                        <select id="neutralInput">
                                                            <option value="birds">Birds</option>
                                                            <option value="cats">Cats</option>
                                                            <option value="rocks">Random Rocks</option>
                                                        </select>
                                                        <button type="button" class="button-service-apply" onClick="applySelTemplate('neutral')">Apply</button>
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>

                                        -->
                                    </c:if>
                                </form>
                            </div>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="form-result" id="service-result">
                            <c:choose>
                                <c:when test="${param.ret == '0'}">
                                    Success
                                </c:when>
                                <c:when test="${param.ret == '1'}">
                                    Invalid Driver ID
                                </c:when>
                                <c:when test="${param.ret == '2'}">
                                    Error (Un)Installing Driver
                                </c:when>
                                <c:when test="${param.ret == '3'}">
                                    Connection Error
                                </c:when>
                            </c:choose>

                            <br><a href="/StackV-web/ops/srvc/viewcreate.jsp?self=true">Return to Views.</a>
                            <br><a href="/StackV-web/ops/catalog.jsp">Return to Services.</a>
                            <br><a href="/StackV-web/orch/graphTest.jsp">Return to Graphic Orchestration.</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <!-- JS -->
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
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
