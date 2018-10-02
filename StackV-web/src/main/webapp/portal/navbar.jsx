import React from "react";
import PropTypes from "prop-types";
import { cx, css } from "emotion";

const stack_nav = css`
    a {
        pointer-events: none;
    }
    ul li:hover {
        background-color: #000;
        color: rgb(51,51,51);
    }
`;

class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};

        this.logout = this.logout.bind(this);
        this.manageAccount = this.manageAccount.bind(this);
    }

    logout() {
        this.props.keycloak.logout();
    }
    manageAccount() {
        this.props.keycloak.accountManagement();
    }

    render() {
        return <nav className={cx(stack_nav, "navbar", "navbar-inverse")}>
            <div className="container-fluid">
                <ul className="nav navbar-nav navbar-left" style={{ width: "100%" }}>
                    <li className={this.props.page === "visualization" ? "active" : ""} id="visualization-tab" onClick={(e) => this.props.switchPage("visualization", e)}><a>Visualization</a></li>
                    <li className={this.props.page === "catalog" ? "active" : ""} id="catalog-tab" onClick={(e) => this.props.switchPage("catalog")}><a>Catalog</a></li>
                    <li className={this.props.page === "details" ? "active" : ""} id="details-tab" onClick={(e) => this.props.switchPage("details")}><a>Details</a></li>
                    <li className={this.props.page === "driver" ? "active" : ""} id="driver-tab" onClick={(e) => this.props.switchPage("driver")}><a>Drivers</a></li>
                    <li className={this.props.page === "acl" ? "active" : ""} id="acl-tab" onClick={(e) => this.props.switchPage("acl")}><a>ACL</a></li>

                    <li className="pull-right" id="logout-button" onClick={this.logout}><a href="#">Logout</a></li>
                    <li className="pull-right" id="account-button" onClick={this.manageAccount}><a href="#">Account</a></li>
                    <li id="admin-tab" className={this.props.page === "admin" ? "active pull-right nav-admin" : "pull-right nav-admin"} onClick={(e) => this.props.switchPage("admin", e)}> <a>Admin</a></li>
                </ul>
            </div>
        </nav>;
    }
}
Navbar.propTypes = {
    page: PropTypes.string.isRequired,
    keycloak: PropTypes.object.isRequired,
    switchPage: PropTypes.func.isRequired
};
export default Navbar;
