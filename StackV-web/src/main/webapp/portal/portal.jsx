import React from "react";
//import Keycloak from "keycloak-js";
import iziToast from "izitoast";
import Mousetrap from "mousetrap";

import "./global.css";

import Navbar from "./navbar";
import Visualization from "./visual/visualization";
import Catalog from "./catalog/catalog";
import Details from "./details/details";

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
        let keycloakIntervalRef = setInterval(() => {
            keycloak.updateToken(45);
        }, 20000);

        this.state = {
            keycloak: keycloak,
            keycloakIntervalRef: keycloakIntervalRef,
            visualMode: "new",
            page: "catalog"
        };

        this.loadPage = this.loadPage.bind(this);
        this.switchPage = this.switchPage.bind(this);
        this.viewShift = this.viewShift.bind(this);

        Mousetrap.bind("shift+left", () => { this.viewShift("left"); });
        Mousetrap.bind("shift+right", () => { this.viewShift("right"); });
    }
    componentDidMount() {
        /*const keycloak = Keycloak("/StackV-web/resources/keycloak.json");
        keycloak.init({ onLoad: "login-required" }).then(authenticated => {
            this.setState({ keycloak: keycloak, authenticated: authenticated });
        }).catch(function () {
            alert("failed to initialize");
        });

        $.ajaxSetup({
            cache: false,
            timeout: 60000,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            }
        });*/
    }
    componentWillUnmount() {
        clearInterval(this.state.keycloakIntervalRef);
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
                    $.ajax({
                        url: apiUrl,
                        async: false,
                        type: "GET",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader("Authorization", "bearer " + portal.state.keycloak.token);
                        },
                        success: function (result) {
                            return result;
                        },
                        error: function (err) {
                            return false;
                        }
                    });
                }
                else { return true; }
                break;
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
                        this.setState({ page: "details", uuid: param.uuid });
                    } else if (this.state.uuid) {
                        this.setState({ page: "details", uuid: this.state.uuid });
                    } else {
                        iziToast.show(detailsErrorToast);
                    }
                    break;
                case "visualization":
                    if (param.shiftKey) {
                        this.setState({ page: "visualization" });
                    } else {
                        window.location.replace("/StackV-web/portal/visual/graphTest.jsp");
                    }
                    break;
                default:
                    this.setState({ page: page });
            }
        } else {
            iziToast.show(accessDeniedToast);
        }
    }

    render() {
        return <div>
            <Navbar page={this.state.page} keycloak={this.state.keycloak} switchPage={this.switchPage}></Navbar>
            <div id="main-pane">
                {this.loadPage()}
            </div>
        </div>;
    }

    /* */
    loadPage() {
        switch (this.state.page) {
            case "catalog":
                return <Catalog keycloak={this.state.keycloak} switchPage={this.switchPage} />;
            case "details":
                return <Details keycloak={this.state.keycloak} uuid={this.state.uuid} />;
            case "visualization":
                return <Visualization visualMode={this.state.visualMode} keycloak={this.state.keycloak} />;
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
}
export default Portal;