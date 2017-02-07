<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Dynamic Network Connection Service</title>
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/dnc.css">
    </head>
    
    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <form class="dncform">
                <fieldset class="active-fs" id="0-template-select" style="z-index: 4;">
                    <div>
                        Dynamic Network Connection:
                        <input type="text" placeholder="Service Name" id="service-name">
                    </div>                    
                    <div>
                        Description:
                        <input type="text" placeholder="Description" id="new-profile-description">
                    </div>
                    <div id="spacer"></div>
                    <div id="spacer"></div>
                    <div id="table-div">
                        <table id="input-table">
                            <tbody id="link-body">
                                <tr>
                                    <td>Link 1</td>
                                    <td style="width: 600px; text-align: center;">
                                        <div>
                                            <input type="text" id="linkUri1" placeholder="Link-URI">
                                            <input type="text" id="linksrc1" placeholder="Source">
                                            <input type="text" id="linksrc-vlan1" placeholder="Vlan-tag">
                                            <input type="text" id="linkdes1" placeholder="Destination">
                                            <input type="text" id="linkdes-vlan1" placeholder="Vlan-tag">
                                        </div>
                                    </td>
                                </tr>
                                <tr id="spacer"></tr>
                            </tbody>
                        </table>
                        <div>
                            <button type="button" class="action-button" onclick="submit();">Submit</button>
                            <button type="button" class="action-button" onclick="save();">Save</button>
                            <button type="button" class="action-button" onclick="addLink();">Add Link</button>
                        </div>
                    </div>
                    
                </fieldset>
            </form>
        </div>
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/svc/dnc.js"></script>
    </body>
</html>
