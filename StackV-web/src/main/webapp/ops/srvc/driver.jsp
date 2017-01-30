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

        <title>Driver Management</title>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/driver.js"></script>
        
        <link rel="stylesheet" href="/StackV-web/css/animate.min.css">
        <link rel="stylesheet" href="/StackV-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>
    
    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
      <div>
                <div class="tab-content" id="catalog-tab-content">
                    
                    <div style="width: 100%; height: 85%; overflow: auto;" id="driver-tab" class="tab-pane fadeIn active">
                        <table class="management-table">
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
                                    <td style="width: 180px;">
                                        <button style='width: 50px;' onclick='clearPanel(); activateSide();  changeNameDet();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='clearPanel(); activateSide(); installStub(); changeNameInst();' class='install' id='install-button'>Install</button>
                                    </td>
                              </tr>
                                <tr>
                                    <td>AWS</td>
                                    <td>This is a placement description</td>
                                    <td style="width: 180px;">
                                        <button style='width: 50px;' onclick='clearPanel(); activateSide(); changeNameDet();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='clearPanel(); activateSide(); installAWS();  changeNameInst();' class='install' id='install-button'>Install</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Generic</td>
                                    <td>This is a placement description</td>
                                    <td style="width: 180px;">
                                        <button style='width: 50px;' onclick='clearPanel(); activateSide(); changeNameDet();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='clearPanel(); activateSide(); installStack(); changeNameInst();' class='install' id='install-button'>Install</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Openstack</td>
                                    <td>This is a placement description</td>
                                    <td  style="width: 180px;">
                                        <button style='width: 50px;' onclick='clearPanel(); activateSide(); changeNameDet();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='clearPanel(); activateSide(); installOpenstack();  changeNameInst();' class='install' id='install-button'>Install</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Stack</td>
                                    <td>This is a placement description</td>
                                    <td  style="width: 180px;">
                                        <button style='width: 50px;' onclick='clearPanel(); activateSide();' class='details' id='details-button'>Details</button>
                                        <div class='divider'></div>
                                        <button style ='width: 50px;' onclick='clearPanel(); activateSide(); installStack();  changeNameInst();' class='install' id='install-button'>Install</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                        
                    <div style="width: 100%; height: 85%; overflow: auto;" id="saved-tab" class="tab-pane fadeIn">
                        <table class="management-table">
                            <thead>
                                <tr>
                                    <th>Driver Name</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="saved-table">
                           
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            <div class="active driver-panel no-side-tab" id="driver-panel-bot">
                <ul class="nav nav-tabs catalog-tabs">
                    <li style="width:100%;" onclick="getAllDetails();"><a data-toggle="tab" href="#installed-tag">Installed Drivers</a></li>
                </ul>
                
                <div class="tab-content" id="catalog-tab-content">
                    <div style="display: inline-block; width: 100%; height: 85%; overflow: auto;" id="template-tab" class="tab-pane fadeIn active">
                        <table  class="management-table">
                            <thead>
                                <tr>
                                    <th>Driver ID</th>
                                    <th>Driver Type</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="installed-body">
                            <script></script>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            
            
            
            
            
            
            
            
            
            
            
            
            <div class="inactive" id="driver-panel-right">
                <ul class="nav nav-tabs catalog-tabs">
                    <li style="width: 100%;" id="side-tab"><a id="side-name">Details</a></li>
                </ul>
                
                <div class="tab-content" id="catalog-tab-content">                                    
                    <div id="install-content" class="tab-pane fadeIn">
                        <div id='install-type'></div>
                        <div id='install-type-right'></div>
                        <div id='install-options'>
                            <button onclick="clearPanel(); closeSide();">Close</button>
                        </div>

                    </div>
                        
                </div>
                    
                <!-- LOADING PANEL -->
                <div id="loading-panel"></div>
            </div>

        <!-- JS -->
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
 
