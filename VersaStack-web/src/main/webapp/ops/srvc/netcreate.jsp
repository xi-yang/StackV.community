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
<html>    
    <head>   
        <meta charset="UTF-8">
        <title>Network Creation Service</title>
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
            <c:choose>                  
                <c:when test="${empty param.ret}">  <!-- Display this section when no return value supplied -->

                    <div id="service-specific">
                        <div id="service-top">
                            <div id="service-menu">
                                <c:if test="${not empty param.self}">
                                    <button type="button" id="button-service-return">Cancel</button>
                                </c:if>
                                <table class="management-table">
                                    <thead>
                                        <tr>
                                            <th>Network Creation</th>
                                            <th>
                                                <select name="networkType" onchange="networkSelect(this)">                                                
                                                    <option value=""></option>
                                                    <option value="aws">AWS</option>
                                                    <option value="ops">OpenStack</option>
                                                </select>
                                            </th>
                                        </tr>
                                    </thead>
                                </table>
                            </div>
                        </div>

                        <div id="service-bottom">                            
                            <div id="service-fields">                                                               
                                <table class="management-table" id="net-template-form" style="margin-bottom: 0px;"> 
                                    <thead>
                                        <tr>
                                            <th>Templates</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:if test="${param.networkType == 'aws'}">
                                            <tr>
                                                <td>Basic Cloud</td>
                                                <td><button onclick="applyNetTemplate(1)">Apply</button></td>
                                            </tr>                                            
                                            <tr>
                                                <td>Basic VM in each subnet and Direct Connection</td>
                                                <td><button onclick="applyNetTemplate(2)">Apply</button></td>
                                            </tr>
                                            <tr>
                                                <td>Various VM Types</td>
                                                <td><button onclick="applyNetTemplate(3)">Apply</button></td>
                                            </tr>
                                        </c:if>
                                        <c:if test="${param.networkType == 'ops'}">    
                                            <tr>
                                                <td>Basic VM</td>
                                                <td><button onclick="applyNetTemplate(4)">Apply</button></td>
                                            </tr>
                                            <tr>
                                                <td>VM with type details and SRIOV connection</td>
                                                <td><button onclick="applyNetTemplate(5)">Apply</button></td>
                                            </tr>
                                        </c:if>
                                    </tbody>
                                </table>    

                                <form id="custom-form" action="/VersaStack-web/ServiceServlet" method="post">
                                    <input type="hidden" name="username" value="${user.getUsername()}"/>
                                    <input type="hidden" name="driverType" value="${param.networkType}"/>  
                                    <table class="management-table" id="net-custom-form">
                                        <thead>
                                            <tr>
                                                <th>Service Alias</th>
                                                <th>
                                                    <input class="header-input" type="text" name="alias" required />
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody id="custom-fields">
                                            <!-- AWS -->
                                            <c:if test="${param.networkType == 'aws'}">
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
                                                <!-- <tr>
                                                    <td>Route Table</td>
                                                    <td>
                                                        <div id="route-block">
                                                            <div>
                                                                <input type="text" name="route1-from" placeholder="From"/>
                                                                <input type="text" name="route1-to" placeholder="To"/>
                                                                <input type="text" name="route1-next" placeholder="Next Hop"/>
                                                            </div>
                                                        </div>
                                                        <div>
                                                            <input type="checkbox" name="route-prop" value="true"/>   Enable VPN Routes Propogation                                                       
                                                        </div>
                                                        <div>
                                                             <input class="button-register" type="button" 
                                                                   value="Add Route" onClick="addRoute()">
                                                        </div>
                                                    </td>
                                                </tr> -->
                                                <tr id="subnet1">
                                                    <td>Subnet 1</td>
                                                    <td>
                                                        <div>
                                                            <input type="text" name="subnet1-name" placeholder="Name"/>
                                                            <input type="text" name="subnet1-cidr" placeholder="CIDR Block"/>
                                                            <div id="subnet1-route-block">
                                                                <div>
                                                                    <input type="text" name="subnet1-route1-from" placeholder="From"/>
                                                                    <input type="text" name="subnet1-route1-to" placeholder="To"/>
                                                                    <input type="text" name="subnet1-route1-next" placeholder="Next Hop"/>
                                                                </div>
                                                            </div>
                                                            <div>
                                                                <input type="checkbox" name="subnet1-route-prop" value="true"/>   Enable VPN Routes Propagation
                                                            </div>
                                                            <div>
                                                                <input class="button-register" id="subnet1-route" type="button" value="Add Route" onClick="addSubnetRoute(this.id)">
                                                            </div>
                                                            <div id="subnet1-vm-block">
                                                                <br>
                                                                <table id="subnet1-vm1-table">                                                                    
                                                                    <tbody>
                                                                        <tr>
                                                                            <td>VM Name</th>
                                                                            <td><input type="text" name="subnet1-vm1"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td><input type="text" name="subnet1-vm1-keypair" placeholder="Keypair Name"></td>
                                                                            <td><input type="text" name="subnet1-vm1-security" placeholder="Security Name"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td><input type="text" name="subnet1-vm1-image" placeholder="Image Type"></td>
                                                                            <td><input type="text" name="subnet1-vm1-instance" placeholder="Instance Type"></td>
                                                                        </tr>                                                                        
                                                                    </tbody>                                                                    
                                                                </table>
                                                            </div>
                                                            <div>
                                                                <input class="button-register" id="subnet1-vm" type="button" value="Add VM" onClick="addVM('aws', this.id)">
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>Direct Connect</td>
                                                    <td><input type="text" name="conn-dest" placeholder="Destination" size="60" /><input type="text" name="conn-vlan" placeholder="VLAN" size="8" /></td>
                                                </tr>
                                                <tr>
                                                    <td></td>
                                                    <td><input type="submit" name="custom" value="Submit" /><input class="button-register" type="button" value="Add Subnet" onClick="addSubnet('aws')"></td>
                                                </tr>
                                            </c:if>

                                            <!-- OpenStack -->
                                            <c:if test="${param.networkType == 'ops'}">
                                                <tr>
                                                    <td>Topology URI</td>
                                                    <td>
                                                        <sql:query dataSource="${rains_conn}" sql="SELECT topologyUri FROM driver_instance WHERE driverEjbPath='java:module/OpenStackDriver'" var="driverlist" />
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
                                                <!-- <tr>
                                                    <td>Route Table</td>
                                                    <td>
                                                        <div id="route-block">
                                                            <div>
                                                                <input type="text" name="route1-from" placeholder="From"/>
                                                                <input type="text" name="route1-to" placeholder="To"/>
                                                                <input type="text" name="route1-next" placeholder="Next Hop"/>
                                                            </div>
                                                        </div>
                                                        <div>
                                                            <input type="checkbox" name="route-prop" value="true"/>   Enable VPN Routes Propogation                                                       
                                                        </div>
                                                        <div>
                                                             <input class="button-register" type="button" 
                                                                   value="Add Route" onClick="addRoute()">
                                                        </div>
                                                    </td>
                                                </tr> -->
                                                <tr id="subnet1">
                                                    <td>Subnet 1</td>
                                                    <td>
                                                        <div>
                                                            <input type="text" name="subnet1-name" placeholder="Name"/>
                                                            <input type="text" name="subnet1-cidr" placeholder="CIDR Block"/>
                                                            <div id="subnet1-route-block">
                                                                <div>
                                                                    <input type="text" name="subnet1-route1-from" placeholder="From"/>
                                                                    <input type="text" name="subnet1-route1-to" placeholder="To"/>
                                                                    <input type="text" name="subnet1-route1-next" placeholder="Next Hop"/>
                                                                </div>
                                                            </div>
                                                            <div>
                                                                <input type="checkbox" name="subnet1-route-default" value="true"/>   Enable Default Routing
                                                            </div>
                                                            <div>
                                                                <input class="button-register" id="subnet1-route" type="button" value="Add Route" onClick="addSubnetRoute(this.id)">
                                                            </div>
                                                            <div id="subnet1-vm-block">
                                                                <br>
                                                                <table id="subnet1-vm1-table">                                                                    
                                                                    <tbody>
                                                                        <tr>
                                                                            <td>VM Name</th>
                                                                            <td><input type="text" name="subnet1-vm1"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td><input type="text" name="subnet1-vm1-keypair" placeholder="Keypair Name"></td>
                                                                            <td><input type="text" name="subnet1-vm1-security" placeholder="Security Name"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td><input type="text" name="subnet1-vm1-image" placeholder="Image Type"></td>
                                                                            <td><input type="text" name="subnet1-vm1-instance" placeholder="Instance Type"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td><input type="text" name="subnet1-vm1-host" placeholder="VM Host"></td>
                                                                            <td><input type="text" name="subnet1-vm1-floating" placeholder="Floating IP"></td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td>SRIOV</td>
                                                                            <td>
                                                                                <div id="subnet1-vm1-sriov-block">
                                                                                    <div>
                                                                                        <input type="text" name="subnet1-vm1-sriov1-dest" placeholder="SRIOV Destination">
                                                                                        <input type="text" name="subnet1-vm1-sriov1-mac" placeholder="SRIOV MAC Address">
                                                                                        <input type="text" name="subnet1-vm1-sriov1-ip" placeholder="SRIOV IP Address">
                                                                                    </div>
                                                                                </div>
                                                                                <div>
                                                                                    <input class="button-register" id="subnet1-vm1-sriov" type="button" value="Add SRIOV" onClick="addSRIOV(this.id)">
                                                                                </div>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td>Routes</td>
                                                                            <td>
                                                                                <div id="subnet1-vm1-route-block">
                                                                                    <div>
                                                                                        <input type="text" name="subnet1-vm1-route1-from" placeholder="From"/>
                                                                                        <input type="text" name="subnet1-vm1-route1-to" placeholder="To"/>
                                                                                        <input type="text" name="subnet1-vm1-route1-next" placeholder="Next Hop"/>
                                                                                    </div>                                                                                
                                                                                </div>
                                                                                <div>
                                                                                    <input class="button-register" id="subnet1-vm1-route" type="button" value="Add VM Route" onClick="addVMRoute(this.id)">
                                                                                </div>
                                                                            </td>
                                                                        </tr>
                                                                    </tbody>                                                                    
                                                                </table>
                                                            </div>
                                                            <div>
                                                                <input class="button-register" id="subnet1-vm" type="button" value="Add VM" onClick="addVM('ops', this.id)">
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <!--                                                <tr>
                                                                                                    <td>Direct Connect</td>
                                                                                                    <td><input type="text" name="conn-dest" placeholder="Destination" size="60" /><input type="text" name="conn-vlan" placeholder="VLAN" size="8" /></td>
                                                                                                </tr>-->
                                                <tr>
                                                    <td></td>
                                                    <td><input type="submit" name="custom" value="Submit" /><input class="button-register" type="button" value="Add Subnet" onClick="addSubnet('ops')"></td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>                                    
                                    <input type="hidden" name="netCreate" value="true"/>
                                </form>
                            </div>
                        </div>


                    </div> 
                </c:when>

                <c:otherwise>                       <!-- Display this section when return value supplied -->
                    <div class="form-result" id="service-result">
                        <c:choose>
                            <c:when test="${param.ret == '0'}">
                                Creation Success!
                            </c:when>
                            <c:when test="${param.ret == '1'}">
                                Connection Error.
                            </c:when>    
                            <c:when test="${param.ret == '2'}">
                                Back-end Interaction Error.
                            </c:when>    
                            <c:when test="${param.ret == '3'}">
                                Failed to Create Network.
                            </c:when>                                      
                        </c:choose>                        

                        <br><a href="/VersaStack-web/ops/srvc/netcreate.jsp?self=true">Create Another Network.</a>                                
                        <br><a href="/VersaStack-web/ops/catalog.jsp">Return to Services.</a>
                        <br><a href="/VersaStack-web/orch/graphTest.jsp">Return to Graphic Orchestration.</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>    
    </body>
</html>
