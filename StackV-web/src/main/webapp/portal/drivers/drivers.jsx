import React from "react";
import PropTypes from "prop-types";
import iziToast from "izitoast";
import ReactInterval from "react-interval";
import { Set } from "immutable";

import DriverModal from "./components/driver_modal";
import "./drivers.css";

const unavailableToast = {
    theme: "dark",
    icon: "fas fa-exclamation",
    title: "Driver unavailable",
    message: "This driver was installed manually, and cannot be edited.",
    position: "topRight",
    progressBarColor: "red",
    pauseOnHover: true,
    timeout: 4000,
    displayMode: "replace"
};

class Drivers extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loading: false,
            data: [],
        };

        this.openDriver = this.openDriver.bind(this);
        this.resetDriverModal = this.resetDriverModal.bind(this);
        this.loadData = this.loadData.bind(this);

        this.plugDriver = this.plugDriver.bind(this);
        this.unplugDriver = this.unplugDriver.bind(this);

        this.enableDriver = this.enableDriver.bind(this);
        this.disableDriver = this.disableDriver.bind(this);
    }
    componentDidMount() {
        this.loadData();
        this.props.resumeRefresh();
    }

    loadData() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        $.ajax({
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                for (let driver of result) {
                    driver.disabled = (driver.disabled == "true");
                    driver.errors = parseInt(driver.errors);
                }

                page.setState({ data: result });
            }
        });
    }

    openDriver(urn, e) {
        if (e.target.tagName !== "BUTTON" || urn === null) {
            if ($(e.target.parentElement).hasClass("backend")) {
                iziToast.show(unavailableToast);
            } else {
                this.setState({ openDriver: this.state.data.find(x => x.urn === urn) });
                $("#driver-modal").modal("show");
                $("#driver-modal-body-select").val(null);
            }
        }
    }

    plugDriver(urn, status, e) {
        let page = this;
        let $row = $(e.target).parents("tr");
        if (status !== "Plugged") {
            let apiURL = window.location.origin + "/StackV-web/restapi/driver/";
            $.ajax({
                url: apiURL,
                type: "POST",
                contentType: "application/xml",
                data: page.state.data.find(x => x.urn === urn).xml,
                dataType: "xml",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                success: function (result) {
                    page.props.resumeRefresh();
                },
                error: function (err) {
                    page.props.resumeRefresh();
                }
            });
        }
    }
    unplugDriver(urn, status, e) {
        let page = this;
        let $row = $(e.target).parents("tr");
        if (status === "Plugged" && !$(e.target).hasClass("btn-danger")) {
            // Confirmation button
            e.persist();
            let cacheText = $(e.target).text();
            let cacheClass = e.target.className;

            e.target.className = "btn btn-sm btn-danger";
            $(e.target).text("Confirm Unplug");
        } else {
            // Unplug driver
            let apiURL = window.location.origin + "/StackV-web/restapi/driver/" + urn;
            $.ajax({
                url: apiURL,
                type: "DELETE",
                datatype: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                error: function (err) {
                    /* The format of result in case of an error looks like this:
                    * {"readyState":4,
                    * "responseText":"[507afdf3-11a7-4d9e-b97b-4aa604e2c722, 776f8022-2a84-403f-9255-4d9dbd30753b]",
                    * "status":409,"statusText":"Conflict"}
                    * The above is JS object. What we need are the UUIDs in the "responseText". However, the responsetext array is not formatted
                    * properly for JavaScript - its elements should be quoted as it mixes numbers and characters. So a string replace is needed
                    * in order to replace [ with [", ] with "], and commas with "," */

                    err = {
                        "readyState": 4,
                        "responseText": "[507afdf3-11a7-4d9e-b97b-4aa604e2c722, 776f8022-2a84-403f-9255-4d9dbd30753b]",
                        "status": 409, "statusText": "Conflict"
                    };

                    console.log("unplug error: " + JSON.stringify(err));
                    console.log("unplug error status: " + JSON.stringify(err["status"]));
                    console.log("unplug error statusText: " + JSON.stringify(err["statusText"]));
                }
            });
        }
    }

    enableDriver(urn) {
        let page = this;
        let apiURL = window.location.origin + "/StackV-web/restapi/driver/" + urn + "/disabled/false";
        $.ajax({
            url: apiURL,
            type: "PUT",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            }
        });
    }
    disableDriver(urn, disabled, e) {
        let page = this;
        if (!disabled && !$(e.target).hasClass("btn-danger")) {
            // Confirmation button
            e.persist();
            let cacheText = $(e.target).text();
            let cacheClass = e.target.className;

            e.target.className = "btn btn-sm btn-danger";
            $(e.target).text("Confirm Disable");
        } else {
            // Disable driver
            let apiURL = window.location.origin + "/StackV-web/restapi/driver/" + urn + "/disabled/true";
            $.ajax({
                url: apiURL,
                type: "PUT",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                }
            });
        }
    }

    resetDriverModal() {
        this.setState({ openDriver: undefined });
    }

    render() {
        let pageClasses = "stack-panel page page-details";
        if (this.state.loading) {
            pageClasses = "stack-panel page page-details loading";
        }
        return <div className={pageClasses}>
            <ReactInterval timeout={this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadData} />

            <div className="stack-header">
                <b>System Drivers</b>
            </div>
            <div className="stack-body" style={{ padding: "20px 10px" }}>
                <div id="drivers-table-div">
                    <table className="table table-hover">
                        <thead>
                            <tr>
                                <th>Driver URN</th>
                                <th>Type</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <DriverElements data={Set(this.state.data)} open={this.openDriver} plug={this.plugDriver} unplug={this.unplugDriver} enable={this.enableDriver} disable={this.disableDriver} confirm={this.confirm} />
                        <tfoot>
                            <tr>
                                <th colSpan="3">
                                    <div id="drivers-table-footer" style={{ cursor: "pointer" }} onClick={(e) => this.openDriver(null, e)}><i className="fas fa-plus-circle" style={{ marginRight: "10px" }} />Add New Driver</div>
                                </th>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <DriverModal {...this.state.openDriver} reset={this.resetDriverModal} {...this.props} />
        </div>;
    }
}
Drivers.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Drivers;

function DriverElements(props) {


    const listItems = props.data.map((d) => <tr key={d.urn} id={d.urn} className={d.status === "Plugged" ? (d.errors > 5 || d.disabled ? "warning" : "success") : undefined} onClick={(e) => props.open(d.urn, e)}>
        <td>
            <i className={d.status === "Plugged" ? (d.errors === 0 ? "fas fa-lg driver-health-icon fa-check-circle pass" : (d.errors < 5 ? "fas fa-lg driver-health-icon fa-exclamation-triangle warn" : "fas fa-lg driver-health-icon fa-exclamation-circle fail")) : undefined}></i>
            {d.urn}
        </td>
        <td>{d.type}</td>
        <td>
            {d.xml !== "<driverInstance><properties></properties></driverInstance>" ? (
                <div className="btn-group pull-right" role="group">
                    <button type="button" className={d.status === "Plugged" ? "btn btn-sm btn-default" : "btn btn-sm btn-primary"} onClick={(e) => props.unplug(d.urn, d.status, e)}>{d.status === "Plugged" ? "Unplug" : "Unplugged"}</button>
                    <button type="button" className={d.status === "Plugged" ? "btn btn-sm btn-primary" : "btn btn-sm btn-default"} onClick={(e) => props.plug(d.urn, d.status, e)}>{d.status === "Plugged" ? "Plugged" : "Plug"}</button>
                </div>
            ) : (<button type="button" className="btn btn-sm btn-default" onClick={(e) => props.unplug(d.urn, d.status, e)}>Unplug (Manually Installed)</button>)}
            {d.status === "Plugged" &&
                <div className="btn-group pull-right" role="group" style={{ marginRight: "5px" }}>
                    <button type="button" className={d.disabled ? "btn btn-sm btn-danger" : "btn btn-sm btn-default"} onClick={(e) => props.disable(d.urn, d.disabled, e)}>{d.disabled ? "Disabled" : "Disable"}</button>
                    <button type="button" className={d.disabled ? "btn btn-sm btn-default" : "btn btn-sm btn-success"} onClick={(e) => props.enable(d.urn)}>{d.disabled ? "Enable" : "Enabled"}</button>
                </div>
            }
        </td>
    </tr>
    );
    return <tbody>{listItems}</tbody>;
}