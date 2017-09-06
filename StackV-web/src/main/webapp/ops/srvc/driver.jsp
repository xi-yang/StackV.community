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

        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.structure.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.theme.css">
    </head>

    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <div id="sub-nav">            
        </div>
        <!-- SIDE BAR -->   
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="black-screen"></div>
        <div class="sub-main" id="main-pane">                                         

            <div id="info-panel" class="">
                <div id="info-fields" style ="float: top;"></div>
                <div id="info-option" style ="float: bottom;"></div>
            </div>

            <div id="driver-add-panel">
                <div style="width: 100%; height: 85%;display: block;" id="driver-tab1" class="tab-pane fadeIn">
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
                                <td>AWS Driver</td>
                                <td>Amazon AWS Cloud Services</td>
                                <td style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installAWS();  changeNameInst();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Generic REST Driver</td>
                                <td>Generic REST API for Compatible Model Driven Services</td>
                                <td style="width: 180px;">
                                    <button onclick='clearPanel(); activateSide(); installGeneric(); changeNameInst();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>OpenStack Driver</td>
                                <td>OpenStack Cloud Services</td>
                                <td  style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installOpenstack();  changeNameInst();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Stack Driver</td>
                                <td>Stack Over Sub-level StackV in Hierarchical Deployment</td>
                                <td  style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installStack();  changeNameInst();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr> 
                            <tr>
                                <td>Stub Driver</td>
                                <td>A Dump Driver Instance with Pre-loaded Fixed Model</td>
                                <td style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installStub(); changeNameInst();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Raw Driver</td>
                                <td>Take Raw XML to Plug in "Any" Type of Driver Instance</td>
                                <td  style="width: 180px;">
                                    <button onclick='clearPanel(); activateSide(); installRaw();  changeNameInstRaw();' class='install install-button button-profile-select btn btn-default'>Install</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="installed-panel">
                <div class="tab-content" id="installed-content">
                    <div id="template-tab" class="tab-pane fadeIn active">
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
            <div id="driver-template-panel">
                <div id="saved-tab" class="tab-pane fadeIn">
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


            <div id="driver-content-panel" class="hidden">
                <div class="modal-content">
                    <div class = "modal-header">
                        <h3 id ="info-panel-title"></h3>
                    </div>
                    <div class ="modal-body " style="background-color:#FFFFFF">
                        <div class="tab-content" style="background-color:#FFFFFF">                                    
                            <div id="info-panel-body" class="tab-pane fadeIn" style="display:block;background-color:#FFFFFF">
                                <div id="install-content" class="tab-pane fadeIn" style="background-color:#FFFFFF">
                                    <div id='install-type' style="background-color:#FFFFFF"></div>
                                    <div id='install-type-right' style="background-color:#FFFFFF"></div>
                                    <div id = "info-panel-button"class = "modal-footer">
                                        <div id='install-options'>
                                            <button onclick="clearPanel(); closeContentPanel();">Close</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                </div>
            </div>

            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
        </div>

        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <script src="/StackV-web/js/mousetrap.js"></script>
        <script src="/StackV-web/js/mousetrap-dict.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/driver.js"></script>
    </body>
</html>