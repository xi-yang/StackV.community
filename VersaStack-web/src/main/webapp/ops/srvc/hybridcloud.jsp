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
        <title>Hybrid Cloud Service</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>
        <script src="/VersaStack-web/js/svc/hybridcloud.js"></script>
        <script src="../../js/svc/hybridcloud.js" type="text/javascript"></script>
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
        <div id="nav"></div>
        <!-- SIDE BAR -->
        <div id="sidebar"></div>
        <!-- MAIN PANEL -->
        <div id="black-screen"></div>
        <div id="main-pane">           
            <!-- Multistep form -->
            <form action="/VersaStack-web/ServiceServlet" method="post" class="stageform" id="msform" onsubmit="return validateHybrid()">
                <input type="hidden" name="username" value="${user.getUsername()}"/>
                <input type="hidden" name="hybridCloud" value="true"/>
                <!-- Progress Bar -->
                <ul class="hc-progress" id="progressbar">
                    <li class="disabled active">Hybrid Clouds</li>
                    <li>Network</li>
                    <li>Subnets</li>                    
                    <li>Gateways</li>
                    <li>VMs</li>
                    <li>SRIOV</li>                    
                    <li>Summary</li>
                </ul>

                <fieldset class="active-fs" id="0-1" style="z-index: 4;">
                    <div><button type="button" class="action-button" onclick="applyTemplate(0)">Start from Scratch</button></div>
                    <h3 class="fs-title">Templates</h3>
                    <div><button type="button" class="action-button" onclick="applyTemplate(1)">Basic Hybrid Cloud</button></div>
                </fieldset>

                <!-- Stage 1: Hybrid Clouds --> 
                <fieldset id='1-base-1'>                   
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>

                <!-- Stage 2: Network -->
                <fieldset id='2-1'>
                    <h2 class="fs-title">Hybrid Cloud Information</h2>                    
                    <table class="fs-table">
                        <tr>
                            <td><input type="text" name="alias" placeholder="Instance Alias" /></td>
                        </tr>
                    </table>
                    <br>
                    <h2 class="fs-title">AWS Network Description</h2>
                    <h3 class="fs-subtitle">Basic Network Details</h3>
                    <table class="fs-table">
                        <tr>
                            <td>
                                <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/AwsDriver'" var="driverlist" />
                                <select name="aws-topoUri" >
                                    <option selected disabled value="test">Choose the driver topology URI</option>
                                    <c:forEach var="driver" items="${driverlist.rows}">
                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                    </c:forEach>
                                </select>        
                            </td>
                        </tr> 
                        <tbody id="awsStage2-network">
                            <tr>
                                <td><input type="text" name="aws-netCidr" placeholder="Network CIDR" /></td>
                                <td><input type="text" name="aws-conn-vlan" placeholder="Direct Connect VLAN" /></td>
                            </tr>
                        </tbody>
                    </table>
                    <br>
                    <h2 class="fs-title">OpenStack Network Description</h2>
                    <h3 class="fs-subtitle">Basic Network Details</h3>
                    <table class="fs-table">
                        <tr>
                            <td>
                                <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/OpenStackDriver'" var="driverlist" />
                                <select name="ops-topoUri" >
                                    <option selected disabled value="test">Choose the driver topology URI</option>
                                    <c:forEach var="driver" items="${driverlist.rows}">
                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                    </c:forEach>
                                </select>        
                            </td>
                        </tr>
                        <tbody id="opsStage2-network">
                            <tr>
                                <td><input type="text" name="ops-netCidr" placeholder="Network CIDR" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>



                <!-- Stage 3: Subnets -->                    
                <fieldset id="3-1">
                    <h2 class="fs-title">AWS Subnets</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="awsStage3-subnet" onfocus="this.oldvalue = this.value;" onchange="setSubnets(this)"/></h3>
                    <table class="subfs-table" id="awsStage3-subnet-route-table">

                    </table>
                    <br>
                    <h2 class="fs-title">OpenStack Subnets</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage3-subnet" onfocus="this.oldvalue = this.value;" onchange="setSubnets(this)"/></h3>
                    <table class="subfs-table" id="opsStage3-subnet-route-table">

                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>                                                             


                <fieldset id="3-2">
                    <fieldset class="subfs" id="awsStage3-subnet-fs">

                    </fieldset>
                    <br>
                    <fieldset class="subfs" id="opsStage3-subnet-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>
                
                <!-- Stage 4: Gateways -->
                <fieldset id="4-1">
                    <h2 class="fs-title">OpenStack Gateways</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage4-gateway" onfocus="this.oldvalue = this.value;" onchange="setGateways(this)"/></h3>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>  


                <fieldset id="4-2">
                    <fieldset class="subfs" id="opsStage4-gateway-fs">
                        
                    </fieldset>        

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>

                <!-- Stage 5: VMs --> 
                <fieldset id="5-1">
                    <h2 class="fs-title">AWS Virtual Machines</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="awsStage5-vm" onfocus="this.oldvalue = this.value;" onchange="setVMs(this)"/></h3>
                    <table class="subfs-table" id="awsStage5-vm-route-table">

                    </table>
                    <br>
                    <h2 class="fs-title">OpenStack Virtual Machines</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage5-vm" onfocus="this.oldvalue = this.value;" onchange="setVMs(this)"/></h3>
                    <table class="subfs-table" id="opsStage5-vm-route-table">

                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>
                <fieldset id="5-2">
                    <fieldset class="subfs" id="awsStage5-vm-fs">

                    </fieldset>
                    <br>
                    <fieldset class="subfs" id="opsStage5-vm-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>

                <!-- Stage 6: SRIOV -->                
                <fieldset id="6-1">
                    <fieldset class="subfs" id="opsStage6-sriov-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />                    
                </fieldset>

                <!-- Stage 7: Summary -->
                <fieldset id="7-1">
                    <h2 class="fs-title">Final Submission</h2>

                    <br>
                    <table class="subfs-table" id="profile-table">
                        <thead>
                            <tr>
                                <td><label id="profile-save-label">Save as Profile <input type="checkbox" id="profile-save-check" name="profile-save" /></label></td>
                            </tr>
                        </thead>
                        <tbody class="fade-hide" id="profile-save-body">
                            <tr>
                                <td><input type="text" name="profile-name" placeholder="Profile Name" /></td>
                                <td><input type="text" name="profile-description" placeholder="Profile Description" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="save" class="profile-save-button action-button" value="Save" />
                    <button type="submit" name="submit" class="action-button" value="ops">Submit</button>  
                </fieldset>
            </form>      
            <div id="info-panel">
                <h3 class="fs-subtitle" id="info-panel-title"></h3>
                <div id="info-panel-div">

                </div>
            </div>
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
