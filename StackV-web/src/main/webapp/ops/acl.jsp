<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>ACL Management</title>

        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.structure.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.theme.css">
    </head>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <div class="left-tab"></div>
            <div class="right-tab"></div>
            <div class="acl-panel" id="acl-role-panel">
                <div class="acl-role-user-div">
                    <table class="management-table" id="acl-user-table">
                        <thead>
                            <tr>
                                <th>Username<button type="button" id="acl-role-exit" class="close" aria-hidden="true">&times;</button></th>
                            </tr>
                        </thead>
                        <tbody id="user-body">
                        </tbody>
                    </table>
                </div>
                <div class="acl-role-role-div">
                    <table class="management-table" id="acl-role-table">
                        <thead>
                            <tr>
                                <th>Roles<button type="button" id="acl-user-exit" class="close" aria-hidden="true">&times;</button></th>
                            </tr>
                        </thead>
                        <tbody id="roles-body">
                        </tbody>
                    </table>
                </div>


            </div>
            <div class="acl-panel active" id="acl-instance-panel">
                <table class="management-table" id="acl-instance-table">
                    <thead>
                        <tr>
                            <th>Instance Alias</th>
                            <th>Service Type</th>
                            <th>Instance UUID<button type="button" id="acl-instance-exit" class="close" aria-hidden="true">&times;</button></th>
                        </tr>
                    </thead>
                    <tbody id="instance-body">
                    </tbody>
                </table>
                <div class="closed" id="acl-instance-acl">                    
                    <input type="hidden" id="acl-instance">
                    <div class="acl-div">
                        <table class="management-table">
                            <colgroup>
                                <col span="1" style="width: 35%;"/>
                                <col span="1" style="width: 35%;"/>
                                <col span="1" style="width: 20%;"/>
                                <col span="1" style="width: 10%;"/>
                            </colgroup>
                            <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Full Name</th>
                                    <th>Email</th>
                                    <th><button type="button" id="acl-instance-close" class="close" aria-hidden="true">&times;</button></th>
                                </tr>
                            </thead>
                            <tbody id="acl-body">                           
                            </tbody>
                        </table>
                    </div>
                    <div class="acl-div">
                        <table class="management-table">
                            <colgroup>
                                <col span="1" style="width: 35%;"/>
                                <col span="1" style="width: 35%;"/>
                                <col span="1" style="width: 20%;"/>
                                <col span="1" style="width: 10%;"/>
                            </colgroup>
                            <tbody id="users-body">                           
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>            
            <div id="loading-panel"></div>
        </div>

        <script src="/StackV-web/js/acl.js"></script>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>        
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
    </body>
</html>
