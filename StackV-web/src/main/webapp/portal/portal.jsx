import React from "react";
//import Keycloak from "keycloak-js";
import iziToast from "izitoast";
import Mousetrap from "mousetrap";
import "bootstrap";
import ReactInterval from "react-interval";

import "./global.css";

import Navbar from "./nav/navbar";
import Visualization from "./visual/visualization";
import Catalog from "./catalog/catalog";
import Details from "./details/details";
import Drivers from "./drivers/drivers";
import Admin from "./admin/admin";

import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";
import { far } from "@fortawesome/free-regular-svg-icons";

library.add(far, fas);

const detailsErrorToast = {
    theme: "dark",
    icon: "fas fa-exclamation",
    title: "Details Unavailable",
    message: "No service instance selected!",
    position: "topRight",
    progressBarColor: "red",
    pauseOnHover: true,
    timeout: 5000,
    displayMode: "replace"
};
const accessDeniedToast = {
    theme: "dark",
    icon: "fas fa-ban",
    title: "Access Denied",
    message: "You do not have access to this resource.",
    position: "topRight",
    progressBarColor: "red",
    pauseOnHover: true,
    timeout: 3000,
    displayMode: "replace"
};

class Portal extends React.Component {
    constructor(props) {
        super(props);
        let keycloak = window.keycloak;

        this.verifyPageAccess = this.verifyPageAccess.bind(this);
        this.state = {
            keycloak: keycloak,
            visualMode: "new",
            page: "catalog",
            loading: false,
            refreshTimer: 1000,
            refreshEnabled: false,
        };

        this.loadPage = this.loadPage.bind(this);
        this.switchPage = this.switchPage.bind(this);
        this.viewShift = this.viewShift.bind(this);
        this.pauseRefresh = this.pauseRefresh.bind(this);
        this.resumeRefresh = this.resumeRefresh.bind(this);
        this.setRefresh = this.setRefresh.bind(this);
        this.healthCheck = this.healthCheck.bind(this);
        this.tokenCheck = this.tokenCheck.bind(this);
        this.frameLoad = this.frameLoad.bind(this);
        this.checkRegistration = this.checkRegistration.bind(this);

        Mousetrap.bind("shift+left", () => { this.viewShift("left"); });
        Mousetrap.bind("shift+right", () => { this.viewShift("right"); });
    }
    componentDidMount() {
        this.checkRegistration();
    }
    verifyPageAccess(newPage, param) {
        let portal = this;
        let roles = this.state.keycloak.tokenParsed.realm_access.roles;

        switch (newPage) {
            case "admin":
                return roles.indexOf("A_Admin") > -1;
            case "details":
                if (roles.indexOf("A_Admin") === -1 && (param && param.uuid)) {
                    let apiUrl = window.location.origin + "/StackV-web/restapi/app/access/instances/" + param.uuid;
                    let res;
                    $.ajax({
                        url: apiUrl,
                        async: false,
                        type: "GET",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
                        },
                        success: function (result) {
                            res = result;
                        },
                        error: function (err) {
                            return false;
                        }
                    });

                    return res;
                }
                else { return true; }
            case "driver":
                return roles.indexOf("F_Drivers-R") > -1;
            case "visualization":
                return roles.indexOf("F_Visualization-R") > -1;
            default: return true;
        }
    }

    switchPage(page, param) {
        if (this.verifyPageAccess(page, param)) {
            switch (page) {
                case "details":
                    if (param && param.uuid) {
                        this.setState({ page: "details", uuid: param.uuid, refreshEnabled: false });
                        sessionStorage.setItem("instance-uuid", param.uuid);
                    } else if (this.state.uuid) {
                        this.setState({ page: "details", uuid: this.state.uuid, refreshEnabled: false });
                        sessionStorage.setItem("instance-uuid", this.state.uuid);
                    } else {
                        iziToast.show(detailsErrorToast);
                    }
                    break;
                case "visualization":
                    if (param.shiftKey) {
                        this.setState({ page: "visualization", refreshEnabled: false });
                    } else {
                        window.location.replace("/StackV-web/portal/visual/graphTest.jsp");
                    }
                    break;
                default:
                    this.setState({ page: page, refreshEnabled: false });
            }
        } else {
            iziToast.show(accessDeniedToast);
        }
    }

    healthCheck() {
        let portal = this;
        var apiUrl = window.location.origin + "/StackV-web/restapi/service/ready";
        $.ajax({
            url: apiUrl,
            type: "GET",
            dataType: "json",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
            },
            success: function (result) {
                portal.setState({ "systemHealth": result });
            },
            error: function (err) {
                console.log("Error in system health check: " + JSON.stringify(err));
            }
        });
    }
    tokenCheck() {
        this.state.keycloak.updateToken(45);
    }

    render() {
        return <div>
            <ReactInterval timeout={10000} enabled={true} callback={this.tokenCheck} />
            <ReactInterval timeout={this.state.refreshTimer} enabled={this.state.refreshEnabled} callback={this.healthCheck} />
            <Navbar {...this.state} switchPage={this.switchPage} pauseRefresh={this.pauseRefresh} resumeRefresh={this.resumeRefresh} setRefresh={this.setRefresh} ></Navbar>
            <div id="main-pane">
                {this.loadPage()}
            </div>
        </div>;
    }

    /* */
    frameLoad(time) {
        let page = this;
        this.setState({ loading: true });
        setTimeout(function () {
            page.setState({ loading: false });
        }, time);
    }
    pauseRefresh() {
        this.setState({ refreshEnabled: false });
    }
    resumeRefresh() {
        this.setState({ refreshEnabled: true });
    }
    setRefresh(time) {
        this.setState({ refreshTimer: parseInt(time) });
    }
    loadPage() {
        switch (this.state.page) {
            case "visualization":
                return <Visualization {...this.state} visualMode={this.state.visualMode} keycloak={this.state.keycloak} />;
            case "catalog":
                return <Catalog {...this.state} switchPage={this.switchPage} pauseRefresh={this.pauseRefresh} resumeRefresh={this.resumeRefresh} />;
            case "details":
                return <Details {...this.state} pauseRefresh={this.pauseRefresh} resumeRefresh={this.resumeRefresh} frameLoad={this.frameLoad} />;
            case "drivers":
                return <Drivers {...this.state} switchPage={this.switchPage} pauseRefresh={this.pauseRefresh} resumeRefresh={this.resumeRefresh} />;
            case "admin":
                return <Admin {...this.state} pauseRefresh={this.pauseRefresh} resumeRefresh={this.resumeRefresh} frameLoad={this.frameLoad} />;
            default:
                return <div></div>;
        }
    }
    viewShift(dir) {
        switch (dir) {
            case "left":
                switch (this.state.page) {
                    case "visualization":
                        break;
                    case "catalog":
                        this.switchPage("visualization");
                        break;
                    case "details":
                        this.switchPage("catalog");
                        break;
                }
                break;
            case "right":
                switch (this.state.page) {
                    case "visualization":
                        this.switchPage("catalog");
                        break;
                    case "catalog":
                        this.switchPage("details");
                        break;
                    case "details":
                        this.switchPage("driver");
                        break;
                }
        }
    }
    checkRegistration() {
        let portal = this;
        $.ajax({
            url: window.location.origin + "/StackV-web/restapi/config/system.name",
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
                xhr.setRequestHeader("Refresh", portal.state.keycloak.token);
            },
            success: function (name) {
                if (name) {
                    console.log("Orchestrator " + name + " online.");
                } else {
                    iziToast.question({
                        drag: false,
                        timeout: false,
                        close: false,
                        overlay: true,
                        displayMode: 1,
                        id: "question",
                        progressBar: true,
                        title: "Server Registration",
                        message: "Please register a server name:",
                        position: "center",
                        inputs: [
                            ["<input type=\"text\">", "keyup", function (instance, toast, input, e) {
                                //console.info(e);                                
                            }, true]
                        ],
                        buttons: [
                            ["<button><b>Confirm</b></button>", function (instance, toast, button, e, inputs) {
                                console.info(button);
                                console.info(e);
                                $.ajax({
                                    url: window.location.origin + "/StackV-web/restapi/config/system.name/" + encodeURIComponent(inputs[0].value),
                                    async: false,
                                    type: "PUT",
                                    beforeSend: function (xhr) {
                                        xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
                                        xhr.setRequestHeader("Refresh", portal.state.keycloak.token);
                                    },
                                    success: function () {
                                        $.ajax({
                                            url: window.location.origin + "/StackV-web/restapi/md2/register/",
                                            async: false,
                                            type: "POST",
                                            beforeSend: function (xhr) {
                                                xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
                                                xhr.setRequestHeader("Refresh", portal.state.keycloak.token);
                                            },
                                            success: function () {
                                                iziToast.success({
                                                    id: "success",
                                                    zindex: 9999,
                                                    timeout: 2000,
                                                    title: "Success",
                                                    overlay: true,
                                                    message: "Orchestrator registered.",
                                                    position: "center"
                                                });
                                            },
                                            error: function () {
                                                iziToast.error({
                                                    id: "error",
                                                    zindex: 9999,
                                                    timeout: 2000,
                                                    title: "Error",
                                                    overlay: true,
                                                    message: "Orchestrator unable to be registered. Please refresh and try again.",
                                                    position: "center"
                                                });
                                            }
                                        });
                                    },
                                    error: function () {
                                        iziToast.error({
                                            id: "error",
                                            zindex: 9999,
                                            timeout: 2000,
                                            title: "Error",
                                            overlay: true,
                                            message: "Orchestrator unable to be saved. Please refresh and try again.",
                                            position: "center"
                                        });
                                    }
                                });
                                instance.hide({ transitionOut: "fadeOut" }, toast, "button");
                                /* iziToast.success({
                                     id: 'success',
                                     zindex: 9999,
                                     timeout: 2000,
                                     title: 'Success',
                                     overlay: true,
                                     message: 'Thank you',
                                     position: 'center'
                                 });*/
                            }, false], // true to focus                            
                        ]
                    });
                }
            }
        });
    }
}
export default Portal;