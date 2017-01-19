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
        <title>Service Catalog</title>

        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
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
                            <th>
                              <%-- TODO: text alignment with rest of header --%>
                                <span>Instance Status</span>
                                <div id="refresh-panel" class="form-inline">
                                    <label for="refresh-timer">Auto-Refresh Interval</label>
                                    <select id="refresh-timer" onchange="timerChange(this)" class="form-control">
                                        <option value="off">Off</option>
                                        <option value="5">5 sec.</option>
                                        <option value="10">10 sec.</option>
                                        <option value="30">30 sec.</option>
                                        <option value="60" selected>60 sec.</option>
                                    </select>
                                    <button class="button-header btn btn-sm" id="refresh-button" onclick="reloadCatalog()">Manually Refresh Now</button>
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

            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
            <!-- TAG PANEL -->
            <div id="tag-panel"></div>
        </div>
        <div id="profile-modal" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="info-panel-title"></h3>
                    </div>
                    <div class="modal-body">
                        <div id="info-panel-text">
                            <textarea id="info-panel-text-area"></textarea>
                        </div>
                    </div>
                    <div id="info-panel-button" class="modal-footer">
                        <div class="info-panel-save-as-description">
                            <form class="form-inline">
                              <div class="form-group">
                                <label for="new-profile-name">
                                <input type="text" class="form-control" id="new-profile-name" placeholder="Hybrid Cloud">
                              </div>
                              <div class="form-group">
                                <label for="new-profile-description">
                                <input type="text" class="form-control" id="new-profile-description" placeholder="Description">
                              </div>
                            </form>
                        </div>
                        <div class="info-panel-footer">
                          <div class="info-panel-regular-buttons">
                            <button type="button" name="button" class="button-profile-save btn btn-default">Save</button>
                            <button class="button-profile-save-as btn btn-default">Save As</button>
                            <button class="button-profile-submit btn btn-default">Submit</button>
                          </div>
                          <div class="info-panel-save-as-description">
                            <button class="button-profile-save-as-confirm btn btn-default">Confirm</button>
                            <button class="button-profile-save-as-cancel btn btn-default">Cancel</button>
                          </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
    </body>
</html>
