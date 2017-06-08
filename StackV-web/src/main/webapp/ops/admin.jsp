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
        <title>Administrator's Panel</title>

        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.structure.min.css">
        <link rel="stylesheet" href="/StackV-web/css/jquery-ui.theme.css">
    </head>

    <body>
        <div id="black-screen"></div>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <div id="sub-nav">            
        </div>
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>       
        <!-- MAIN PANEL -->
        <div class="sub-main" id="main-pane">
            <div id="admin-panel">
                <div id = "API-panel">
                    <div id ="API-header-div">API Module</div>
                    <div id="logging-body-div">
                        <select id = "API-request">
                            <option value="GET">GET</option>
                            <option value="PUT">PUT</option>
                            <option value="POST">POST</option>
                            <option value="DELETE">DELETE</option>
                        </select>

                            <input class = "typeahead" type="text" placeholder="URL" id="URL">

                        <button id = "SEND" type="button" class="action-button" onclick="executeRequest();">Send</button>
                    </div>
                    <div id="logging-body-div">
                        <textarea id="api_result" style="color: black;"></textarea>
                    </div>
                </div>
            </div>
            <div id="logging-panel">
                <div id="logging-header-div">
                    Logs                  
                </div>
                <div id="logging-body-div">
                    <table id="loggingData" class="table table-striped table-bordered display nowrap" cellspacing="0" width="100%">
                        <thead>
                            <tr>
                                <th></th>
                                <th>Timestamp</th>
                                <th>Event</th>
                                <th>Reference UUID</th>
                                <th>Level</th>                                
                            </tr>
                        </thead>
                        <tfoot>
                            <tr>
                                <th></th>
                                <th>Timestamp</th>
                                <th>Event</th>
                                <th>Reference UUID</th>
                                <th>Level</th>                                
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
        <div id="details-viz" ></div>

        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/mousetrap.js"></script>
        <script src="/StackV-web/js/mousetrap-dict.js"></script>

        <script src="/StackV-web/js/datatables/jquery.dataTables.min.js"></script>
        <script src="/StackV-web/js/datatables/dataTables.scroller.min.js"></script>
        <script src="/StackV-web/js/datatables/dataTables.fixedColumns.min.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/admin.js"></script>
<!--        type ahead libraries-->
	<script src="/StackV-web/js/typeahead.js/typeahead.bundle.js"></script>
  	<script src="/StackV-web/js/test.js"></script>
        
        
        

        <script>
            //Based off http://dojotoolkit.org/documentation/tutorials/1.10/dojo_config/ recommendations
            dojoConfig = {
                has: {
                    "dojo-firebug": true,
                    "dojo-debug-messages": true
                },
                async: true,
                parseOnLoad: true,
                packages: [
                    {
                        name: "d3",
                        location: "//d3js.org/",
                        main: "d3.v3"
                    },
                    {
                        name: "local",
                        location: "/StackV-web/js/"
                    }
                ]
            };

            $(function () {
                $("#dialog_policyAction").dialog({
                    autoOpen: false
                });
                $("#dialog_policyData").dialog({
                    autoOpen: false
                });
            });
        </script>
        <script src="//ajax.googleapis.com/ajax/libs/dojo/1.10.0/dojo/dojo.js"></script>
    </body>
</html>
