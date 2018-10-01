import React from "react";
import PropTypes from "prop-types";
import Keycloak from "keycloak-js";
import iziToast from "izitoast";

import "./global.css";

import Catalog from "../catalog/catalog";
import Details from "../details/details";
import Navbar from "./navbar";

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
    timeout: 7000,
    displayMode: "once"
};

class Portal extends React.Component {
    constructor(props) {
        super(props);
        let keycloak = window.keycloak;

        this.verifyPageRoles = this.verifyPageRoles.bind(this);
        let keycloakIntervalRef = setInterval(() => {
            keycloak.updateToken(45);
        }, 20000);

        this.state = {
            keycloak: keycloak,
            keycloakIntervalRef: keycloakIntervalRef,
            page: "catalog"
        };

        this.loadPage = this.loadPage.bind(this);
        this.switchPage = this.switchPage.bind(this);

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

    verifyPageRoles() {
        return true;
    }


    loadPage() {
        switch (this.state.page) {
            case "catalog":
                return <Catalog keycloak={this.state.keycloak} switchPage={this.switchPage} />;
            case "details":
                return <Details keycloak={this.state.keycloak} uuid={this.state.uuid} />;
            default:
                return <div></div>;
        }
    }

    switchPage(page, param) {
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
            default:
                this.setState({ page: page });
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
}
export default Portal;