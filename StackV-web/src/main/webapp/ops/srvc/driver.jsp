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
        <link rel="stylesheet" href="/StackV-web/css/driver.css">
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
                                    <button  onclick='clearPanel(); activateSide(); installAWS();  changeNameInst();' class='install install-button button-profile-select btn btn-primary'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Generic REST Driver</td>
                                <td>Generic REST API for Compatible Model Driven Services</td>
                                <td style="width: 180px;">
                                    <button onclick='clearPanel(); activateSide(); installGeneric(); changeNameInst();' class='install install-button button-profile-select btn btn-primary'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>OpenStack Driver</td>
                                <td>OpenStack Cloud Services</td>
                                <td  style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installOpenstack();  changeNameInst();' class='install install-button button-profile-select btn btn-primary'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Stack Driver</td>
                                <td>Stack Over Sub-level StackV in Hierarchical Deployment</td>
                                <td  style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installStack();  changeNameInst();' class='install install-button button-profile-select btn btn-primary'>Install</button>
                                </td>
                            </tr> 
                            <tr>
                                <td>Stub Driver</td>
                                <td>A Dump Driver Instance with Pre-loaded Fixed Model</td>
                                <td style="width: 180px;">
                                    <button  onclick='clearPanel(); activateSide(); installStub(); changeNameInst();' class='install install-button button-profile-select btn btn-primary'>Install</button>
                                </td>
                            </tr>
                            <tr>
                                <td>Raw Driver</td>
                                <td>Take Raw XML to Plug in "Any" Type of Driver Instance</td>
                                <td  style="width: 180px;">
                                    <button onclick='clearPanel(); activateSide(); installRaw();  changeNameInstRaw();' class='install install-button button-profile-select btn btn-primary'>Install</button>
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
                                <th>Driver Template Name</th>
                                <th>Driver Template Description</th>
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
                    <div class="modal-header">
                        <h3 id="info-panel-title"></h3>
                    </div>
                    <div class ="modal-body " style="background-color:#FFFFFF">
                        <div class="tab-content" style="background-color:#FFFFFF">                                    
                            <div id="info-panel-body" class="tab-pane fadeIn" style="display:block;background-color:#FFFFFF">
                                <div id="install-content" class="tab-pane fadeIn" style="background-color:#FFFFFF">
                                    <div id='install-type' style="background-color:#FFFFFF">
                                    </div>
                                    <div id='install-type-right' style="background-color:#FFFFFF"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div id="info-panel-button" class="modal-footer">
                        <div id='install-options'>
                            <button onclick="clearPanel(); closeContentPanel();">Close</button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- jQuery element to display a dialog contain the verbose value of a driver detail -->
            <div id="dialog-overflow-details">
                <div id="dialog-overflow-details-text"></div>
            </div>

            <!-- element to display confirmation dialogs with jquery -->
            <div id="dialog-confirm">
                <div id="dialog-confirm-text"></div>
            </div>

            <!-- jQuery dialog for the system health check -->
            <div id="system-health-check">
                <div id="system-health-check-text"></div>
            </div>

            <!-- jQuery dialog to display service isntance errors -->
            <div id="service-instances">
                <div id="service-instances-body"></div>
            </div>

            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
        </div>

        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>

        <script src="https://code.jquery.com/jquery-2.2.4.min.js"
                integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44="
                crossorigin="anonymous">
        </script>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" 
                integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" 
                crossorigin="anonymous">
        </script>        
        <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"
                integrity="sha256-T0Vest3yCU7pafRw9r+settMBX6JkKN06dqBnpQ8d30="
                crossorigin="anonymous">
        </script>

        <script src="https://unpkg.com/sweetalert/dist/sweetalert.min.js"></script>

        <script src="/StackV-web/js/mousetrap.js"></script>
        <script src="/StackV-web/js/mousetrap-dict.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/driver.js"></script>
    </body>
</html>
