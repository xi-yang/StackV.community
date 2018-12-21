import React from "react";
import PropTypes from "prop-types";
import Mousetrap from "mousetrap";

import "./admin.css";

import AdminDots from "./components/admin_dots";
import LoggingPanel from "../datatables/logging_panel";
import APIPanel from "./components/admin_api";

class Admin extends React.Component {
    constructor(props) {
        super(props);

        this.viewShift = this.viewShift.bind(this);

        let page = this;
        Mousetrap.bind("left", function () { page.viewShift("left"); });
        Mousetrap.bind("right", function () { page.viewShift("right"); });
        this.setView = this.setView.bind(this);
        this.state = {
            view: "logging",
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
                modView = [true, false];
                break;
            case "logging":
                modView = [false, true];
                break;
        }
        return <div style={{ width: "100%", height: "100%" }}>
            <div className="page page-admin">
                <AdminDots view={this.state.view} setView={this.setView}></AdminDots>

                <APIPanel {...this.props} active={modView[0]} />
                <LoggingPanel {...this.props} active={modView[1]}></LoggingPanel>
            </div>
        </div>;
    }

    // =============== //
    viewShift(dir) {
        switch (this.state.view) {
            case "api":
                if (dir === "right") {
                    this.setState({ view: "logging" });
                }
                break;
            case "logging":
                if (dir === "left") {
                    this.setState({ view: "api" });
                }
                break;
        }
    }
}
Admin.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Admin;