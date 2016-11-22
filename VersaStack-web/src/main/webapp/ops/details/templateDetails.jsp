<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Service Details</title>
        <script src="/VersaStack-web/js/keycloak.js"></script> 
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <script src="/VersaStack-web/js/jquery-ui.min.js"></script>

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
                        location: "/VersaStack-web/js/"
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
        
        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.structure.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/jquery-ui.theme.css">                

    </head>

    <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/frontend"
                       user="front_view"  password="frontuser"/>

    <body>        
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">         
            <div id="button-panel">
                <button type="button" id="button-service-return">Back to Catalog</button>                 
            </div> 
            <div id="details-panel">
                <sql:query dataSource="${front_conn}" sql="SELECT S.name, I.creation_time, I.alias_name, X.super_state, V.verification_state FROM service S, service_instance I, service_state X, service_verification V
                           WHERE I.referenceUUID = ? AND I.service_instance_id = V.service_instance_id AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id" var="instancelist">
                    <sql:param value="${param.uuid}" />
                </sql:query>

                <c:forEach var="instance" items="${instancelist.rows}">
                    <div id="instance-verification" class="hide">${instance.verification_state}</div>
                    <table class="management-table" id="details-table">
                        <thead>
                            <tr>
                                <th>${instance.name} Service Details</th>
                                <th>
                                    <div id="refresh-panel">
                                        Auto-Refresh Interval
                                        <select id="refresh-timer" onchange="timerChange(this)">
                                            <option value="off">Off</option>
                                            <option value="5">5 sec.</option>
                                            <option value="10">10 sec.</option>
                                            <option value="30">30 sec.</option>
                                            <option value="60" selected>60 sec.</option>
                                        </select>
                                    </div>       
                                    <button class="button-header" id="refresh-button" onclick="reloadInstance()">Refresh in    seconds</button>
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>Instance Alias</td>
                                <td>${instance.alias_name}</td>
                            </tr>
                            <tr>
                                <td>Reference UUID</td>
                                <td>${param.uuid}</td>
                            </tr>
                            <tr>
                                <td>Creation Time</td>
                                <td id="instance-creation-time">${instance.creation_time}</td>
                            </tr>
                            <tr>
                                <td>Instance State</td>
                                <td id="instance-superstate">${instance.super_state}</td>
                            </tr>
                            <tr>
                                <td>Operation Status</td>
                                <td id="instance-substate"></td>
                            </tr>
                            <tr>
                                <td colspan="2"><div id="instruction-block"></div></td>
                            </tr>
                            <tr>
                                <td colspan="2">
                                    <div class="service-instance-panel">
                                        <button class="hide" id="instance-reinstate" onClick="reinstateInstance('${param.uuid}')">Reinstate</button>
                                        <button class="hide" id="instance-freinstate" onClick="forceReinstateInstance('${param.uuid}')">Force Reinstate</button>
                                        <button class="hide" id="instance-cancel" onClick="cancelInstance('${param.uuid}')">Cancel</button>
                                        <button class="hide" id="instance-fcancel" onClick="forceCancelInstance('${param.uuid}')">Force Cancel</button>
                                        <button class="hide" id="instance-fretry" onClick="forceRetryInstance('${param.uuid}')">Force Retry</button>
                                        <button class="hide" id="instance-modify" onClick="modifyInstance('${param.uuid}')">Modify</button>
                                        <button class="hide" id="instance-fmodify" onClick="forceModifyInstance('${param.uuid}')">Force Modify</button>
                                        <button class="hide" id="instance-reverify" onClick="verifyInstance('${param.uuid}')">Re-Verify</button>
                                        <button class="hide" id="instance-delete" onClick="deleteInstance('${param.uuid}')">Delete</button>
                                        <button class="hide" id="instance-fdelete" onClick="deleteInstance('${param.uuid}')">Force Delete</button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <sql:query dataSource="${front_conn}" sql="SELECT D.service_delta_id, D.delta, D.type, S.super_state FROM service_delta D, service_instance I, service_state S, service_history H 
                               WHERE I.referenceUUID = ? AND I.service_instance_id = D.service_instance_id AND D.service_history_id = H.service_history_id 
                               AND D.service_instance_id = H.service_instance_id AND H.service_state_id = S.service_state_id" var="deltalist">
                        <sql:param value="${param.uuid}" />
                    </sql:query>

                    <c:forEach var="delta" items="${deltalist.rows}">
                        <table class="management-table delta-table" id="delta-${delta.type}">
                            <thead class="delta-table-header" id="delta-${delta.service_delta_id}">
                                <tr>
                                    <th>Delta Details</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody class="delta-table-body" id="body-delta-${delta.service_delta_id}">
                                <tr>
                                    <td>Delta State</td>
                                    <td>${delta.super_state}</td>
                                </tr>
                                <tr>
                                    <td>Delta Type</td>
                                    <td>${delta.type}</td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td>${delta.delta}</td>
                                </tr>
                                <tr>                               
                                    <td colspan="2">       
                                        <button  class="details-model-toggle" onclick="toggleTextModel('.${delta.type}-delta-table', '#delta-${delta.type}');">Toggle Text Model</button>          
                                    </td>
                                </tr>                                
                            </tbody>
                        </table>
                    </c:forEach>

                    <table class="management-table">
                        <thead class="delta-table-header">
                            <tr>
                                <th>Access Control</th>                                
                            </tr>
                        </thead>
                        <tbody class="delta-table-body" id="acl-body">
                            <tr>
                                <td><select id="acl-select" size="5" name="acl-select" multiple></select></td>
                            </tr>
                            <tr>
                                <td><label>Give user access: <input type="text" name="acl-input" /></label></td>
                            </tr>
                        </tbody>
                    </table>


                    <table class="management-table hide service-delta-table">
                        <thead class="delta-table-header">
                            <tr>
                                <th>Service Delta</th>
                                <th>Addition</th>
                                <th>Reduction</th>
                            </tr>
                        </thead>
                        <tbody class="delta-table-body">
                            <tr id="serv-delta-row">
                                <td></td>
                                <td id="serv-add"></td>
                                <td id="serv-red"></td>
                            </tr>
                            <tr>                               
                                <td colspan="3" >       
                                    <button class="details-model-toggle" onclick="toggleTextModel('.service-delta-table', '#delta-Service');">Toggle Text Model</button>          
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <table class="management-table hide system-delta-table">
                        <thead class="delta-table-header">
                            <tr>
                                <th>System Delta</th>
                                <th>Addition</th>
                                <th>Reduction</th>
                            </tr>
                        </thead>
                        <tbody class="delta-table-body">
                            <tr id="sys-delta-row">
                                <td></td>
                                <td id="sys-add"></td>
                                <td id="sys-red"></td>
                            </tr>
                            <tr>
                                <td colspan="3">
                                    <button class="details-model-toggle" onclick="toggleTextModel('.system-delta-table', '#delta-System');">Toggle Text Model</button>                                
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <sql:query dataSource="${front_conn}" sql="SELECT V.service_instance_id, V.verification_run, V.creation_time, V.addition, V.reduction, V.verified_reduction, V.verified_addition, V.unverified_reduction, V.unverified_addition
                               FROM service_verification V, service_instance I WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id" var="verificationlist">
                        <sql:param value="${param.uuid}" />
                    </sql:query>

                    <c:forEach var="verification" items="${verificationlist.rows}">
                        <div id="verification-run" class="hide">${verification.verification_run}</div>
                        <div id="verification-time" class="hide">${verification.creation_time}</div>
                        <div id="verification-addition" class="hide">${verification.addition}</div>
                        <div id="verification-reduction" class="hide">${verification.reduction}</div>
                        <table class="management-table hide verification-table">
                            <thead class="delta-table-header" id="delta-${verification.service_instance_id}">
                                <tr>
                                    <th></th>
                                    <th>Verified</th>
                                    <th>Unverified</th>
                                </tr>
                            </thead>
                            <tbody class="delta-table-body" id="body-delta-${verification.service_instance_id}">
                                <tr id="verification-addition-row">
                                    <td>Addition</td>
                                    <td id="ver-add"></td>
                                    <td id="unver-add"></td>
                                </tr>
                                <tr id="verification-reduction-row">
                                    <td>Reduction</td>
                                    <td id="ver-red"></td>
                                    <td id="unver-red"></td>
                                </tr>
                                <tr>
                                    <td colspan="3">
                                        <button class="details-model-toggle" onclick="toggleTextModel('.verification-table', '#delta-System');">Toggle Text Model</button>                                
                                    </td>
                                </tr>                              
                            </tbody>
                        </table>
                    </c:forEach>
                </c:forEach>                
            </div>  
            <div id="loading-panel"></div>
        </div>
        <div id="details-viz" ></div>
    </body>
</html>
