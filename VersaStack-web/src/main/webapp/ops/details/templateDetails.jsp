<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Service Details</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
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
            <button type="button" id="button-service-return">Back to Catalog</button>           

            <div id="instance-panel">
                <sql:query dataSource="${front_conn}" sql="SELECT S.name, X.super_state, V.verification_state FROM service S, service_instance I, service_state X, service_verification V
                           WHERE I.referenceUUID = ? AND I.service_instance_id = V.service_instance_id AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id" var="instancelist">
                    <sql:param value="${param.uuid}" />
                </sql:query>

                <c:forEach var="instance" items="${instancelist.rows}"> 
                    <div id="instance-verification" class="hide">${instance.verification_state}</div>
                    <table class="management-table" id="details-table">
                        <thead>
                            <tr>
                                <th>${instance.name} Service Details</th>
                                <th><button class="button-header" onclick="reloadPanel('instance-panel')">Refresh Now</button></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>Service Reference UUID</td>
                                <td>${param.uuid}</td>
                            </tr>
                            <tr>
                                <td>Service State</td>
                                <td id="instance-superstate">${instance.super_state}</td>
                            </tr>
                            <tr>
                                <td>Operation Status</td>
                                <td id="instance-substate">${serv.detailsStatus(param.uuid)}</td>
                            </tr>
                            <tr>
                                <td><div id="instruction-block"></div></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td>
                                    <div class="service-instance-panel">
                                        <button class="hide" id="instance-reinstate" onClick="reinstateInstance('${param.uuid}')">Reinstate</button>
                                        <button class="hide" id="instance-cancel" onClick="cancelInstance('${param.uuid}')">Cancel</button>
                                        <button class="hide" id="instance-fcancel" onClick="forceCancelInstance('${param.uuid}')">Force Cancel</button>
                                        <button class="hide" id="instance-fretry" onClick="">Force Retry</button>
                                        <button class="hide" id="instance-modify" onClick="">Modify</button>
                                        <button class="hide" id="instance-fmodify" onClick="">Force Modify</button>
                                        <button class="hide" id="instance-reverify" onClick="">Re-Verify</button>
                                        <button class="hide" id="instance-delete" onClick="deleteInstance('${param.uuid}')">Delete</button>
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
                            </tbody>
                        </table>
                    </c:forEach>

                    <sql:query dataSource="${front_conn}" sql="SELECT V.service_instance_id, V.creation_time, V.addition, V.reduction, V.verified_reduction, V.verified_addition, V.unverified_reduction, V.unverified_addition
                               FROM service_verification V, service_instance I WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id" var="verificationlist">
                        <sql:param value="${param.uuid}" />
                    </sql:query>

                    <c:forEach var="verification" items="${verificationlist.rows}">
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
                                    <td>${verification.verified_addition}</td>
                                    <td>${verification.unverified_addition}</td>
                                </tr>
                                <tr id="verification-reduction-row">
                                    <td>Reduction</td>
                                    <td>${verification.verified_reduction}</td>
                                    <td>${verification.unverified_reduction}</td>
                                </tr>
                            </tbody>
                        </table>
                    </c:forEach>
                </c:forEach>
            </div>  
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                deltaModerate();        
                instructionModerate();
                buttonModerate();
                
                setInterval(function() {
                  $('#instance-panel').load(document.URL +  ' #instance-panel');
                }, 10000); 

                $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
                    if (${user.isAllowed(1)}) {
                        var element = document.getElementById("service1");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(2)}) {
                        var element = document.getElementById("service2");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(3)}) {
                        var element = document.getElementById("service3");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(4)}) {
                        var element = document.getElementById("service4");
                        element.classList.remove("hide");
                    }
                });
            });
            
            function deltaModerate() {
                var verificationTime = document.getElementById("verification-time").innerHTML;
                var verificationAddition = document.getElementById("verification-addition").innerHTML;
                var verificationReduction = document.getElementById("verification-reduction").innerHTML;
                
                if (verificationTime !== '') {
                    $("#delta-System").toggleClass("hide");
                    $(".verification-table").toggleClass("hide");
                    
                    if (verificationAddition === '') {
                        $("#verification-addition-row").toggleClass("hide");
                    }
                    if (verificationReduction === '') {
                        $("#verification-reduction-row").toggleClass("hide");
                    }
                }
            }

            function instructionModerate() {
                var subState = document.getElementById("instance-substate").innerHTML;
                var verificationState = document.getElementById("instance-verification").innerHTML;
                var blockString = "";

                // State 0 - Before Verify
                if (subState !== 'READY' && subState !== 'FAILED') {
                    blockString = "Service is still processing. Please hold for further instructions.";
                }
                // State 1 - Ready & Verifying
                else if (subState === 'READY' && verificationState === '0') {
                    blockString = "Service is still verifying.";
                }
                // State 2 - Ready & Verified
                else if (subState === 'READY' && verificationState === '1') {
                    blockString = "Service has been successfully verified.";
                }
                // State 3 - Ready & Unverified
                else if (subState === 'READY' && verificationState === '-1') {
                    blockString = "Service was not able to be verified.";
                }
                // State 4 - Failed & Verifying
                else if (subState === 'FAILED' && verificationState === '0') {
                    blockString = "Service is still verifying.";
                }
                // State 5 - Failed & Verified
                else if (subState === 'FAILED' && verificationState === '1') {
                    blockString = "Service has been successfully verified.";
                }
                // State 6 - Failed & Unverified
                else if (subState === 'FAILED' && verificationState === '-1') {
                    blockString = "Service was not able to be verified.";
                }

                document.getElementById("instruction-block").innerHTML = blockString;
            }

            function buttonModerate() {
                var superState = document.getElementById("instance-superstate").innerHTML;
                var subState = document.getElementById("instance-substate").innerHTML;
                var verificationState = document.getElementById("instance-verification").innerHTML;

                if (superState === 'Create') {
                    // State 1 - Ready & Verifying
                    if (subState === 'READY' && verificationState === '0') {

                    }
                    // State 2 - Ready & Verified
                    else if (subState === 'READY' && verificationState === '1') {
                        $("#instance-cancel").toggleClass("hide");
                        $("#instance-modify").toggleClass("hide");
                    }
                    // State 3 - Ready & Unverified
                    else if (subState === 'READY' && verificationState === '-1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-reverify").toggleClass("hide");
                    }
                    // State 4 - Failed & Verifying
                    else if (subState === 'FAILED' && verificationState === '0') {

                    }
                    // State 5 - Failed & Verified
                    else if (subState === 'FAILED' && verificationState === '1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-fmodify").toggleClass("hide");
                    }
                    // State 6 - Failed & Unverified
                    else if (subState === 'FAILED' && verificationState === '-1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-fretry").toggleClass("hide");
                        $("#instance-reverify").toggleClass("hide");
                    }
                }
                else if (superState === 'Cancel') {
                    // State 1 - Ready & Verifying
                    if (subState === 'READY' && verificationState === '0') {

                    }
                    // State 2 - Ready & Verified
                    else if (subState === 'READY' && verificationState === '1') {
                        $("#instance-reinstate").toggleClass("hide");
                        $("#instance-modify").toggleClass("hide");
                        $("#instance-delete").toggleClass("hide");
                    }
                    // State 3 - Ready & Unverified
                    else if (subState === 'READY' && verificationState === '-1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-reverify").toggleClass("hide");
                    }
                    // State 4 - Failed & Verifying
                    else if (subState === 'FAILED' && verificationState === '0') {

                    }
                    // State 5 - Failed & Verified
                    else if (subState === 'FAILED' && verificationState === '1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-fmodify").toggleClass("hide");
                    }
                    // State 6 - Failed & Unverified
                    else if (subState === 'FAILED' && verificationState === '-1') {
                        $("#instance-fcancel").toggleClass("hide");
                        $("#instance-fretry").toggleClass("hide");
                        $("#instance-reverify").toggleClass("hide");
                    }
                }
            }

        </script>        
    </body>
</html>
