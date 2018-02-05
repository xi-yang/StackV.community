<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Service Catalog</title>

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
        <!-- TAG PANEL -->
        <div id="tag-panel">
        </div>
        <!-- MAIN PANEL -->
        <div id="black-screen"></div>
        <div class="sub-main" id="main-pane">
            <div id="instance-panel">
                <table class="management-table" id="status-table">
                    <colgroup>
                        <col style="width:25%">
                        <col style="width:10%">
                        <col style="width:35%">
                        <col style="width:30%">
                    </colgroup>
                    <thead>
                        <tr>
                            <th>Instance Alias</th>
                            <th>Service Type</th>
                            <th>Instance UUID</th>
                            <th>Instance Status</th>
                        </tr>
                    </thead>
                    <tbody id="status-body">
                    </tbody>
                </table>
            </div>            
            <br>
            <button type="button" class="hide" id="button-service-cancel">Cancel</button>
            <div id="service-specific"></div>

            <!-- LOADING PANEL -->
            <div id="loading-panel"></div>
            <!-- TAG PANEL -->
            <div id="tag-panel"></div>

            <!-- jQuery dialog for the system health check -->
            <div id="system-health-check">
                <div id="system-health-check-text"></div>
            </div>
        </div>
        <div id="catalog-panel" class="closed">
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
        <div id="profile-modal" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="info-panel-title"></h3>
                    </div>
                    <div class="modal-body">
                        <div id="info-panel-input">
                            <input id="profile-alias" placeholder="Instance Alias"/>
                        </div>
                        <div id="info-panel-text">                            
                            <textarea id="info-panel-text-area"></textarea>
                        </div>
                    </div>
                    <div id="info-panel-button" class="modal-footer">
                        <div class="info-panel-save-as-description">
                            <form class="form-inline">
                                <div class="form-group">
                                    <label for="new-profile-name"/>
                                    <input type="text" class="form-control" id="new-profile-name" placeholder="Name">
                                </div>
                                <div class="form-group">
                                    <label for="new-profile-description"/>
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

        <script src="https://unpkg.com/sweetalert/dist/sweetalert.min.js"></script>

        <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
        <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
        <script src="/StackV-web/js/mousetrap.js"></script>
        <script src="/StackV-web/js/mousetrap-dict.js"></script>

        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/catalog.js"></script>

        <script src="/StackV-web/js/svc/intentEngine.js"></script>
        <script src="/StackV-web/js/svc/handlebars.js"></script> 
    </body>
</html>
