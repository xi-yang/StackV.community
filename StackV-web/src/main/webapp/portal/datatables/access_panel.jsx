import React from "react";
import iziToast from "izitoast";
import ReactInterval from "react-interval";

import "./logging.css";
let successToast = {
    theme: "dark",
    icon: "fas fa-ban",
    close: "false",
    title: "Success",
    position: "topRight",
    progressBarColor: "green",
    pauseOnHover: false,
    timeout: 2500,
    displayMode: "replace",
};
let errorToast = {
    theme: "dark",
    icon: "fas fa-ban",
    close: "false",
    title: "Error",
    message: "The ACL Request could not be completed. Please try again.",
    position: "topRight",
    progressBarColor: "red",
    pauseOnHover: false,
    timeout: 5000,
    displayMode: "replace",
};

class UserPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            apiUrl: window.location.origin + "/StackV-web/restapi/app/data/users/",
            ipaUrl: window.location.origin + "/StackV-web/restapi/app/acl/ipa/request"
        };

        this.initTable = this.initTable.bind(this);
        this.loadData = this.loadData.bind(this);

        this.updateIPA = this.updateIPA.bind(this);
        this.updateLogin = this.updateLogin.bind(this);
        this.updateSudo = this.updateSudo.bind(this);

        this.ipaLogin = this.ipaLogin.bind(this);
        this.isUserInAclPolicy = this.isUserInAclPolicy.bind(this);
        this.checkForExistingACLPolicy = this.checkForExistingACLPolicy.bind(this);
        this.getHostsForServiceInstance = this.getHostsForServiceInstance.bind(this);

        this.createLoginAclPolicy = this.createLoginAclPolicy.bind(this);
        this.createUserGroup = this.createUserGroup.bind(this);
        this.createHostGroup = this.createHostGroup.bind(this);
        this.addUsersToUserGroup = this.addUsersToUserGroup.bind(this);
        this.addHostsToHostGroup = this.addHostsToHostGroup.bind(this);
        this.addUserGroupToHBACRule = this.addUserGroupToHBACRule.bind(this);
        this.addHostGroupToHBACRule = this.addHostGroupToHBACRule.bind(this);
        this.addServicesToHBACRule = this.addServicesToHBACRule.bind(this);
        this.createHost = this.createHost.bind(this);

        this.removeACLPolicy = this.removeACLPolicy.bind(this);
        this.deleteUserGroup = this.deleteUserGroup.bind(this);
        this.deleteHostGroup = this.deleteHostGroup.bind(this);
        this.deleteHBACRule = this.deleteHBACRule.bind(this);
        this.removeUserFromACLPolicy = this.removeUserFromACLPolicy.bind(this);
    }
    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.active === false && nextProps.active === false) { return false; }
        return true;
    }
    componentDidMount() {
        this.initTable();
    }
    loadData() {
        if (this.props.active && this.state.dataTable.scroller.page().start === 0) {
            this.updateIPA();
            this.state.dataTable.ajax.reload();
        }
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="logging-panel">
            <div id="logging-header-div">
                Resource Access Control
            </div>
            <ReactInterval timeout={this.props.refreshTimer < 3000 ? 3000 : this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadData} />
            <div id="logging-body-div">
                <table id="accessData" className="table table-striped table-bordered display" cellSpacing="0" width="100%">
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Login</th>
                            <th>Sudo</th>
                        </tr>
                    </thead>
                </table>
            </div>
        </div>;
    }

    initTable() {
        let panel = this;
        let dataTable = $("#accessData").DataTable({
            "ajax": {
                url: panel.state.apiUrl,
                type: "GET",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                }
            },
            "buttons": [],
            "columns": [
                { "data": "username" },
                {
                    "data": null,
                    "render": function (data, type, full, meta) {
                        return "<input class=\"access-checkbox\" data-access=\"login\" data-username=" + data.username + " type=\"checkbox\" />";
                    }
                },
                {
                    "data": null,
                    "render": function (data, type, full, meta) {
                        return "<input class=\"access-checkbox\" data-access=\"sudo\" data-username=" + data.username + " type=\"checkbox\" />";
                    }
                },
            ],
            "createdRow": function (row, data, dataIndex) {
                $(row).addClass("user-row");
                $(row).attr("data-username", data.username);
            },
            "dom": "Bfrtip",
            "ordering": false,
            "processing": true,
            "scroller": {
                loadingIndicator: true
            },
            "scrollX": true,
            "scrollY": "calc(60vh - 130px)",
        });

        // Add event listeners for adding/removing access
        $("#accessData tbody").on("change", "td input[data-access=\"login\"]", function (e) {
            e.preventDefault();
            panel.updateLogin($(this).data("username"), $(this).is(":checked"));
        });
        $("#accessData tbody").on("change", "td input[data-access=\"sudo\"]", function (e) {
            e.preventDefault();
            panel.updateSudo($(this).data("username"), $(this).is(":checked"));
        });

        panel.setState({ dataTable: dataTable }, () => { panel.setState({ refreshEnabled: true }); });
    }

    updateIPA() {
        let panel = this;
        let data = this.state.dataTable.data();
        let loginArr = [], sudoArr = [];

        $.when(panel.ipaLogin()).done(function (result) {
            console.debug(result);
            data.each(function (value, index) {
                let user = value.username;
                panel.isUserInAclPolicy(panel.props.keycloak, panel.props.uuid, "login", user).done(function (aclResult) {
                    console.debug(aclResult.result);
                    loginArr.push(aclResult.result);
                });
                panel.isUserInAclPolicy(panel.props.keycloak, panel.props.uuid, "sudo", user).done(function (aclResult) {
                    sudoArr.push(aclResult.result);
                });
            });
            panel.setState({ access: { "login": loginArr, "sudo": sudoArr } });
        });
    }
    updateLogin(username, add) {
        let panel = this;
        const loadingToast = {
            theme: "dark",
            icon: "fas fa-ban",
            close: "false",
            title: "Processing Request",
            position: "topRight",
            progressBarColor: "green",
            pauseOnHover: false,
            timeout: 15000,
            displayMode: "replace",
            onClosing: function (instance, toast, closedBy) {
                if (closedBy === "timeout") {
                    panel.removeACLPolicy(panel.props.uuid, "login");
                    errorToast.message = "The ACL Request could not be completed. Please try again.";
                    iziToast.show(errorToast);
                    console.log("removing policy due to timeout");
                }
            }
        };
        // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
        iziToast.show(loadingToast);
        let toast = document.querySelector(".iziToast");
        // ensure the user is logged in before making any IPA requests
        $.when(panel.ipaLogin()).done(function () {
            if (add) {
                $.when(panel.checkForExistingACLPolicy(panel.props.uuid, "login")).done(function (existsRes) {
                    // if no ACL policy exists, then create it
                    if (existsRes["result"]["count"] === 0) {
                        panel.createLoginAclPolicy(panel.props.uuid, username).done(function (result) {

                            console.log(result);

                            // success keys in the returned json (see ipa.js creation methods for json structure)
                            if (result["GroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedUsersHostsToGroupAndServicesToRule"] === true) {
                                iziToast.hide({}, toast);
                                successToast.message = "Login ACL Policy Created Successfully! Added " + username + " to the Login ACL Policy";
                                iziToast.show(successToast);
                            } else {
                                iziToast.hide({}, toast);
                                errorToast.message = "Login ACL Policy Creation Failed!\n" + parseACLPolicyResult(result);
                                iziToast.show(errorToast);
                            }
                        });
                    } else {
                        // just add the user to the existing policy by adding them to the right user group
                        var usergroupLogin = "ug-login-" + panel.props.uuid;
                        panel.addUsersToUserGroup(username, usergroupLogin).done(function (result) {

                            console.log(result);

                            if (result["error"] === null && result["result"]["completed"] === 1) {
                                iziToast.hide({}, toast);
                                successToast.message = "Added " + username + " to the Login ACL Policy!";
                                iziToast.show(successToast);
                            } else {
                                iziToast.hide({}, toast);
                                errorToast.message = "Could not add " + username + " to the Login ACL Policy. Ensure " + username + " is registered with the IPA server.";
                                iziToast.show(errorToast);
                            }
                        });
                    }
                }).fail(function (err) {
                    // if something fails due not due to the content of the request                
                    panel.removeACLPolicy(panel.props.uuid, "login");
                    iziToast.hide({}, toast);
                    errorToast.message = "Request could not be completed. Error: " + JSON.stringify(err);
                    iziToast.show(errorToast);
                });
            } else {
                panel.removeUserFromACLPolicy(username, panel.props.uuid, "login").done(function (result) {
                    // if no error
                    if (result["error"] === null && result["result"]["completed"] === 1) {
                        iziToast.hide({}, toast);
                        successToast.message = "Removed " + username + " from Login ACL Policy";
                        iziToast.show(successToast);
                    } else {
                        iziToast.hide({}, toast);
                        errorToast.message = "Not able to remove " + username + " from Login ACL Policy. Ensure " + username + " is in the ACL policy";
                        iziToast.show(errorToast);
                    }

                    // if the login checkbox is unchecked,
                    // then uncheck (if checked) the sudo checkbox (which should remove the sudo access for the user)
                    if ($("td input[data-username=\"" + username + "\"][data-access=\"sudo\"]").is(":checked")) { panel.updateSudo(username, false); }
                });
            }
        });
    }
    updateSudo(username, add) {
        let panel = this;
        const loadingToast = {
            theme: "dark",
            icon: "fas fa-ban",
            close: "false",
            title: "Processing Request",
            position: "topRight",
            progressBarColor: "green",
            pauseOnHover: false,
            timeout: 15000,
            displayMode: "replace",
            onClosing: function (instance, toast, closedBy) {
                if (closedBy === "timeout") {
                    panel.removeACLPolicy(panel.props.uuid, "sudo");
                    errorToast.message = "The ACL Request could not be completed. Please try again.";
                    iziToast.show(errorToast);
                    console.log("removing policy due to timeout");
                }
            }
        };
        // show a loading alert, if after 15 seconds the process doesn't complete - show an error message
        iziToast.show(loadingToast);
        let toast = document.querySelector(".iziToast");
        // ensure the user is logged in before making any IPA requests
        $.when(panel.ipaLogin()).done(function () {
            if (add) {
                $.when(panel.checkForExistingACLPolicy(panel.props.uuid, "sudo")).done(function (existsRes) {
                    // if no ACL policy exists, then create it
                    if (existsRes["result"]["count"] === 0) {
                        panel.createSudoAclPolicy(panel.props.uuid, username).done(function (result) {
                            if (result["GroupAndRuleCreatedAndRightHostsFound"] === true && result["AddedUsersHostsToGroupAndServicesToRule"] === true) {
                                iziToast.hide({}, toast);
                                successToast.message = "Sudo ACL Policy Created Successfully. Added " + username + " to the Sudo ACL Policy";
                                iziToast.show(successToast);
                            } else {
                                iziToast.hide({}, toast);
                                errorToast.message = "Sudo ACL Policy Creation Failed!\n" + parseACLPolicyResult(result);
                                iziToast.show(errorToast);
                            }
                        });
                    } else {
                        // just add the user to the existing policy by adding them to the right user group
                        var usergroupSudo = "ug-sudo-" + panel.props.uuid;
                        panel.addUsersToUserGroup(username, usergroupSudo).done(function (result) {
                            if (result["error"] === null && result["result"]["completed"] === 1) {
                                iziToast.hide({}, toast);
                                successToast.message = "Added " + username + " to the Sudo ACL Policy";
                                iziToast.show(successToast);
                            } else {
                                iziToast.hide({}, toast);
                                errorToast.message = "Could not add " + username + " to the Sudo ACL Policy. Ensure " + username + " is registered with the IPA server.";
                                iziToast.show(errorToast);
                            }
                        });
                    }

                    if (!$("td input[data-username=\"" + username + "\"][data-access=\"login\"]").is(":checked")) { panel.updateLogin(username, true); }
                }).fail(function (err) {
                    // if something fails due not due to the content of the request                
                    panel.removeACLPolicy(panel.props.uuid, "sudo");
                    iziToast.hide({}, toast);
                    errorToast.message = "Request could not be completed. Error: " + JSON.stringify(err);
                    iziToast.show(errorToast);
                });
            } else {
                panel.removeUserFromACLPolicy(username, panel.props.uuid, "sudo").done(function (result) {
                    // if no error
                    if (result["error"] === null && result["result"]["completed"] === 1) {
                        iziToast.hide({}, toast);
                        successToast.message = "Removed " + username + " from Sudo ACL Policy";
                        iziToast.show(successToast);
                    } else {
                        iziToast.hide({}, toast);
                        errorToast.message = "Not able to remove " + username + " from Sudo ACL Policy. Ensure " + username + " is in the ACL policy";
                        iziToast.show(errorToast);
                    }
                });
            }
        });
    }

    /* IPA Functions */
    /* INFO */
    ipaLogin() {
        let panel = this;
        var apiUrl = window.location.origin + "/StackV-web/restapi/app/acl/ipa/login";
        return $.ajax({
            url: apiUrl,
            type: "POST",
            beforeSend: function (xhr) {
                // check here if the user is already logged in and the cookie did not expire
                xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
            },
            success: function (result) {
                //console.log("ipaLogin success: " + JSON.stringify(result));
                if (result["Result"] === "Login Successful") {
                    return true;
                } else {
                    return false;
                }
            },
            error: function (err) {
                console.log("ipaLogin error: " + JSON.stringify(err));
            }
        });
    }
    isUserInAclPolicy(serviceUUID, accessType, username) {
        /**
        * Try to find an user (hence user_find) that is in both the correct
        * user group and hbac rule
        */
        var ipaRequestData = {
            "method": "user_find",
            "params": [
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
            "id": 0
        };
        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    checkForExistingACLPolicy(serviceUUID, accessType) {
        var hbacrule = "hbac-" + accessType + "-" + serviceUUID;

        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_find",
            "params": [
                [hbacrule],
                {}
            ],
            "id": 0
        };


        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    getHostsForServiceInstance(serviceUUID) {
        var apiUrl = window.location.origin + "/StackV-web/restapi/service/manifest/" + serviceUUID;

        // ajax call fields
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": apiUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/xml",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": "<serviceManifest>\n<serviceUUID></serviceUUID>\n<jsonTemplate>\n{\n    \"hostgroup\": [\n        {\n          \"hostname\": \"?fqdn?\",\n          \"sparql\": \"SELECT DISTINCT ?fqdn WHERE {?hypervisor mrs:providesVM ?vm. ?vm mrs:hasNetworkAddress ?na. ?na mrs:type \\\"fqdn\\\". ?na mrs:value ?fqdn.}\",\n          \"required\": \"false\"\n        }\n      ]\n}\n</jsonTemplate>\n</serviceManifest>"
        };

        return $.ajax(settings);
    }


    /* ADDITION */
    createLoginAclPolicy(serviceUUID, username) {
        let panel = this;
        // start by creating login access as both login and sudo require login access
        var ugLoginName = "ug-login-" + serviceUUID;
        var hgLoginName = "hg-login-" + serviceUUID;
        var hbacLoginName = "hbac-login-" + serviceUUID;
        var loginServices = ["login", "sshd"];
        var hosts = [];
        var ugError = null;
        var hgError = null;
        var hbacError = null;
        var hostsQueryError = true;

        var aclLoginPolicyResult = {}; // currently a way to debug errors

        // need to change it so when all the ajax calls are done - then return the aclPolicyResult

        // the user group, host group, hbac rule all need to created before any
        // (mainly adding users, hosts, and services) is done to them

        var createLoginUg = panel.createUserGroup(ugLoginName, "Login user group for service instance: " + serviceUUID);
        var createLoginHg = panel.createHostGroup(hgLoginName, "Login host group for service instance: " + serviceUUID);
        var createLoginHbac = panel.createHBACRule(hbacLoginName, "Login HBAC Rule (login,ssh) for service instance: " + serviceUUID);
        var getLoginHosts = panel.getHostsForServiceInstance(serviceUUID);

        return $.when(createLoginUg, createLoginHg, createLoginHbac, getLoginHosts).done(function (ug, hg, hbac, hostsQuery) {
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
                    hostsObjs.forEach(function (h) {
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

        }).then(function () {

            // if no errors in all three creation/query calls -> continue the process
            // null is a falsy value
            if (!ugError && !hgError && !hbacError && !hostsQueryError) {
                aclLoginPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = true;

                var addLoginUgUsers = panel.addUsersToUserGroup(username, ugLoginName);
                var addLoginHgHosts = panel.addHostsToHostGroup(hosts, hgLoginName);
                var addLoginUgToHbac = panel.addUserGroupToHBACRule(ugLoginName, hbacLoginName);
                var addLoginHgToHbac = panel.addHostGroupToHBACRule(hgLoginName, hbacLoginName);
                var addLoginSrvcsToHbac = panel.addServicesToHBACRule(loginServices, hbacLoginName);

                return $.when(addLoginUgUsers, addLoginHgHosts, addLoginUgToHbac, addLoginHgToHbac, addLoginSrvcsToHbac)
                    .then(function (ugusers, hghosts, ughbac, hghbac, srvcshbac) {
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
                            panel.removeACLPolicy(serviceUUID, "login");
                        }
                        return aclLoginPolicyResult;
                    }).fail(function (err) {
                        console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
                    });
            } else {
                aclLoginPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = false;
                // if something went wrong - delete the ACL policy
                panel.removeACLPolicy(serviceUUID, "login");
                return aclLoginPolicyResult;
            }
        }).fail(function (err) {
            console.log("IPA ACL Login policy creation failed: " + JSON.stringify(err));
        });
    }
    createSudoAclPolicy(serviceUUID, username) {
        let panel = this;
        var ugSudoName = "ug-sudo-" + serviceUUID;
        var hgSudoName = "hg-sudo-" + serviceUUID;
        var hbacSudoName = "hbac-sudo-" + serviceUUID;
        var sudoServices = ["login", "sshd", "sudo"];
        var aclSudoPolicyResult = {}; // currently a way to debug errors
        var hosts = [];
        var ugError = null;
        var hgError = null;
        var hbacError = null;
        var hostsQueryError = true;

        // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult

        // the user group, host group, hbac rule all need to created before any
        // (mainly adding users, hosts, and services) is done to them

        var createSudoUg = panel.createUserGroup(ugSudoName, "Sudo user group for service instance: " + serviceUUID);
        var createSudoHg = panel.createHostGroup(hgSudoName, "Sudo host group for service instance: " + serviceUUID);
        var createSudoHbac = panel.createHBACRule(hbacSudoName, "Sudo HBAC Rule (login,ssh,sudo) for service instance: " + serviceUUID);
        var getSudoHosts = panel.getHostsForServiceInstance(serviceUUID);

        return $.when(createSudoUg, createSudoHg, createSudoHbac, getSudoHosts).done(function (ug, hg, hbac, hostsQuery) {
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
                    hostsObjs.forEach(function (h) {
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


        }).then(function () {

            // if no errors in all three -> null is a falsy value
            if (!ugError && !hgError && !hbacError && !hostsQueryError) {
                aclSudoPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = true;

                var addSudoUgUsers = panel.addUsersToUserGroup(username, ugSudoName);
                var addSudoHgHosts = panel.addHostsToHostGroup(hosts, hgSudoName);
                var addSudoUgToHbac = panel.addUserGroupToHBACRule(ugSudoName, hbacSudoName);
                var addSudoHgToHbac = panel.addHostGroupToHBACRule(hgSudoName, hbacSudoName);
                var addSudoSrvcsToHbac = panel.addServicesToHBACRule(sudoServices, hbacSudoName);

                return $.when(addSudoUgUsers, addSudoHgHosts, addSudoUgToHbac, addSudoHgToHbac, addSudoSrvcsToHbac)
                    .then(function (ugusers, hghosts, ughbac, hghbac, srvcshbac) {
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
                            panel.removeACLPolicy(serviceUUID, "sudo");
                        }
                        return aclSudoPolicyResult;
                    }).fail(function (err) {
                        console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
                    });
            } else {
                aclSudoPolicyResult["GroupAndRuleCreatedAndRightHostsFound"] = false;
                // if something went wrong - delete the ACL policy
                panel.removeACLPolicy(serviceUUID, "sudo");
                return aclSudoPolicyResult;
            }
        }).fail(function (err) {
            console.log("IPA ACL Sudo policy creation failed: " + JSON.stringify(err));
        });
    }

    createUserGroup(groupName, desc) {
        // creating the IPA request
        var ipaRequestData = {
            "method": "group_add",
            "params": [
                [groupName],
                { "description": desc }
            ],
            "id": 0
        };

        // ajax call fields
        // in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    createHostGroup(groupName, desc) {
        // creating the IPA request
        var ipaRequestData = {
            "method": "hostgroup_add",
            "params": [
                [groupName],
                { "description": desc }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    createHBACRule(ruleName, desc) {
        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_add",
            "params": [
                [ruleName],
                { "description": desc }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    addUsersToUserGroup(users, userGroup) {
        // if only a single user is provided - make sure it is enclosed in an array
        if (!Array.isArray(users)) {
            users = [users];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "group_add_member",
            "params": [
                [userGroup],
                { "user": users }
            ],
            "id": 0
        };


        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    addHostsToHostGroup(hosts, hostGroupName) {
        let panel = this;
        if (!Array.isArray(hosts)) {
            hosts = [hosts];
        }

        var badHosts = [];
        // before adding the hosts - make sure each one exists by creating them
        hosts.forEach(function (h) {
            $.when(panel.createHost(h)).done(function (hRes) {

                /**
                * if error is null (in which result will not be null) then the new host
                * was successfully added. In the event that error is not null (indicating
                * an error) then check the error code.
                */
                if (hRes["error"] !== null && hRes["error"]["code"] !== 4002) {
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
            return { "BadHosts": badHosts };
        }


        // creating the IPA request
        var ipaRequestData = {
            "method": "hostgroup_add_member",
            "params": [
                [hostGroupName],
                { "host": hosts }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    addUserGroupToHBACRule(userGroup, hbacRule) {
        if (!Array.isArray(userGroup)) {
            userGroup = [userGroup];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_add_user",
            "params": [
                [hbacRule],
                { "group": userGroup }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    addHostGroupToHBACRule(hostGroup, hbacRule) {
        if (!Array.isArray(hostGroup)) {
            hostGroup = [hostGroup];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_add_host",
            "params": [
                [hbacRule],
                { "hostgroup": hostGroup }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    addServicesToHBACRule(services, hbacRule) {
        if (!Array.isArray(services)) {
            services = [services];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_add_service",
            "params": [
                [hbacRule],
                { "hbacsvc": services }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    createHost(hostName) {
        // creating the IPA request
        // host must be valid (even if not registered with the IPA server or have FreeIPA installed)
        // otherwise this error may show up: "Host 'hostName' does not have corresponding DNS A/AAAA record"
        // Host must unique so duplicate host names are not allowed, will return an error if a duplicate entry is added
        var ipaRequestData = {
            "method": "host_add",
            "params": [
                [hostName],
                {}
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }


    /* DELETION */
    removeACLPolicy(serviceUUID, accessType) {
        let panel = this;
        var ugName = "ug-" + accessType + "-" + serviceUUID;
        var hgName = "hg-" + accessType + "-" + serviceUUID;
        var hbacName = "hbac-" + accessType + "-" + serviceUUID;
        var removeAclPolicyResult = {}; // currently a way to debug errors

        // need to change it so when all the ajax calls are done - then return the aclSudoPolicyResult

        // the user group, host group, hbac rule all need to created before any
        // (mainly adding users, hosts, and services) is done to them

        var deleteUg = panel.deleteUserGroup(ugName);
        var deleteHg = panel.deleteHostGroup(hgName);
        var deleteHbac = panel.deleteHBACRule(hbacName);

        return $.when(deleteUg, deleteHg, deleteHbac).done(function (delUg, delHg, delHbac) {
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
    deleteUserGroup(usergroupName) {
        if (!Array.isArray(usergroupName)) {
            usergroupName = [usergroupName];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "group_del",
            "params": [
                usergroupName,
                {}
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    deleteHostGroup(hostgroupName) {
        if (!Array.isArray(hostgroupName)) {
            hostgroupName = [hostgroupName];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "hostgroup_del",
            "params": [
                hostgroupName,
                {}
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    deleteHBACRule(hbacruleName) {
        if (!Array.isArray(hbacruleName)) {
            hbacruleName = [hbacruleName];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "hbacrule_del",
            "params": [
                hbacruleName,
                {}
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
    removeUserFromACLPolicy(username, serviceUUID, accessType) {
        var usergroup = "ug-" + accessType + "-" + serviceUUID;

        if (!Array.isArray(username)) {
            username = [username];
        }

        // creating the IPA request
        var ipaRequestData = {
            "method": "group_remove_member",
            "params": [
                [usergroup],
                { "user": username }
            ],
            "id": 0
        };

        // ajax call fields
        // future use: in the beforeSend field, if false is return the request will be cancelled. Can be used to check if the user is logged in
        var ipaAjaxCall = {
            "url": this.state.ipaUrl,
            "method": "POST",
            "headers": {
                "Content-Type": "application/json",
                "Authorization": "bearer " + this.props.keycloak.token
            },
            "data": JSON.stringify(ipaRequestData)
        };

        return $.ajax(ipaAjaxCall);
    }
}
UserPanel.propTypes = {
};
export default UserPanel;

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


    // if none of the above errors were caught - just output the stringified json
    if (prettyResults.length === 0) {
        prettyResults += "Unkown error. Error output: " + JSON.stringify(resultJSON);
    }

    return prettyResults;
}
