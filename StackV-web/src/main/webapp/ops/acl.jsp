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
                <input type="hidden" id="acl-user">
                <div id="acl-role-user-div">
                    <table class="management-table" id="acl-user-table">
                        <thead>
                            <tr>
                                <th>Username</th>
                            </tr>
                        </thead>
                        <tbody id="user-body">
                        </tbody>
                    </table>
                </div>                
                <div id="acl-role-group-div" class="closed">
                    <table class="management-table" id="acl-group-table">
                        <thead>
                            <tr>
                                <th>Groups<button type="button" class="acl-user-close close" aria-hidden="true">&times;</button></th>
                            </tr>
                        </thead>
                        <tbody id="group-body">
                        </tbody>
                        <tfoot>
                            <tr>
                                <th>
                                    <select id="acl-group-select"><option selected disabled>Choose a group</option></select>
                                    <button class="btn-default" id="acl-group-add">Add</button>
                                </th>
                            </tr>
                        </tfoot>
                    </table>
                </div>
                <div id="acl-role-role-div" class="closed">
                    <table class="management-table" id="acl-role-table">
                        <thead>
                            <tr>
                                <th>Roles<button type="button" class="acl-user-close close" aria-hidden="true">&times;</button></th>
                            </tr>
                        </thead>
                        <tbody id="role-body">
                        </tbody>
                        <tfoot>
                            <tr>
                                <th>
                                    <select id="acl-role-select"><option selected disabled>Choose a role</option></select>
                                    <button class="btn-default" id="acl-role-add">Add</button>
                                </th>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <div class="acl-panel" id="acl-instance-panel">
                <input type="hidden" id="acl-instance">
                <table class="management-table" id="acl-instance-table">
                    <thead>
                        <tr>
                            <th>Instance Alias</th>
                            <th>Service Type</th>
                            <th>Instance UUID</th>
                        </tr>
                    </thead>
                    <tbody id="instance-body">
                    </tbody>
                </table>                
                <div id="acl-instance-acl">  
                    <div id="acl-instance-acl-existing">
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
                                    <th><button type="button" class="acl-instance-close close" aria-hidden="true">&times;</button></th>
                                </tr>
                            </thead>
                            <tbody id="acl-body">                           
                            </tbody>
                        </table>
                    </div>
                    <div id="acl-instance-acl-new">
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
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="users-body">                           
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>            
            <div id="loading-panel"></div>
        </div>

        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>        
        <script src="/StackV-web/js/acl.js"></script>
    </body>
</html>
