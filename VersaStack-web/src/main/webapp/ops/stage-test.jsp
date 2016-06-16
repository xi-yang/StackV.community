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
        <title>Staging Test</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>
        <script src="/VersaStack-web/js/svc/netcreate.js"></script>
        
        <!-- jQuery easing plugin -->
        <script src="http://thecodeplayer.com/uploads/js/jquery.easing.min.js" type="text/javascript"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
    </head>

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

    <body>
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
            <!-- Multistep form -->
            <form id="msform">
                <!-- Progress Bar -->
                <ul id="progressbar">
                    <li class="active">Service Host</li>
                    <li>Network</li>
                    <li>Subnets</li>
                    <li>VMs</li>
                    <li>Gateways</li>
                    <li>Summary</li>
                </ul>

                <!-- Stage 1: Host --> 
                <fieldset id='1-base-start'>
                    <h2 class="fs-title">Select your Host</h2>
                    <h3 class="fs-subtitle"></h3>
                    <input type="button" name="type" class="type-select action-button" value="AWS" />
                    <input type="button" name="type" class="type-select action-button" value="Openstack" />
                </fieldset>

                <!-- AWS -->
                <!-- Stage 2: Network -->
                <fieldset id='2-AWS-start'>
                    <h2 class="fs-title">Network Description</h2>
                    <h3 class="fs-subtitle">Basic Network Details</h3>
                    <table class="fs-table" id="awsStage2-table">
                        <tr>
                            <td>Instance Alias</td>
                            <td><input type="text" name="alias" required /></td>
                        </tr>
                        <tr>
                            <td>Topology URI</td>
                            <td>
                                <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/AwsDriver'" var="driverlist" />
                                <select name="topoUri" required>
                                    <option></option>
                                    <c:forEach var="driver" items="${driverlist.rows}">
                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                    </c:forEach>
                                </select>       
                            </td>
                        </tr>
                        <tr>
                            <td>Network Type</td>
                            <td><input type="text" name="netType" required/></td>
                        </tr>
                        <tr>
                            <td>Network CIDR</td>
                            <td><input type="text" name="netCidr" required/></td>
                        </tr>
                    </table>
                    <input type="button" name="previous" class="reset action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>
                                
                <!-- Stage 3: Subnets -->                    
                <fieldset id="3-aws">
                    <h2 class="fs-title">Subnets</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="awsStage3-count1" /></h3>
                    <h3 class="fs-subtitle">How many routes for each? <i>(Blank inputs are acceptable)</i><input type="number" class="small-counter" id="awsStage3-count2" /></h3>
                    <input type="button" name="previous" class="reset action-button" value="Previous" />
                    <input type="button" name="next" id="awsStage3" class="field-next action-button" value="Next" />
                </fieldset>                                                             
                <fieldset>
                    <fieldset class="subfs" id="awsStage3-subfs">
                        
                    </fieldset>
                    
                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>
                                
                                
                                
                                
                                

                <!-- Type 2 -->
                <fieldset id='2-Openstack-start'>
                    <h2 class="fs-title">Specify Form Fields</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?</h3>
                    <div><input type="number" id="opsStage1-count" /></div>
                    <input type="button" name="previous" class="reset action-button" value="Previous" />
                    <input type="button" name="next" id="opsStage1" class="field-next action-button" value="Next" />
                </fieldset>
                <fieldset>
                    <h2 class="fs-title">Field Details</h2>
                    <h3 class="fs-subtitle"></h3>
                    <div id='opsStage1-block'>

                    </div>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>
            </form>

        </div>
        <!-- TAG PANEL -->       
        <div id="tag-panel"> 
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                $("#tag-panel").load("/VersaStack-web/tagPanel.jsp", null);
            });
        </script>        
    </body>
</html>
