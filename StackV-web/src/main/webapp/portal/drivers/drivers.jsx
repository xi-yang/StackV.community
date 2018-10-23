import React from "react";
import PropTypes from "prop-types";
import iziToast from "izitoast";
import ReactInterval from "react-interval";
import { Set } from "immutable";

import DriverModal from "./components/editor";
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
    }
    componentDidMount() {
        this.loadData();
        this.props.resumeRefresh();
    }
    shouldComponentUpdate() {
        return true;
    }

    loadData() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        let mapped = [];
        $.ajax({
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                let data = [];
                //Frontend
                for (let driver of result) {
                    let ret = {};
                    ret.urn = driver[0];
                    ret.type = driver[1];
                    ret.xml = driver[2];
                    ret.status = driver[3];

                    mapped.push(ret.urn);
                    data.push(ret);
                }

                page.setState({ data: data });
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
                    $row.addClass("loading");
                    page.props.resumeRefresh();
                },
                error: function (err) {
                    $row.addClass("loading");
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

            e.target.className = "btn btn-danger";
            $(e.target).text("Confirm Unplug");
        } else {
            // Unplug driver
            $row.addClass("loading");
            let apiURL = window.location.origin + "/StackV-web/restapi/driver/" + urn;
            $.ajax({
                url: apiURL,
                type: "DELETE",
                datatype: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                success: function (result) {
                    $row.addClass("loading");
                },
                error: function (err) {
                    $row.addClass("loading");
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
                        <DriverElements data={Set(this.state.data)} open={this.openDriver} plug={this.plugDriver} unplug={this.unplugDriver} confirm={this.confirm} />
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
            <DriverModal {...this.state.openDriver} reset={this.resetDriverModal} />
        </div>;
    }
}
Drivers.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Drivers;

function DriverElements(props) {
    const listItems = props.data.map((d) => <tr key={d.urn} id={d.urn} className={d.xml !== "<driverInstance><properties></properties></driverInstance>" ? (d.status === "Plugged" ? "success" : undefined) : "backend"} onClick={(e) => props.open(d.urn, e)}>
        <td>{d.urn}</td>
        <td>{d.type}</td>
        <td>
            {d.xml !== "<driverInstance><properties></properties></driverInstance>" ? (
                <div className="btn-group" role="group">
                    <button type="button" className={d.status === "Plugged" ? "btn btn-default" : "btn btn-primary"} onClick={(e) => props.unplug(d.urn, d.status, e)}>{d.status === "Plugged" ? "Unplug" : "Unplugged"}</button>
                    <button type="button" className={d.status === "Plugged" ? "btn btn-primary" : "btn btn-default"} onClick={(e) => props.plug(d.urn, d.status, e)}>{d.status === "Plugged" ? "Plugged" : "Plug"}</button>
                </div>
            ) : (
                <button type="button" className="btn btn-default" onClick={(e) => props.unplug(d.urn, d.status, e)}>Unplug (Manually Installed)</button>
            )}
        </td>
    </tr>
    );
    return <tbody>{listItems}</tbody>;
}