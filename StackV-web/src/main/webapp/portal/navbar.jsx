import React from "react";
import PropTypes from "prop-types";
import { cx, css } from "emotion";

import LoaderBubble from "./loader_bubble";

const stack_nav = css`
    border-radius: 0 0 20px 20px !important;
    z-index: 998;

    a {
        pointer-events: none;
    }
    ul li:hover {
        background-color: #000;
        color: rgb(51,51,51);
    }
`;
const loadStyle = css`
    display: inline;
    float: right;
    margin: 15px;
`;

class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.verifyRoles = this.verifyRoles.bind(this);
        this.state = this.verifyRoles();
    }

    render() {
        return <nav className={cx(stack_nav, "navbar", "navbar-inverse")}>
            <div className="container-fluid">
                <NavElements {...this.state} {...this.props} />
            </div>
        </nav>;
    }

    verifyRoles() {
        let roles = this.props.keycloak.tokenParsed.realm_access.roles;
        if (roles.indexOf("A_Admin") <= -1) {
            $(".nav-admin").hide();
        }

        let navSet = ["visualization", "catalog", "details", "drivers"];

        if (!roles.includes("F_Drivers-R")) {
            navSet.splice(navSet.indexOf("drivers"), 1);
        }
        if (!roles.includes("F_Visualization-R")) {
            navSet.splice(navSet.indexOf("visualization"), 1);
        }

        return { navSet: navSet };
    }
}
Navbar.propTypes = {
    page: PropTypes.string.isRequired,
    keycloak: PropTypes.object.isRequired,
    switchPage: PropTypes.func.isRequired
};
export default Navbar;

function NavElements(props) {
    const listItems = props.navSet.map((d) =>
        <li key={d} className={props.page === d ? "active" : ""} id={d + "-tab"} onClick={(e) => props.switchPage(d, e)}><a>{d[0].toUpperCase() + d.substr(1)}</a></li>
    );

    function logout() {
        props.keycloak.logout();
    }
    function manageAccount() {
        props.keycloak.accountManagement();
    }

    return <ul className="nav navbar-nav navbar-left" style={{ width: "100%" }}>
        {listItems}
        <li className="pull-right" id="logout-button" onClick={logout}><a href="#">Logout</a></li>
        <li className="pull-right" id="account-button" onClick={manageAccount}><a href="#">Account</a></li>
        <li id="admin-tab" className={props.page === "admin" ? "active pull-right nav-admin" : "pull-right nav-admin"} onClick={(e) => props.switchPage("admin", e)}> <a>Admin</a></li>
        <div className={loadStyle}><LoaderBubble {...props} /></div>
    </ul>;
}