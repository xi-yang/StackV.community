
/* global XDomainRequest, baseUrl, keycloak, TweenLite, Power2, Mousetrap */

var ipaApiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/request";

/**
 * Currently, just logs in the user in order to maintain a fresh cookie. Called
 * before any call involving the IPA server.
 * @returns {jqXHR}
 */
function ipaLogin(){
    var apiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/login";
    
    return $.ajax({
        url: apiUrl,
        type: 'POST',
        beforeSend: function (xhr) {
            // check here if the user is already logged in and the cookie did not expire
            xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
        },
        success: function(result) {
            //console.log("ipaLogin success: " + JSON.stringify(result));
            if (result["Result"] === "Login Successful") {
                return true;
            } else {
                return false;
            }
        },
        error: function(err) {
            console.log("ipaLogin error: " + JSON.stringify(err));
        }
    });
}


/**
 * Creates the UserGroup for the specified service with given group name
 * @param {string} groupName
 * @param {string} desc 
 * @returns {jqXHR}
 */
function createUserGroup(groupName, desc) {   
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_add",
        "params":[
            [groupName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds specified users to specified userGroup
 * @param {type} users
 * @param {type} userGroup
 * @returns {jqXHR}
 */
function addUsersToUserGroup(users,userGroup) {
    // if only a single user is provided - make sure it is enclosed in an array
    if (!Array.isArray(users)) {
        users = [users];
    }
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_add_member",
        "params":[
            [userGroup],
            {"user": users}
        ],
        "id":0
    };
        
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Creates/adds a single host to the IPA server. Host must be valid.
 * @param {type} hostName
 * @returns {undefined}
 */
function createHost(hostName) {
    // creating the IPA request
    // host must be valid (even if not registered with the IPA server or have FreeIPA installed)
    // otherwise this error may show up: "Host 'hostName' does not have corresponding DNS A/AAAA record"
    // Host must unique so duplicate host names are not allowed, will return an error if a duplicate entry is added
    var ipaRequestData = {
        "method":"host_add",
        "params":[
            [hostName],
            {}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Creates the HostGroup for the specified service with given group name
 * @param {type} groupName
 * @param {type} desc
 * @returns {jqXHR}
 */
function createHostGroup(groupName, desc) {
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_add",
        "params":[
            [groupName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds host(s) to a host group
 * @param {type} hosts
 * @param {type} hostGroupName
 * @returns {jqXHR}
 */
function addHostsToHostGroup(hosts, hostGroupName) {
    if (!Array.isArray(hosts)) {
        hosts = [hosts];
    }
    
    var badHosts = [];
    // before adding the hosts - make sure each one exists by creating them
    hosts.forEach(function(h) {        
        $.when(createHost(h)).done(function(hRes) {            
            
            /**
             * if error is null (in which result will not be null) then the new host
             * was successfully added. In the event that error is not null (indicating
             * an error) then check the error code.
             */
            if ( hRes["error"] !== null && hRes["error"]["code"] !== 4002){
                // error code 4002 indicates a duplicate entry (which is
                // fine since the host might already be on the IPA server) and 
                // we can ignore the error.
                // But if the error is not code 4002, then some other error
                // showed up and should be noted
                badHosts.push(h);                
            } else {
                console.log("new host added: " + h);
            }        
        }); 
    });
    
    if (badHosts.length !== 0) {
        return {"BadHosts":badHosts};
    }
    
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_add_member",
        "params":[
            [hostGroupName],
            {"host":hosts}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Gets hosts for the specified service instance
 * @param {String} serviceUUID
 * @returns {jqXHR}
 */
function getHostsForServiceInstance(serviceUUID) {
    var apiUrl = baseUrl + '/StackV-web/restapi/service/manifest/' + serviceUUID;
    
    // ajax call fields
    var settings = {
        "async": true,
        "crossDomain": true,
        "url": apiUrl,
        "method": "POST",
        "headers": {
          "Content-Type": "application/xml",
          "Authorization": "bearer " + keycloak.token
        },
        "data": "<serviceManifest>\n<serviceUUID></serviceUUID>\n<jsonTemplate>\n{\n    \"hostgroup\": [\n        {\n          \"hostname\": \"?fqdn?\",\n          \"sparql\": \"SELECT DISTINCT ?fqdn WHERE {?hypervisor mrs:providesVM ?vm. ?vm mrs:hasNetworkAddress ?na. ?na mrs:type \\\"fqdn\\\". ?na mrs:value ?fqdn.}\",\n          \"required\": \"false\"\n        }\n      ]\n}\n</jsonTemplate>\n</serviceManifest>"
      };
    
    return $.ajax(settings);
}

/**
 * Creates an HBAC rule give the ruleName and description
 * @param {type} ruleName
 * @param {type} desc
 * @returns {jqXHR}
 */
function createHBACRule(ruleName, desc) {
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add",
        "params":[
            [ruleName],
            {"description": desc}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds a user group to a HBACRule
 * @param {type} userGroup
 * @param {type} hbacRule
 * @returns {jqXHR}
 */
function addUserGroupToHBACRule(userGroup, hbacRule) {
    if (!Array.isArray(userGroup)) {
        userGroup = [userGroup];
    }
        
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_user",
        "params":[
            [hbacRule],
            {"group": userGroup}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds hostgroup(s) to a HBAC rule
 * @param {type} hostGroup
 * @param {type} hbacRule
 * @returns {jqXHR}
 */
function addHostGroupToHBACRule(hostGroup, hbacRule) {
    if (!Array.isArray(hostGroup)) {
        hostGroup = [hostGroup];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_host",
        "params":[
            [hbacRule],
            {"hostgroup": hostGroup}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Adds services to a HBAC rule
 * @param {type} services
 * @param {type} hbacRule
 * @returns {jqXHR}
 */
function addServicesToHBACRule(services, hbacRule) {
    if (!Array.isArray(services)) {
        services = [services];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_add_service",
        "params":[
            [hbacRule],
            {"hbacsvc": services}
        ],
        "id":0
    };
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/*
 * Creates a new IPA ACL Login policy (HBAC Rule with user groups and host groups) for the specified UUID
 * Abbreviations: ug -> user group, hg -> host group
 */
function createLoginAclPolicy(serviceUUID, username) {        
    // start by creating login access as both login and sudo require login access
    var ugLoginName = "ug-login-" + serviceUUID;
    var hgLoginName = "hg-login-" + serviceUUID;
    var hbacLoginName = "hbac-login-" + serviceUUID;    
    var loginServices = ["login","sshd"];
    var hosts = [];
    var ugError = null;
    var hgError = null;
    var hbacError = null;
    var hostsQueryError = true; 
    
    var aclLoginPolicyResult = {}; // currently a way to debug errors
    
    // need to change it so when all the ajax calls are done - then return the aclPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createLoginUg = createUserGroup(ugLoginName,"Login user group for service instance: " + serviceUUID);
    var createLoginHg = createHostGroup(hgLoginName, "Login host group for service instance: " + serviceUUID);
    var createLoginHbac = createHBACRule(hbacLoginName,"Login HBAC Rule (login,ssh) for service instance: " + serviceUUID);
    var getLoginHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createLoginUg, createLoginHg, createLoginHbac, getLoginHosts).done(function(ug, hg, hbac, hostsQuery) {
        ugError = ug[0]["error"];
        hgError = hg[0]["error"];
        hbacError = hbac[0]["error"];
        hostsQueryError = true;        
                
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;            
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            if (hostsObjs) {
                hostsObjs.forEach(function(h) {                
                    hosts.push(h["hostname"]);               
                });
                aclLoginPolicyResult["ReceivedHostsForServiceInstance"] = true; 
            } else {
                aclLoginPolicyResult["ReceivedHostsForServiceInstance"] = false;
                aclLoginPolicyResult["ReceivedHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
                hostsQueryError = true;
            }                       
        } else {
            aclLoginPolicyResult["ReceivedHostsForServiceInstance"] = false;
            aclLoginPolicyResult["ReceivedHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error is null for the IPA requests, then the request was successful
        
        if (ugError === null) {
            aclLoginPolicyResult["CreatedUserGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedUserGroup"] = false;
            aclLoginPolicyResult["CreatedUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclLoginPolicyResult["CreatedHostGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedHostGroup"] = false;
            aclLoginPolicyResult["CreatedHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclLoginPolicyResult["CreatedHBACRule"] = true;
        } else {
            aclLoginPolicyResult["CreatedHBACRule"] = false;
            aclLoginPolicyResult["CreatedHBACRuleError"] = hbacError;
        }                
        
    }).then(function() {
        
        // if no errors in all three creation/query calls -> continue the process
        // null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclLoginPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = true;
            
            var addLoginUgUsers = addUsersToUserGroup(username, ugLoginName);
            var addLoginHgHosts = addHostsToHostGroup(hosts, hgLoginName);
            var addLoginUgToHbac = addUserGroupToHBACRule(ugLoginName,hbacLoginName);
            var addLoginHgToHbac = addHostGroupToHBACRule(hgLoginName,hbacLoginName);
            var addLoginSrvcsToHbac = addServicesToHBACRule(loginServices, hbacLoginName);

            return $.when(addLoginUgUsers, addLoginHgHosts, addLoginUgToHbac, addLoginHgToHbac, addLoginSrvcsToHbac)
                    .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                        var ugusersError = ugusers[0]["error"];
                        
                        // get the number of users that have been successfully added to the user group
                        // since in this case only one user can be added at a time (due to clicking on 
                        // checkbox immediately adding the user / creating the policy). 
                        // just check if the completed is equal to 1 which shows that the user has been added
                        var ugusersCompleted = ugusers[0]["result"]["completed"];
                        
                        var hghostsError = hghosts[0]["error"];
                        var ughbacError = ughbac[0]["error"];
                        var hghbacError = hghbac[0]["error"];
                        var srvcshbacError = srvcshbac[0]["error"];


                        if (ugusersError === null && ugusersCompleted === 1) {
                            aclLoginPolicyResult["AddedUsersToUserGroup"] = true;
                        } else {
                            aclLoginPolicyResult["AddedUsersToUserGroup"] = false;
                            aclLoginPolicyResult["AddedUsersToUserGroupError"] = ugusersError;
                        }

                        if (hghostsError === null) {
                            aclLoginPolicyResult["AddedHostsToHostGroup"] = true;
                        } else {
                            aclLoginPolicyResult["AddedHostsToHostGroup"] = false;
                            aclLoginPolicyResult["AddedHostsToHostGroupError"] = hghostsError;
                        }

                        if (ughbacError === null) {
                            aclLoginPolicyResult["AddedUserGroupToHBAC"] = true;
                        } else {
                            aclLoginPolicyResult["AddedUserGroupToHBAC"] = false;
                            aclLoginPolicyResult["AddedUserGroupToHBACError"] = ughbacError;
                        }

                        if (hghbacError === null) {
                            aclLoginPolicyResult["AddedHostGroupToHBAC"] = true;
                        } else {
                            aclLoginPolicyResult["AddedHostGroupToHBAC"] = false;
                            aclLoginPolicyResult["AddedHostGroupToHBACError"] = hghbacError;
                        }

                        if (srvcshbacError === null) {
                            aclLoginPolicyResult["AddedServicesToHBAC"] = true;
                        } else {
                            aclLoginPolicyResult["AddedServicesToHBAC"] = false;
                            aclLoginPolicyResult["AddedServicesToHBACError"] = srvcshbacError;
                        }
                        
                        // if no errors in adding users/services/hosts -> give true 
                        if (!srvcshbacError && !ugusersError && ugusersCompleted === 1 && !hghbacError && !ughbacError) {
                            aclLoginPolicyResult["AddedUsersHostsToGroupAndServicesToRule"] = true;
                        } else {
                            // return an error
                            aclLoginPolicyResult["AddedUsersHostsToGroupAndServicesToRule"] = false;
                            
                            // if something went wrong - delete the ACL policy
                            removeACLPolicy(serviceUUID,"login"); 
                        }
                        return aclLoginPolicyResult;
                    }).fail(function(err) {
                        console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
                    });
        } else {
            aclLoginPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = false;
            // if something went wrong - delete the ACL policy
            removeACLPolicy(serviceUUID, "login");
            return aclLoginPolicyResult; 
        }                    
        }).fail(function(err) {
            console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
        });                   
}

/*
 * Creates a new IPA ACL Sudo policy (HBAC Rule with user groups and host groups) for the specified UUID
 * Abbreviations: ug -> user group, hg -> host group
 */
function createSudoAclPolicy(serviceUUID, username) {                
    var ugSudoName = "ug-sudo-" + serviceUUID;
    var hgSudoName = "hg-sudo-" + serviceUUID;
    var hbacSudoName = "hbac-sudo-" + serviceUUID;
    var sudoServices = ["login","sshd","sudo"];    
    var aclSudoPolicyResult = {}; // currently a way to debug errors
    var hosts = [];
    var ugError = null;
    var hgError = null;
    var hbacError = null;
    var hostsQueryError = true; 
    
    // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createSudoUg = createUserGroup(ugSudoName,"Sudo user group for service instance: " + serviceUUID);
    var createSudoHg = createHostGroup(hgSudoName, "Sudo host group for service instance: " + serviceUUID);
    var createSudoHbac = createHBACRule(hbacSudoName,"Sudo HBAC Rule (login,ssh,sudo) for service instance: " + serviceUUID);
    var getSudoHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createSudoUg, createSudoHg, createSudoHbac, getSudoHosts).done(function(ug, hg, hbac, hostsQuery) {
        ugError = ug[0]["error"];
        hgError = hg[0]["error"];
        hbacError = hbac[0]["error"];
        hostsQueryError = true;        
        
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;
            aclSudoPolicyResult["ReceivedHostsForServiceInstance"] = true;
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            
            // check if any hosts were recieved.
            if (hostsObjs) {
                hostsObjs.forEach(function(h) {                
                    hosts.push(h["hostname"]);               
                });
                aclSudoPolicyResult["ReceivedHostsForServiceInstance"] = true; 
            } else {
                aclSudoPolicyResult["ReceivedHostsForServiceInstance"] = false;
                aclSudoPolicyResult["ReceivedHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
                hostsQueryError = true;
            }            
        } else {
            aclSudoPolicyResult["ReceivedHostsForServiceInstance"] = false;
            aclSudoPolicyResult["ReceivedHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error for ipa requests is null, then the request was successful
        
        if (ugError === null) {
            aclSudoPolicyResult["CreatedUserGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedUserGroup"] = false;
            aclSudoPolicyResult["CreatedUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclSudoPolicyResult["CreatedHostGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedHostGroup"] = false;
            aclSudoPolicyResult["CreatedHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclSudoPolicyResult["CreatedHBACRule"] = true;
        } else {
            aclSudoPolicyResult["CreatedHBACRule"] = false;
            aclSudoPolicyResult["CreatedHBACRuleError"] = hbacError;
        }
                 
        
    }).then(function() {
        
        // if no errors in all three -> null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclSudoPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = true;
            
            var addSudoUgUsers = addUsersToUserGroup(username, ugSudoName);
            var addSudoHgHosts = addHostsToHostGroup(hosts, hgSudoName);
            var addSudoUgToHbac = addUserGroupToHBACRule(ugSudoName,hbacSudoName);
            var addSudoHgToHbac = addHostGroupToHBACRule(hgSudoName,hbacSudoName);
            var addSudoSrvcsToHbac = addServicesToHBACRule(sudoServices, hbacSudoName);

            return $.when(addSudoUgUsers, addSudoHgHosts, addSudoUgToHbac, addSudoHgToHbac, addSudoSrvcsToHbac)
                    .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                        var ugusersError = ugusers[0]["error"];
                        
                        // get the number of users that have been successfully added to the user group
                        // since in this case only one user can be added at a time (due to clicking on 
                        // checkbox immediately adding the user / creating the policy). 
                        // just check if the completed is equal to 1 which shows that the user has been added
                        var ugusersCompleted = ugusers[0]["result"]["completed"];
                
                        var hghostsError = hghosts[0]["error"];
                        var ughbacError = ughbac[0]["error"];
                        var hghbacError = hghbac[0]["error"];
                        var srvcshbacError = srvcshbac[0]["error"];

                        if (ugusersError === null && ugusersCompleted === 1) {
                            aclSudoPolicyResult["AddedUsersToUserGroup"] = true;
                        } else {
                            aclSudoPolicyResult["AddedUsersToUserGroup"] = false;
                            aclSudoPolicyResult["AddedUsersToUserGroupError"] = ugusersError;
                        }

                        if (hghostsError === null) {
                            aclSudoPolicyResult["AddedHostsToHostGroup"] = true;
                        } else {
                            aclSudoPolicyResult["AddedHostsToHostGroup"] = false;
                            aclSudoPolicyResult["AddedHostsToHostGroupError"] = hghostsError;
                        }

                        if (ughbacError === null) {
                            aclSudoPolicyResult["AddedUserGroupToHBAC"] = true;
                        } else {
                            aclSudoPolicyResult["AddedUserGroupToHBAC"] = false;
                            aclSudoPolicyResult["AddedUserGroupToHBACError"] = ughbacError;
                        }

                        if (hghbacError === null) {
                            aclSudoPolicyResult["AddedHostGroupToHBAC"] = true;
                        } else {
                            aclSudoPolicyResult["AddedHostGroupToHBAC"] = false;
                            aclSudoPolicyResult["AddedHostGroupToHBACError"] = hghbacError;
                        }

                        if (srvcshbacError === null) {
                            aclSudoPolicyResult["AddedServicesToHBAC"] = true;
                        } else {
                            aclSudoPolicyResult["AddedServicesToHBAC"] = false;
                            aclSudoPolicyResult["AddedServicesToHBACError"] = srvcshbacError;
                        }


                        if (!srvcshbacError && !ugusersError && ugusersCompleted === 1 && !hghbacError && !ughbacError) {
                            aclSudoPolicyResult["AddedUsersHostsToGroupAndServicesToRule"] = true;
                        } else {
                            // return an error
                            aclSudoPolicyResult["AddedUsersHostsToGroupAndServicesToRule"] = false;
                            
                            // if something went wrong - delete the ACL policy
                            removeACLPolicy(serviceUUID,"sudo"); 
                        }
                        return aclSudoPolicyResult;
                    }).fail(function(err) {
                        console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
                    });
        } else {
            aclSudoPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = false;
            // if something went wrong - delete the ACL policy
            removeACLPolicy(serviceUUID, "sudo");
            return aclSudoPolicyResult;
        }
    }).fail(function(err) {
        console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
    });  
}

/**
 * Searches for the username in the specified ACL Policy
 * @param {String} serviceUUID
 * @param {String} accessType
 * @param {String} username 
 * @returns {jqXhr}
 */
function isUserInAclPolicy(serviceUUID, accessType, username) {
    // creating the IPA request
    /**
     * Try to find an user (hence user_find) that is in both the correct
     * user group and hbac rule
     */
    var ipaRequestData = {
        "method":"user_find",
        "params":[
            [username],
            {
                "in_group": [
                    "ug-" + accessType + "-" + serviceUUID
                ],
                "in_hbacrule": [
                    "hbac-" + accessType + "-" + serviceUUID
                ]
            }
        ],
        "id":0
    };
    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Checks if the ACL policy - given the service instance UUID and access type - exists.
 * Simply queries to see if the HBAC rule was at least created, does not check if 
 * user groups, host groups, or services have been added. (However, the whole HBAC rule
 * with groups and services should already be added if the HBAC rule was created)
 * @param {type} serviceUUID
 * @param {type} accessType
 * @returns {jqXHR}
 */
function checkForExistingACLPolicy(serviceUUID, accessType) { 
    var hbacrule = "hbac-" + accessType + "-" + serviceUUID;
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_find",
        "params":[
            [hbacrule],
            {}
        ],
        "id":0
    };
    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

/**
 * Removes the specified user from the user group, effectively removing them from
 * the ACL policy
 * @param {type} username
 * @param {type} serviceUUID
 * @param {type} accessType
 * @returns {undefined}
 */
function removeUserFromACLPolicy(username, serviceUUID, accessType) {
    var usergroup = "ug-" + accessType + "-" + serviceUUID;
    
    if (!Array.isArray(username)) {
        username = [username];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_remove_member",
        "params":[
            [usergroup],
            {"user":username}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}


function deleteHostGroup(hostgroupName) {
    if (!Array.isArray(hostgroupName)) {
        hostgroupName = [hostgroupName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hostgroup_del",
        "params":[
            hostgroupName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function deleteUserGroup(usergroupName) {
    if (!Array.isArray(usergroupName)) {
        usergroupName = [usergroupName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"group_del",
        "params":[
            usergroupName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

function deleteHBACRule(hbacruleName) {
    if (!Array.isArray(hbacruleName)) {
        hbacruleName = [hbacruleName];
    }
    
    // creating the IPA request
    var ipaRequestData = {
        "method":"hbacrule_del",
        "params":[
            hbacruleName,
            {}
        ],
        "id":0
    };    
    
    // ajax call fields
    // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
    var ipaAjaxCall = {
        "url": ipaApiUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "bearer " + keycloak.token
        },
        "data": JSON.stringify(ipaRequestData)
    };
    
    return $.ajax(ipaAjaxCall);
}

// Delete the ACL Policy of the specified access when the service instance is cancelled.
function removeACLPolicy(serviceUUID, accessType) {
    var ugName = "ug-" + accessType + "-" + serviceUUID;
    var hgName = "hg-" + accessType + "-" + serviceUUID;
    var hbacName = "hbac-" + accessType + "-" + serviceUUID;
    var removeAclPolicyResult = {}; // currently a way to debug errors
    
    // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var deleteUg = deleteUserGroup(ugName);
    var deleteHg = deleteHostGroup(hgName);
    var deleteHbac = deleteHBACRule(hbacName);
    
    return $.when(deleteUg, deleteHg, deleteHbac).done(function(delUg, delHg, delHbac) {
        var delUgError = delUg[0]["error"];
        var delHgError = delHg[0]["error"];
        var delHbacError = delHbac[0]["error"];
        
        if (delUgError === null) {
            removeAclPolicyResult["DeletedUserGroup"] = true;
        } else {
            removeAclPolicyResult["DeletedUserGroup"] = false;
            removeAclPolicyResult["DeletedUserGroupError"] = JSON.stringify(delUg);
        }
        
        if (delHgError === null) {
            removeAclPolicyResult["DeletedHostGroup"] = true;
        } else {
            removeAclPolicyResult["DeletedHostGroup"] = false;
            removeAclPolicyResult["DeletedHostGroupError"] = JSON.stringify(delHg);
        }
        
        if (delHbacError === null) {
            removeAclPolicyResult["DeletedHBAC"] = true;
        } else {
            removeAclPolicyResult["DeletedHBAC"] = false;
            removeAclPolicyResult["DeletedHBACError"] = JSON.stringify(delHbac);
        }
                 
        return removeAclPolicyResult;
    });
}

/**
 * Removes both types (login and sudo) ACL policies.
 * @param {type} serviceUUID
 * @returns {undefined}
 */
function removeAllACLPolicies(serviceUUID) {
    var removedAllACLPolicies = {};
    
    var removeLoginPolicy = removeACLPolicy(serviceUUID,"login");
    var removeSudoPolicy = removeACLPolicy(serviceUUID,"sudo");
    
    return $.when(removeLoginPolicy, removeSudoPolicy).done(function(rmLogin, rmSudo) {
        removedAllACLPolicies["RemovedLoginACLPolicy"] = JSON.stringify(rmLogin);
        removedAllACLPolicies["RemovedSudoACLPolicy"] = JSON.stringify(rmSudo);        
        return removedAllACLPolicies;
    });
}

/**
 * Parses an acl function result JSON object (created by the ACL 
 * functions) and returns a pretty string to display to the user.
 * Mainly used to parse and pretty print failures/error messages 
 *
 * @param {JSON} resultJSON
 * @returns {String}
 */
function parseACLPolicyResult(resultJSON) {
    var prettyResults = "";
    
    // general keys for the create*AclPolicy functions 
    // these are the keys in the json that indicate success or error
    // policy creation is done in 2 steps
    // step 1 - create groups and rules and find the hosts
    // step 2 - add the users, hosts, services to the groups and rules

    var stepOne = "GroupAndRuleCreatedAndRightHostsFound";
    var stepTwo = "AddedUsersHostsToGroupAndServicesToRule";

    // check step 1
    if (resultJSON[stepOne] === false) {    	
    	var receivedHosts = "ReceivedHostsForServiceInstance";
        var createdUg = "CreatedUserGroup";
        var createdHg = "CreatedHostGroup";
        var createdHbac = "CreatedHBACRule";

    	if (resultJSON[receivedHosts] === false) {
            prettyResults += "Could not find or receive hosts for service instance.\n";
            console.log("Error: " + resultJSON[receivedHosts + "Error"]);
    	}

    	if (resultJSON[createdUg] === false) {
            prettyResults += "Could not create user group.\n";
            console.log("Error: " + resultJSON[createdUg + "Error"]);
    	}

    	if (resultJSON[createdHg] === false) {
            prettyResults += "Could not create host group.\n";
            console.log("Error: " + resultJSON[createdHg + "Error"]);
    	}

    	if (resultJSON[createdHbac] === false) {
            prettyResults += "Could not create HBAC rule.\n";
            console.log("Error: " + createdHbac[createdHg + "Error"]);
    	}
    		
    }

    // check step 2
    if (resultJSON[stepTwo] === false) {
    	var addedUsersToUg = "AddedUsersToUserGroup";
    	var addedHostsToHg = "AddedHostsToHostGroup";
    	var addedUgToHbac = "AddedUserGroupToHBAC";
    	var addedHgToHbac = "AddedHostGroupToHBAC";
    	var addedSrvcToHbac = "AddedServicesToHBAC";

    	if (resultJSON[addedUsersToUg] === false) {
            prettyResults += "Could not add user to user group. Ensure user is registered with IPA server and is NOT already in ACL policy.\n";
            console.log("Error: " + resultJSON[addedUsersToUg + "Error"]);
    	}

    	if (resultJSON[addedHostsToHg] === false) {
            prettyResults += "Could not add hosts to host group. Ensure hosts has a fully qualied domain name.\n";
            console.log("Error: " + resultJSON[addedHostsToHg + "Error"]);
    	}

    	if (resultJSON[addedUgToHbac] === false) {
            prettyResults += "Could not add user group to HBAC Rule.\n";
            console.log("Error: " + resultJSON[addedUgToHbac + "Error"]);
    	}

    	if (resultJSON[addedHgToHbac] === false) {
            prettyResults += "Could not add host group to HBAC Rule.\n";
            console.log("Error: " + resultJSON[addedHgToHbac + "Error"]);
    	}

    	if (resultJSON[addedSrvcToHbac] === false) {
            prettyResults += "Could not add services to HBAC Rule.\n";
            console.log("Error: " + resultJSON[addedSrvcToHbac + "Error"]);
    	}    	
    		
    }

    // keys for removeACLPolicy
    var deletedUg = "DeletedUserGroup";
    var deletedHg = "DeletedHostGroup";
    var deletedHbac = "DeletedHBAC";

    if (resultJSON[deletedUg] === false) {
        prettyResults += "Could not delete user group.\n";
        console.log("Error: " + resultJSON[deletedUg + "Error"]);
    } 

    if (resultJSON[deletedHg] === false) {
        prettyResults += "Could not delete host group.\n";
        console.log("Error: " + resultJSON[deletedHg + "Error"]);
    }

    if (resultJSON[deletedHbac] === false) {
        prettyResults += "Could not delete HBAC Rule.\n";
        console.log("Error: " + resultJSON[deletedHbac + "Error"]);
    }


    // fif none of the above errors were caught - just output the stringified json
    if (prettyResults.length === 0) {
    	prettyResults += "Unkown error. Error output: " + JSON.stringify(resultJSON);
    }

    return prettyResults;
}