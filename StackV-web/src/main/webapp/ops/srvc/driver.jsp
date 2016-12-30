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
        <script src="/StackV-web/js/driver.js"></script>
        <script src="/StackV-web/js/jquery.floatThead.js"></script>

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

            
            
            
            
            
            
            <div style="margin: 0 auto;" class="active driver-panel no-side-tab" id="driver-panel-top">
                <ul class="nav nav-tabs catalog-tabs">
                    <li class="active"><a data-toggle="tab" href="#template-tab">Template</a></li>
                    <li><a data-toggle="tab" href="#driver-tab">Saved Drivers</a></li>
                </ul>

                
                <div class="tab-content" id="catalog-tab-content">
                    <div style="display: inline-block; width: 100%; height: 85%; overflow: auto;" id="template-tab" class="tab-pane fadeIn active">
                        <table  class="management-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Stub</td>
                                    <td>This is a placement description</td>
                                    <td style="width: 170px;">
                                        <button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>
                                    </td>
                                <tr>
                                    <td>AWS</td>
                                    <td>This is a placement description</td>
                                    <td style="width: 170px;">
                                        <button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>
                                    </td>
                                <tr>
                                    <td>Generic</td>
                                    <td>This is a placement description</td>
                                    <td style="width: 170px;">
                                        <button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>
                                    </td>
                                <tr>
                                    <td>Openstack</td>
                                    <td>This is a placement description</td>
                                    <td  style="width: 170px;">
                                        <button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>
                                    </td>
                                <tr>
                                    <td>Stack</td>
                                    <td>This is a placement description</td>
                                    <td  style="width: 170px;">
                                        <button style='width: 50px;' onclick='activateDetails();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='activateInstall();' class='install' id='install-button'>Install</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                         
                    <div style="display: inline-block; width: 100%; height: 85%; overflow: auto;" id="driver-tab" class="tab-pane fadeIn">
                        <table class="management-table tab-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="template-body">
                            <script>driverTopTable();</script>
                            </tbody>
                        </table>
                        </table>
                    </div>
                </div>
            </div>
            
            
            
            
     
            
            
            
            
            
            
            
            
            
            <div class="active driver-panel no-side-tab" id="driver-panel-bot">
                <ul class="nav nav-tabs">
                    <li style="width:100%;" class="active"><a data-toggle="tab" href="#installed-tag">Installed Drivers</a></li>
                </ul>

                <div class="tab-content" id="catalog-tab-content">                    
                    <div style="display: inline-block; width: 100%; height: inherit; overflow: auto;" id="installed-tag" class="tab-pane fadeIn active">
                        <table class="management-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="installed-body"> 
                            <script>driverBotTable();</script>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            
            
                
                
                
            
            
            
            
            
            
            
            <div class="inactive" id="driver-panel-right">
                <ul class="nav nav-tabs catalog-tabs">
                    <li style="width: 100%;" id="details-tab"><a data-toggle="tab" href="#detail-content">Details</a></li>
                    <li style="width: 100%;" id="install-tab"><a data-toggle="tab" href="#install-content">Install</a></li>
                </ul>
                    
                <div class="tab-content" id="catalog-tab-content">                    
                    <div id="detail-content" class="tab-pane fadeIn">
                        <button style="width: 50px;" onclick="closeSide();" class="install" id="close-button">Install</button>
                    </div>
                        
                        
                    <div id="install-content" class="tab-pane fadeIn">
                        <button style="width: 50px;" onclick="closeSide();" class="install" id="close-button">Close</button>
                    </div>
                    
                </div>
                
                <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
        </div>
    </body>
</html>
