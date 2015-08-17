<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
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
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
    </head>

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
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
                                <form id="view-form" action="/VersaStack-web/ViewServlet" method="post">
                                    <!-- Management Form -->
                                    <c:if test="${param.mode != 'create'}">
                                        Manage
                                    </c:if>
                                    <!-- Creation Form -->
                                    <c:if test="${param.mode == 'create'}">
                                        <table class="management-table">
                                            <thead>
                                                <tr>
                                                    <th>Query</th>
                                                    <th>
                                                        <span style="color:black">
                                                            <input type="text" id="sparquery" name="sparquery" size="70" />
                                                        </span>
                                                    </th>
                                                </tr>

                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>Flags</td>
                                                    <td>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="inc" name="viewInclusive"/>
                                                            <label for="inc">Inclusive</label>
                                                        </div>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="sub" name="subRecursive"/>
                                                            <label for="sub">Subtree Recursive</label>
                                                        </div>
                                                        <div class="view-flag">
                                                            <input type="checkbox" id="sup" name="supRecursive"/>
                                                            <label for="sup">Supertree Recursive</label>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td><strong>Templates</strong></td>
                                                    <td></td>
                                                </tr>
                                                <tr>
                                                    <td>I like</td>
                                                    <td>
                                                        <input type="hidden" id="likeTemp" value="I like "/>
                                                        <input type="text" class="spar-template" id="likeInput"/>
                                                        <button type="button" class="button-service-apply" onClick="applyTextTemplate('like')">Apply</button>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>I hate</td>
                                                    <td>
                                                        <input type="hidden" id="hateTemp" value="I hate "/>
                                                        <input type="text" class="spar-template" id="hateInput"/>
                                                        <button type="button" class="button-service-apply" onClick="applyTextTemplate('hate')">Apply</button>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>I'm neutral about</td>
                                                    <td>
                                                        <input type="hidden" id="neutralTemp" value="I'm neutral about "/>
                                                        <select id="neutralInput">
                                                            <option value="birds">Birds</option>
                                                            <option value="cats">Cats</option>
                                                            <option value="rocks">Random Rocks</option>
                                                        </select>
                                                        <button type="button" class="button-service-apply" onClick="applySelTemplate('neutral')">Apply</button>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td></td>
                                                    <td>
                                                        <input class="button-register" name="create" type="submit" value="Submit" />
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
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

                            <br><a href="/VersaStack-web/ops/srvc/driver.jsp?self=true">(Un)Install Another Driver.</a>                                
                            <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                            <br><a href="/VersaStack-web/orch/graphTest.html">Return to Graphic Orchestration.</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>       
        </div>
        <!-- JS -->
        <script>
            $(function () {
                $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
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
            });
        </script>        
    </body>
</html>
