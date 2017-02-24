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
        <title>Service Details</title>

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
            <div id="details-panel">
            </div>
            <div id="loading-panel"></div>
        </div>
        <div id="details-viz" ></div>

        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        
        <script src="/StackV-web/js/nexus.js"></script>

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
