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
        <title>Driver Management</title>
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

            
            
            <div class="active" id="driver-panel-top">
                <ul class="nav nav-tabs catalog-tabs">
                    <li class="active"><a data-toggle="tab" href="#wizard-tab">Profiles</a></li>
                    <li><a data-toggle="tab" href="#editor-tab">Intents</a></li>
                </ul>

                <div class="tab-content" id="catalog-tab-content">
                    
                    
                    
                    <div style="height:85%;overflow:auto;" id="wizard-tab" class="tab-pane fadeIn active">
                        <table class="management-table driver-tab-table">
                            <thead style="position: fixed;">
                                <tr>
                                    <th>Profile Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>

                    
                    
                    
                    <div id="editor-tab" class="tab-pane fadeIn">
                        <table class="management-table tab-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
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
            
            
            <div class="active" id="driver-panel-bot">
                <ul class="nav nav-tabs">
                    <li style="width:100%;" class="active"><a>Installed Drivers</a></li>
                </ul>

                <div class="tab-content" id="catalog-tab-content">                    
                    <div id="wizard-tab" class="tab-pane fadeIn active">
                        <table style="margin-top: 0;" class="management-table tab-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody> 
                            <p style="color: white" id="demo"></p>

                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
        </div>
    </body>
</html>
