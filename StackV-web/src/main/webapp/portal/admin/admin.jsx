import React from "react";
import PropTypes from "prop-types";
import Mousetrap from "mousetrap";
import { RotateLoader } from "react-spinners";

import "./admin.css";

import AdminDots from "./components/admin_dots";
import LoggingPanel from "../datatables/logging_panel";
import APIPanel from "./components/admin_api";
import SettingsPanel from "./components/admin_settings";

class Admin extends React.Component {
    constructor(props) {
        super(props);

        this.viewShift = this.viewShift.bind(this);

        let page = this;
        Mousetrap.bind("left", function () { page.viewShift("left"); });
        Mousetrap.bind("right", function () { page.viewShift("right"); });
        this.setView = this.setView.bind(this);
        this.state = {
            view: "settings",
        };

    }
    componentDidMount() {
        this.props.resumeRefresh();
    }
    setView(panel) {
        this.setState({ view: panel });
    }

    render() {
        let modView = [];
        switch (this.state.view) {
            case "api":
                modView = [false, false, true];
                break;
            case "settings":
                modView = [false, true, false];
                break;
            case "logging":
                modView = [true, false, false];
                break;
        }

        return <div style={{ width: "100%", height: "100%" }}>
            <RotateLoader
                css={"display: block;position: absolute;margin: auto;left: 50%;top: 30%;z-index: 100;"}
                sizeUnit={"px"}
                size={15}
                color={"#7ED321"}
                loading={this.props.loading}
            />
            <div className={this.props.loading ? "page page-admin loading" : "page page-admin"}>
                <AdminDots view={this.state.view} setView={this.setView}></AdminDots>

                <LoggingPanel {...this.props} active={modView[0]}></LoggingPanel>
                <SettingsPanel {...this.props} active={modView[1]} />
                <APIPanel {...this.props} active={modView[2]} />
            </div>
        </div>;
    }

    // =============== //
    viewShift(dir) {
        switch (this.state.view) {
            case "logging":
                if (dir === "right") {
                    this.setState({ view: "settings" });
                }
                break;
            case "settings":
                if (dir === "left") {
                    this.setState({ view: "logging" });
                }
                if (dir === "right") {
                    this.setState({ view: "api" });
                }
                break;
            case "api":
                if (dir === "left") {
                    this.setState({ view: "settings" });
                }
                break;

        }
    }
}
Admin.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Admin;