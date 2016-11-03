<!--
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 !-->

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
        <title>Manifest Portal</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
        <style>
            .manifest-list{
                /*list-style:none;*/
                padding-left: 1.5em;
            }
        </style>
    </head>
    <sql:setDataSource var="front_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/frontend"
                       user="front_view"  password="frontuser"/>
    <sql:query dataSource="${front_conn}" sql="SELECT S.name, I.creation_time, I.alias_name, X.super_state, V.verification_state FROM service S, service_instance I, service_state X, service_verification V
                           WHERE I.referenceUUID = ? AND I.service_instance_id = V.service_instance_id AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id" var="instance">
        <sql:param value="${param.uuid}" />
    </sql:query>

    <body>
        <!-- MAIN PANEL -->
        <div id="main-pane">
                 <table class="management-table" >
                    <thead>
                        <tr>
                            <th colspan="2"> Manifest Portal </th>
                        </tr>
                    </thead>
                    <tbody id="manifest_table_body" data-service-name='${instance.getRowsByIndex()[0][0]}'>
                    </tbody>
                 </table>
                                <script>
                                    /* ALERT CHECK IF MAP IS ARRAY
                                     * 
                                     * 
                                     * 
                                     * 
                                     * 
                                     * ALERT CHECK IF MAP IS ARRAY 
                                     */
                        function parseMap(map, container, container_type, base) {
                            for (var key in map) {
                                var string = "";
                                if (container_type === "table") {
                                    if (map.constructor !== Array) {
                                        string = $("<tr><td>" + key + "</td></tr>");
                                        container.append(string);
                                        var new_container = string;
                                     } else {
                                         var new_container = container;
                                     }
                                    var isMap = Object.keys(map[key]).length > 0;
                                    var isString = (typeof map[key]) === "string";
                                    if ((map[key].constructor === Array) && !isString) {
                                        var n1 = $("<td></td>");
                                        new_container.append(n1);
                                        //container.append(n1);
                                        var n3 = $("<ul class=\"manifest-list\" style=\"padding-left:.5em;\"></ul>");
                                        n1.append(n3);
                                        for (var i in map[key]) {
                                            if ((typeof map[key[i]]) === "string" ) {
                                                string += "<li><b> " + i + "</b>: " + map[key][i] + "</li>";
                                                container.append(string);   
                                                string = "";
                                            } else if (map[key][i].constructor === Array)   {
                                                parseMap(map[key][i], n3,"list", base);
                                            }
                                        }
                                    } else if (isMap  && !isString) {
                                        var n1 = $("<td></td>");
                                        new_container.append(n1);
                                        //container.append(n1);
                                        var n3 = $("<ul class=\"manifest-list\" style=\"padding-left:0;\"></ul>");
                                        n1.append(n3);

                                       parseMap(map[key], n3,"list", base);
                                    } else {
                                        string = $("<td>" + map[key] + "</td>");
                                        new_container.append(string);
                                    }
                                } else if (container_type === "list") {
                                    var isMap = Object.keys(map[key]).length > 0;
                                    var isString = (typeof map[key]) === "string";
                                    if ((map[key].constructor === Array || isMap) && !isString) {
                                        var n3 = $("<ul class=\"manifest-list\"></ul>");
                                        if (map[key].constructor === Array)  {
                                           // if ($.trim(key).length !== 1) { // hack will replace 
                                                var item = $("<li><b>"+key+"</b></li>");
                                                n3.append(item);
                                                //item.append(n3);
                                           // }
                                            container.append(n3);
                                        } else {
                                            if ($.trim(key).length !== 1) { // hack will replace 
                                                var item = $("<li><b>"+key+"</b></li>");
                                                //container.append(item);
                                                //item.append(n3);
                                                n3.append(item);
                                                container.append(n3);
                                            } else {
                                                var item = $("<li></li>");
                                                   item.append(n3);

                                                container.append(item);

                                                //container.append(n3);
                                            }
                                        }
                                        for (var i in map[key]) {
                                            if ((typeof map[key][i]) === "string" ) {// && ($.trim(map[key[i]]) !== '')) {
                                                string += "<li><b> " + i + "</b>: " + map[key][i] + "</li>";
                                                n3.append(string);    
                                                string = "";
                                            } else {
                                               // if ()
                                               //if ((map[key][i].constructor === Array && map[key][i].constructor.length > 1) || map[key][i].constructor === Object) {
                                                //if (!isNaN($.trim(key))) {
                                                    var bullet = $("<li></li>");
                                                    n3.append(bullet);
                                                
                                                    var newList = $("<ul class=\"manifest-list\"></ul>");
                                                    bullet.append(newList);
                                                //bullet.append(newList);
                                                    parseMap(map[key][i], newList,"list", base);
                                                //} else {
                                                 //  parseMap(map[key][i], n3,"list", base);

                                                //}
                                                // to set back just use N3
                                            }
                                        }                                        
                                    } else {
                                        if ($.trim(map[key]) !== '') {
                                            
                                            var n3 = $("<ul class=\"manifest-list\"></ul>");
                                           
                                            string = $("<li><b> " + key + "</b>: " + map[key] + "</li>");
                                            n3.append(string);
                                            container.append(n3);
                                            //container.append(string);                                        
                                        }
                                    }                                 
                                }                        
                            }
                        }
                        
                        var UUID = location.search.split("?uuid=")[1];
                        var serviceName = $("#manifest_table_body").attr("data-service-name");
                        
                        switch (serviceName){
                            case "Dynamic Network Connection":
                                loadManifest("dnc-manifest-template.xml");
                                break;
                            case "Virtual Cloud Network":
                                $.get( {
                                    url: "/VersaStack-web/restapi/service/property/" + UUID + "/host/", 
                                    success: function( data ) {
                                        if (data === "ops") {
                                            loadManifest("vcn-ops-manifest-template.xml");
                                        } else if (data === "aws") {
                                            loadManifest("vcn-aws-manifest-template.xml");
                                        } 
                                    }, 
                                    dataType: "text" 
                                  });
                                break;
                            case "Advanced Hybrid Cloud":
                                loadManifest("ahc-manifest-template.xml");
                                break;
                        }
                        loadManifest();
                        function loadManifest(templateURL) {
                            $.get( {
                                url: "/VersaStack-web/data/xml/manifest-templates/" + templateURL, 
                               // url: "/VersaStack-web/data/xml/manifest-templates/"    + "manifest-ahc1.json",
                                success: function( data ) {
//                                     var manifest = JSON.parse(data.jsonTemplate);
//                                     var name = Object.keys(manifest)[0];
//                                     manifest = manifest[name];
//                                     var baseTable =  $("#manifest_table_body");
//                                     baseTable.append("<tr><td colspan=\"2\"><b>" + name + "</b><br/><b>UUID</b>: " + data.serviceUUID  +"</td></tr>");
//                                    parseMap(manifest, baseTable, "table", baseTable);

                                    var template = data; //JSON.parse($(data).find("jsonTemplate").text());
                                    //alert(JSON.stringify(dnc_template));
                                    $.ajax({
                                        type: "POST",
                                        crossDomain: true,
                                        url: "/VersaStack-web/restapi/service/manifest/" + UUID,
                                        data: template,
                                        headers: { 
                                            'Accept': 'application/json',
                                            'Content-Type': 'application/xml'
                                        },
                                        success: function(data,  textStatus,  jqXHR ) {
                                            var manifest = JSON.parse(data.jsonTemplate);
                                            var name = Object.keys(manifest)[0];
                                            manifest = manifest[name];
                                        var baseTable =  $("#manifest_table_body");
                                             baseTable.append("<tr><td colspan=\"2\"><b>" + name + "</b><br/><b>UUID</b>: " + data.serviceUUID  +"</td></tr>");

                                            parseMap(manifest, baseTable, "table");
                                        },
                                        error: function(jqXHR, textStatus, errorThrown ) {
                                            var exceptionObj = JSON.parse(jqXHR.responseText);
                                            alert("exception: " + exceptionObj.exception);
                                        },

                                        dataType: "json"
                                    });
                                }, 
                                dataType: "text" 
                              });
                                                          //    dataType: "json" 
                    }     
                          
               </script>

        </div>
    </body>
</html>
