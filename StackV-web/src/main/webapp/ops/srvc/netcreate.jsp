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
        <title>Virtual Cloud Network Service</title>
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/StackV-web/css/style.css">
    </head>

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

    <body>
        <!-- NAV BAR -->
        <div id="nav"></div>
        <!-- MAIN PANEL -->
        <div id="black-screen"></div>
        <div id="main-pane">
            <!-- Multistep form -->
            <form action="/StackV-web/ServiceServlet" method="post" class="stageform" id="msform" onsubmit="return validateVCN()">
                <input type="hidden" name="username" value="${sessionStorage.username}"/>
                <input type="hidden" name="netCreate" value="true"/>
                <!-- Progress Bar -->
                <ul class="vcn-progress" id="progressbar">
                    <li class="disabled active">Service Host</li>
                    <li class="disabled">Network</li>
                    <li class="disabled">Subnets</li>
                    <li class="disabled">VMs</li>
                    <li class="disabled">Gateways</li>
                    <li class="disabled">SRIOV</li>
                    <li class="disabled">Summary</li>
                </ul>

                <fieldset class="active-fs" id="0-template-select" style="z-index: 4;">
                    <div><button type="button" class="action-button" onclick="applyTemplate(0)">Start from Scratch</button></div>
                    <h3 class="fs-title">Templates</h3>
                    <div><button type="button" class="action-button" onclick="applyTemplate(1)">Basic AWS</button></div>
                    <div><button type="button" class="action-button" onclick="applyTemplate(2)">AWS w/ VMs</button></div>
                    <div><button type="button" class="action-button" onclick="applyTemplate(3)">Basic OpenStack</button></div>
                </fieldset>

                <!-- Stage 1: Host -->
                <fieldset id='1-base-1'>
                    <h2 class="fs-title">Select your Host</h2>
                    <h3 class="fs-subtitle"></h3>
                    <button type="button" name="type" class="stage1-next action-button" value="aws">AWS</button>
                    <button type="button" name="type" class="stage1-next action-button" value="ops">Openstack</button>
                </fieldset>

                <!-- AWS -->
                <!-- Stage 2: Network -->
                <fieldset id='2-aws-1'>
                    <h2 class="fs-title">Network Description</h2>
                    <h3 class="fs-subtitle">Basic Network Details</h3>
                    <table class="fs-table">
                        <thead id="awsStage2-base">

                       </thead>
                        <tr>
                            <td>
                                <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/AwsDriver'" var="driverlist" />
                                <select name="topoUri" >
                                    <option selected disabled value="test">Choose the driver topology URI</option>
                                    <c:forEach var="driver" items="${driverlist.rows}">
                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                    </c:forEach>
                                </select>
                            </td>
                        </tr>
                        <tbody id="awsStage2-network">

                        </tbody>
                    </table>
                    <input type="button" name="previous" class="reset action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 3: Subnets -->
                <fieldset id="3-aws-1">
                    <h2 class="fs-title">Subnets</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="awsStage3-subnet" onfocus="this.oldvalue = this.value;" onchange="setSubnets(this)"/></h3>
                    <table class="subfs-table" id="awsStage3-subnet-route-table">

                    </table>
                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="awsStage3" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="3-aws-2">
                    <fieldset class="subfs" id="awsStage3-subnet-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 4: VMs -->
                <fieldset id="4-aws-1">
                    <h2 class="fs-title">Virtual Machines</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="awsStage4-vm" onfocus="this.oldvalue = this.value;" onchange="setVMs(this)"/></h3>
                    <table class="subfs-table" id="awsStage4-vm-route-table">

                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="awsStage4" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="4-aws-2">
                    <fieldset class="subfs" id="awsStage4-vm-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 7: Summary -->
                <fieldset id="7-aws-1">
                    <h2 class="fs-title">Final Submission</h2>

                    <br>
                    <table class="subfs-table" id="profile-table">
                        <thead>
                            <tr>
                                <td><label class="profile-save-label" id="profile-save-label-aws">Save as Profile <input type="checkbox" class="profile-save-check" id="profile-save-check-aws" name="profile-save" /></label></td>
                            </tr>
                        </thead>
                        <tbody class="fade-hide" class="profile-save-body" id="profile-save-body-aws">
                            <tr>
                                <td><input type="text" name="profile-name" placeholder="Profile Name" /></td>
                                <td><input type="text" name="profile-description" placeholder="Profile Description" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="submit" name="save" class="profile-save-button action-button" value="Save" />
                    <button type="submit" name="submit" class="action-button" value="submit">Submit</button>
                </fieldset>


                <!-- Openstack -->
                <!-- Stage 2: Network -->
                <fieldset id='2-ops-1'>
                    <h2 class="fs-title">Network Description</h2>
                    <h3 class="fs-subtitle">Basic Network Details</h3>
                    <table class="fs-table">
                        <thead id="opsStage2-base">

                        </thead>
                        <tr>
                            <td>
                                <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/OpenStackDriver'" var="driverlist" />
                                <select name="topoUri" >
                                    <option selected disabled value="test">Choose the driver topology URI</option>
                                    <c:forEach var="driver" items="${driverlist.rows}">
                                        <option value="${driver.topologyUri}">${driver.topologyUri}</option>
                                    </c:forEach>
                                </select>
                            </td>
                        </tr>
                        <tbody id="opsStage2-network">

                        </tbody>
                    </table>
                    <input type="button" name="previous" class="reset action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 3: Subnets -->
                <fieldset id="3-ops-1">
                    <h2 class="fs-title">Subnets</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage3-subnet" onfocus="this.oldvalue = this.value;" onchange="setSubnets(this)"/></h3>
                    <table class="subfs-table" id="opsStage3-subnet-route-table">

                    </table>
                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="opsStage3" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="3-ops-2">
                    <fieldset class="subfs" id="opsStage3-subnet-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 4: VMs -->
                <fieldset id="4-ops-1">
                    <h2 class="fs-title">Virtual Machines</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage4-vm" onfocus="this.oldvalue = this.value;" onchange="setVMs(this)"/></h3>
                    <table class="subfs-table" id="opsStage4-vm-route-table">

                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="opsStage4" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="4-ops-2">
                    <fieldset class="subfs" id="opsStage4-vm-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 5: Gateways -->
                <fieldset id="5-ops-1">
                    <h2 class="fs-title">Gateways</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage5-gateway" onfocus="this.oldvalue = this.value;" onchange="setGateways(this)"/></h3>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="opsStage5" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="5-ops-2">
                    <fieldset class="subfs" id="opsStage5-gateway-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 6: SRIOV -->
                <fieldset id="6-ops-1">
                    <h2 class="fs-title">SRIOV</h2>
                    <h3 class="fs-subtitle">How many do you wish to include?<input type="number" class="small-counter" id="opsStage6-sriov" onfocus="this.oldvalue = this.value;" onchange="setSRIOV(this)"/></h3>
                    <table class="subfs-table" id="opsStage6-sriov-route-table">

                    </table>
                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" id="opsStage6" class="next action-button" value="Next" />
                </fieldset>
                <fieldset id="6-ops-2">
                    <fieldset class="subfs" id="opsStage6-sriov-fs">

                    </fieldset>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="button" name="next" class="next action-button" value="Next" />
                </fieldset>

                <!-- Stage 7: Summary -->
                <fieldset id="7-ops-1">
                    <h2 class="fs-title">Final Submission</h2>

                    <br>
                    <table class="subfs-table" id="profile-table">
                        <thead>
                            <tr>
                                <td><label class="profile-save-label" id="profile-save-label-ops">Save as Profile <input type="checkbox" class="profile-save-check" id="profile-save-check-ops" name="profile-save" /></label></td>
                            </tr>
                        </thead>
                        <tbody class="fade-hide" id="profile-save-body-ops" class="profile-save-body">
                            <tr>
                                <td><input type="text" name="profile-name" placeholder="Profile Name" /></td>
                                <td><input type="text" name="profile-description" placeholder="Profile Description" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <input type="button" name="previous" class="previous action-button" value="Previous" />
                    <input type="submit" name="save" class="profile-save-button action-button" value="Save" />
                    <button type="submit" name="submit" class="action-button" value="submit">Submit</button>
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
        <script src="/StackV-web/js/keycloak.js"></script>
        <script src="/StackV-web/js/jquery/jquery.js"></script>
        <script src="/StackV-web/js/bootstrap.js"></script>
        <script src="/StackV-web/js/nexus.js"></script>
        <script src="/StackV-web/js/svc/netcreate.js"></script>
        <!-- jQuery easing plugin -->
        <script src="http://thecodeplayer.com/uploads/js/jquery.easing.min.js" type="text/javascript"></script>
        <script>
            $(function () {
                $("#tag-panel").load("/StackV-web/tagPanel.jsp", null);
            });
        </script>
    </body>
</html>
