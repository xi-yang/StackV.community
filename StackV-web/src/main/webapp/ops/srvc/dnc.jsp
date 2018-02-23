<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Dynamic Network Connection Service</title>
        <link rel='stylesheet prefetch' href='https://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
        <link rel="stylesheet" href="/StackV-web/css/dnc.css">
    </head>

    <body>
        <div id="nav">
        </div>
        <!-- MAIN PANEL -->
        <div id="info-panel" class="">
            <div id="info-fields" style ="float: top;"></div>
            <div id="info-option" style ="float: bottom;"></div>
        </div>
        <div id="overlay-black"></div>
        <div id="main-pane">
            <form class="stageform">
                <fieldset class="active-fs" id="0-template-select" style="z-index: 4;">
                    <div id="table-div">
                        <table id="input-table">
                            <tbody id="link-body">
                                <input type="text" placeholder="Service Alias" id="service-name">
                                <tr id="spacer"></tr>
                                <tr>
                                    <td>Link 1</td>
                                    <td style="text-align: center;">
                                        <div>
                                            <input type="text" id="linkUri1" placeholder="Connection-Name">
                                            <input type="text" id="linksrc1" placeholder="Source">
                                            <input type="text" id="linksrc-vlan1" placeholder="Vlan-tag" value="any">
                                            <input type="text" id="linkdes1" placeholder="Destination">
                                            <input type="text" id="linkdes-vlan1" placeholder="Vlan-tag" value="any">
                                        </div>
                                    </td>
                                </tr>
                                <tr id="spacer"></tr>
                            </tbody>
                        </table>
                        <div id="test">
                            <p id="resss"></p>
                            <button type="button" class="action-button" onclick="submitToBackend();">Submit</button>
                            <button type="button" class="action-button" onclick="openWindow();">Save</button>
                            <button type="button" class="action-button" onclick="addLinkDNC();">Add Link</button>
                        </div>
                    </div>

                </fieldset>
            </form>
            <!--
            <div>
                <p style="color: white;">add:</p>
                <textarea rows="10" cols="100" id="addfield"></textarea>
                <p style="color: white;">Del:</p>
                <input type="text" id="delfield">
                <p id="ret_field" style="color: white;"></p>
                <button type="button" class="action-button" onclick="test();">Add</button>
                <button type="button" class="action-button" onclick="del();">Delete</button>
            </div>
        </div> -->
            <!-- TAG PANEL -->
            <div id="tag-panel">
            </div>
        </div>

        <script src="https://k152.maxgigapop.net:8543/auth/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/jquery-ui.min.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/svc/dnc.js"></script>

    </body>
</html>
