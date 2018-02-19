<%@page contentType="text/html" pageEncoding="UTF-8"%>
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

        <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/v/dt/jszip-2.5.0/dt-1.10.16/b-1.5.1/b-colvis-1.5.1/b-html5-1.5.1/fc-3.2.4/r-2.2.1/sc-1.4.4/sl-1.2.5/datatables.min.css"/>
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
                    Logs - Current time: <p id="log-time"></p>
                    <div style="float:right;">
                        <label for="logging-filter-level" style="font-weight: normal;margin-left: 15px;">Logging Level</label>
                        <select id="logging-filter-level" onchange="filterLogs(this)">
                            <option value="TRACE" selected>TRACE</option>
                            <option value="INFO">INFO</option>
                            <option value="WARN">WARN</option>
                            <option value="ERROR">ERROR</option>
                        </select> 
                    </div>                  
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

        <script src="https://k152.maxgigapop.net:8543/auth/js/keycloak.js"></script>
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
        
        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/mousetrap.js"></script>
        <script src="/StackV-web/js/mousetrap-dict.js"></script>

        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.32/pdfmake.min.js"></script>
        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.32/vfs_fonts.js"></script>
        <script type="text/javascript" src="https://cdn.datatables.net/v/dt/jszip-2.5.0/dt-1.10.16/b-1.5.1/b-colvis-1.5.1/b-html5-1.5.1/fc-3.2.4/r-2.2.1/sc-1.4.4/sl-1.2.5/datatables.min.js"></script>

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
