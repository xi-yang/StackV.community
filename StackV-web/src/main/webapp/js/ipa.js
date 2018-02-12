
/* global XDomainRequest, baseUrl, keycloak, TweenLite, Power2, Mousetrap */

var ipaApiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/request";

/**
 * Currently, just logs in the user using default credentials
 * Check if the user is currently logged into IPA server. If not logged in, the log in the user.
 * @param {type} username
 * @param {type} password
 * @returns {jqXHR}
 */
function ipaLogin(username, password){
    var apiUrl = baseUrl + "/StackV-web/restapi/app/acl/ipa/login";
    
    return $.ajax({
        url: apiUrl,
        type: 'POST',
        data: {
            "username":"admin",
            "password":"max12345"
        },
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
    
    var aclLoginPolicyResult = {}; // currently a way to debug errors
    
    // need to change it so when all the ajax calls are done - then return the aclPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createLoginUg = createUserGroup(ugLoginName,"Login user group for service instance: " + serviceUUID);
    var createLoginHg = createHostGroup(hgLoginName, "Login host group for service instance: " + serviceUUID);
    var createLoginHbac = createHBACRule(hbacLoginName,"Login HBAC Rule (login,ssh) for service instance: " + serviceUUID);
    var getLoginHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createLoginUg, createLoginHg, createLoginHbac, getLoginHosts).done(function(ug, hg, hbac, hostsQuery) {
        var ugError = ug[0]["error"];
        var hgError = hg[0]["error"];
        var hbacError = hbac[0]["error"];
        var hostsQueryError = true;        
                
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;
            aclLoginPolicyResult["RecievedRightHostsForServiceInstance"] = true;
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            hostsObjs.forEach(function(h) {                
                hosts.push(h["hostname"]);               
            });
            console.log("parsed login hosts: " + hosts);
        } else {
            aclLoginPolicyResult["RecievedHostsForServiceInstance"] = false;
            aclLoginPolicyResult["RecievedHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error is null for the IPA requests, then the request was successful
        
        if (ugError === null) {
            aclLoginPolicyResult["CreatedLoginUserGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginUserGroup"] = false;
            aclLoginPolicyResult["CreatedLoginUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclLoginPolicyResult["CreatedLoginHostGroup"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginHostGroup"] = false;
            aclLoginPolicyResult["CreatedLoginHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclLoginPolicyResult["CreatedLoginHBACRule"] = true;
        } else {
            aclLoginPolicyResult["CreatedLoginHBACRule"] = false;
            aclLoginPolicyResult["CreatedLoginHBACRuleError"] = hbacError;
        }
        
        // if no errors in all three -> null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclLoginPolicyResult["LoginGroupAndRuleCreatedAndRightHostsFound"] = true;
        } else {
            aclLoginPolicyResult["LoginGroupAndRuleCreatedAndRightHostsFound"] = false;
        }
        
    }).then(function() {                        
        var addLoginUgUsers = addUsersToUserGroup(username, ugLoginName);
        var addLoginHgHosts = addHostsToHostGroup(hosts, hgLoginName);
        var addLoginUgToHbac = addUserGroupToHBACRule(ugLoginName,hbacLoginName);
        var addLoginHgToHbac = addHostGroupToHBACRule(hgLoginName,hbacLoginName);
        var addLoginSrvcsToHbac = addServicesToHBACRule(loginServices, hbacLoginName);
        
        return $.when(addLoginUgUsers, addLoginHgHosts, addLoginUgToHbac, addLoginHgToHbac, addLoginSrvcsToHbac)
                .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                    var ugusersError = ugusers[0]["error"];
                    var hghostsError = hghosts[0]["error"];
                    var ughbacError = ughbac[0]["error"];
                    var hghbacError = hghbac[0]["error"];
                    var srvcshbacError = srvcshbac[0]["error"];
                                        
                    
                    if (ugusersError === null) {
                        aclLoginPolicyResult["AddedUsersToLoginUserGroup"] = true;
                    } else {
                        aclLoginPolicyResult["AddedUsersToLoginUserGroup"] = false;
                        aclLoginPolicyResult["AddedUsersToLoginUserGroupError"] = ugusersError;
                    }
                    
                    if (hghostsError === null) {
                        aclLoginPolicyResult["AddedHostsToLoginHostGroup"] = true;
                    } else {
                        aclLoginPolicyResult["AddedHostsToLoginHostGroup"] = false;
                        aclLoginPolicyResult["AddedHostsToLoginHostGroupError"] = hghostsError;
                    }
                    
                    if (ughbacError === null) {
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginUserGroupToLoginHBACError"] = ughbacError;
                    }
                    
                    if (hghbacError === null) {
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginHostGroupToLoginHBACError"] = hghbacError;
                    }
                    
                    if (srvcshbacError === null) {
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBAC"] = true;
                    } else {
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBAC"] = false;
                        aclLoginPolicyResult["AddedLoginServicesToLoginHBACError"] = srvcshbacError;
                    }
                    
                    
                    if (!srvcshbacError && !ugusersError && !hghbacError && !ughbacError) {
                        aclLoginPolicyResult["AddedLoginGroupAndServicesToLoginHBAC"] = true;
                    }
                    return aclLoginPolicyResult;
                }).fail(function(err) {
                    console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
                });
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
    
    // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult
    
    // the user group, host group, hbac rule all need to created before any
    // (mainly adding users, hosts, and services) is done to them
    
    var createSudoUg = createUserGroup(ugSudoName,"Sudo user group for service instance: " + serviceUUID);
    var createSudoHg = createHostGroup(hgSudoName, "Sudo host group for service instance: " + serviceUUID);
    var createSudoHbac = createHBACRule(hbacSudoName,"Sudo HBAC Rule (login,ssh,sudo) for service instance: " + serviceUUID);
    var getSudoHosts = getHostsForServiceInstance(serviceUUID);
    
    return $.when(createSudoUg, createSudoHg, createSudoHbac, getSudoHosts).done(function(ug, hg, hbac, hostsQuery) {
        var ugError = ug[0]["error"];
        var hgError = hg[0]["error"];
        var hbacError = hbac[0]["error"];
        var hostsQueryError = true;        
        
        // verify and parse the loginHosts data
        // looks like below
        //{ serviceUUID: "5578c890-9a1c-4e23-9686-ab70ad274a92", jsonTemplate: "{\"hostgroup\":[{\"hostname\":\"180-146.research.maxgigapop.net\"}]}", jsonModel: null }
        
        // just to verify the right data is gotten
        if (hostsQuery[0]["serviceUUID"] === serviceUUID) {
            hostsQueryError = false;
            aclSudoPolicyResult["RecievedRightHostsForServiceInstance"] = true;
            var parsed = JSON.parse(hostsQuery[0]["jsonTemplate"]);
            var hostsObjs = parsed["hostgroup"];
            hostsObjs.forEach(function(h) {                
                hosts.push(h["hostname"]);               
            });
            console.log("parsed sudo hosts: " + hosts);            
        } else {
            aclSudoPolicyResult["RecievedRightHostsForServiceInstance"] = false;
            aclSudoPolicyResult["RecievedRightHostsForServiceInstanceError"] = JSON.stringify(hostsQuery);
        }
        
        // if error for ipa requests is null, then the request was successful
        
        if (ugError === null) {
            aclSudoPolicyResult["CreatedSudoUserGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoUserGroup"] = false;
            aclSudoPolicyResult["CreatedSudoUserGroupError"] = ugError;
        }
        
        if (hgError === null) {
            aclSudoPolicyResult["CreatedSudoHostGroup"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoHostGroup"] = false;
            aclSudoPolicyResult["CreatedSudoHostGroupError"] = hgError;
        }
        
        if (hbacError === null) {
            aclSudoPolicyResult["CreatedSudoHBACRule"] = true;
        } else {
            aclSudoPolicyResult["CreatedSudoHBACRule"] = false;
            aclSudoPolicyResult["CreatedSudoHBACRuleError"] = hbacError;
        }
        
        // if no errors in all three -> null is a falsy value
        if (!ugError && !hgError && !hbacError && !hostsQueryError) {
            aclSudoPolicyResult["SudoGroupAndRuleCreatedAndRightHostsFound"] = true;
        } else {
            aclSudoPolicyResult["SudoGroupAndRuleCreatedAndRightHostsFound"] = false;
        }
        
    }).then(function() {                        
        var addSudoUgUsers = addUsersToUserGroup(username, ugSudoName);
        var addSudoHgHosts = addHostsToHostGroup(hosts, hgSudoName);
        var addSudoUgToHbac = addUserGroupToHBACRule(ugSudoName,hbacSudoName);
        var addSudoHgToHbac = addHostGroupToHBACRule(hgSudoName,hbacSudoName);
        var addSudoSrvcsToHbac = addServicesToHBACRule(sudoServices, hbacSudoName);
        
        return $.when(addSudoUgUsers, addSudoHgHosts, addSudoUgToHbac, addSudoHgToHbac, addSudoSrvcsToHbac)
                .then(function(ugusers, hghosts, ughbac, hghbac, srvcshbac) {
                    var ugusersError = ugusers[0]["error"];
                    var hghostsError = hghosts[0]["error"];
                    var ughbacError = ughbac[0]["error"];
                    var hghbacError = hghbac[0]["error"];
                    var srvcshbacError = srvcshbac[0]["error"];
                    
                    if (ugusersError === null) {
                        aclSudoPolicyResult["AddedUsersToSudoUserGroup"] = true;
                    } else {
                        aclSudoPolicyResult["AddedUsersToSudoUserGroup"] = false;
                        aclSudoPolicyResult["AddedUsersToSudoUserGroupError"] = ugusersError;
                    }
                    
                    if (hghostsError === null) {
                        aclSudoPolicyResult["AddedHostsToSudoHostGroup"] = true;
                    } else {
                        aclSudoPolicyResult["AddedHostsToSudoHostGroup"] = false;
                        aclSudoPolicyResult["AddedHostsToSudoHostGroupError"] = hghostsError;
                    }
                    
                    if (ughbacError === null) {
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoUserGroupToSudoHBACError"] = ughbacError;
                    }
                    
                    if (hghbacError === null) {
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoHostGroupToSudoHBACError"] = hghbacError;
                    }
                    
                    if (srvcshbacError === null) {
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBAC"] = true;
                    } else {
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBAC"] = false;
                        aclSudoPolicyResult["AddedSudoServicesToSudoHBACError"] = srvcshbacError;
                    }
                    
                    
                    if (!srvcshbacError && !ugusersError && !hghbacError && !ughbacError) {
                        aclSudoPolicyResult["AddedSudoGroupAndServicesToSudoHBAC"] = true;
                    }
                    return aclSudoPolicyResult;
                }).fail(function(err) {
                    console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
                });
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
 * Removes both types (login and sudo) ACL policies
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
