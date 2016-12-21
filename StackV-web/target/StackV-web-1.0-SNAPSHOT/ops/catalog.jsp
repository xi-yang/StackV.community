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
        <title>Service Catalog</title>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/bootstrap.css">
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/driver.css">
    </head>
    
    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->   
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="black-screen" class="off"></div>
        <div id="main-pane">                                                 
            <div class="closed" id="instance-panel">
                <table class="management-table" id="status-table">
                    <thead>
                        <tr>
                            <th>Instance Alias</th>
                            <th>Service Type</th>
                            <th>Instance UUID</th>
                            <th><div style="float: left;">Instance Status</div>                           
                                <button class="button-header" id="refresh-button" onclick="reloadCatalog()">Manually Refresh Now</button>
                                <div id="refresh-panel">
                                    Auto-Refresh Interval
                                    <select id="refresh-timer" onchange="timerChange(this)">
                                        <option value="off">Off</option>
                                        <option value="5">5 sec.</option>
                                        <option value="10">10 sec.</option>
                                        <option value="30">30 sec.</option>
                                        <option value="60" selected>60 sec.</option>
                                    </select>                        
                                </div>
                            </th>
                        </tr>
                    </thead>
                    <tbody id="status-body">                        
                    </tbody>
                </table>
            </div>

            <div class="closed" id="catalog-panel">
                <ul class="nav nav-tabs catalog-tabs">
                    <li><a data-toggle="tab" href="#wizard-tab">Profiles</a></li>
                    <li class="active"><a data-toggle="tab" href="#editor-tab">Intents</a></li>
                </ul>

                <div class="tab-content" id="catalog-tab-content">
                    <div id="wizard-tab" class="tab-pane fadeIn">
                        <table class="management-table tab-table">
                            <thead>
                                <tr>
                                    <th>Profile Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="wizard-body">
                            </tbody>
                        </table>
                    </div>

                    <div id="editor-tab" class="tab-pane fadeIn active">
                        <table class="management-table tab-table">
                            <thead>
                                <tr>
                                    <th>Service Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="editor-body">
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <br>
            <button type="button" class="hide" id="button-service-cancel">Cancel</button>
            <div id="service-specific"></div>
            <div id="info-panel">
                <h3 class="fs-subtitle" id="info-panel-title"></h3>
                <div id="info-panel-text">
                    <textarea id="info-panel-text-area" style="height: 400px;width: 80%;"></textarea>
                </div>
                <div id="info-panel-button">
                    <button class="button-profile-submit">Submit</button>
                </div>
            </div>
            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
            <!-- TAG PANEL -->
            <div id="tag-panel"></div>
        </div>
    </body>
</html>
