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
            <form style="width: 800px;" class="dncform">
                <fieldset class="active-fs" id="0-template-select" style="z-index: 4;">
                    <div>
                        <table>
                            <tbody>
                                <tr id="link_1">
                                    <td>
                                        <p>Link 1</p>
                                    </td>
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
                            </tbody>
                        </table>
                        <div>
                            <button type="button" class="action-button" onclick="">Submit</button>
                            <button type="button" class="action-button" onclick="">Save</button>
                            <button type="button" class="action-button" onclick="">Add Link</button>
                        </div>
                            
                    </div>
                        
                </fieldset>
            </form>
        </div>
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <script src ="/StackV-web/js/svc/dnc.js"></script>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
    </body>
</html>
