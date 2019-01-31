import { Set } from "immutable";
import iziToast from "izitoast";
import PropTypes from "prop-types";
import React from "react";
import ReactInterval from "react-interval";
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
        this.setType = this.setType.bind(this);
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

        $(document).on("click", ".in-use-uuid", (e) => {
            let uuid = e.target.text;
            this.props.switchPage("details", { uuid: uuid });
            iziToast.destroy();
        });
    }
    componentWillUnmount() {
        $(document).off("click", ".in-use-uuid");
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
                $(".driver-modal-body-header").show();
            } else {
                if (urn === null) {
                    this.setState({ openDriver: { type: "" } });
                    $(".driver-modal-body-header").show();
                } else {
                    this.setState({ openDriver: this.state.data.find(x => x.urn === urn) });
                    $(".driver-modal-body-header").hide();
                }
                $("#driver-modal").modal("show");
                $("#driver-modal-body-select").val(null);
            }
        }
    }
    setType(type) {
        this.setState({ openDriver: { type: type } });
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
        e.stopPropagation();
        let page = this;
        let $row = $(e.target).parents("tr");
        if (status === "Plugged" && !$(e.target).hasClass("btn-danger")) {
            // Confirmation button
            e.persist();
            let $cacheTarget = $(e.target);
            let cacheText = $(e.target).text();
            let cacheClass = e.target.className;

            e.target.className = "btn btn-sm btn-danger";
            $(e.target).text("Confirm");

            $(document).on("click", "#main-pane", (e) => {
                setTimeout(() => {
                    $cacheTarget[0].className = cacheClass;
                    $cacheTarget.text(cacheText);
                    $(document).off("click", "#main-pane");
                }, 100);
            });
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
                success: function (result) {
                    page.props.resumeRefresh();
                },
                error: function (err) {
                    let arr = err.responseText.replace(/[\[\]\s]/g, "").split(",");

                    let text = "";
                    for (let uuid of arr) {
                        text += "<div><a class=\"in-use-uuid\">" + uuid + "</a></div>";
                    }

                    iziToast.show({
                        icon: "fas fa-exclamation",
                        layout: 2,
                        message: "This driver is in use by the following service instances: " + text,
                        overlay: true,
                        overlayClose: true,
                        position: "center",
                        theme: "dark",
                        timeout: false,
                        title: "Driver in use"
                    });
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
            },
            success: function (result) {
                page.props.resumeRefresh();
            },
        });
    }
    disableDriver(urn, disabled, e) {
        e.stopPropagation();
        let page = this;
        if (!disabled && !$(e.target).hasClass("btn-danger")) {
            // Confirmation button
            e.persist();
            let $cacheTarget = $(e.target);
            let cacheText = $(e.target).text();
            let cacheClass = e.target.className;

            e.target.className = "btn btn-sm btn-danger";
            $(e.target).text("Confirm");

            $(document).on("click", "#main-pane", (e) => {
                setTimeout(() => {
                    $cacheTarget[0].className = cacheClass;
                    $cacheTarget.text(cacheText);
                    $(document).off("click", "#main-pane");
                }, 100);
            });
        } else {
            // Disable driver
            $.ajax({
                url: window.location.origin + "/StackV-web/restapi/driver/" + urn + "/disabled/true",
                type: "PUT",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                success: function () {
                    page.props.resumeRefresh();
                },
            });
            $.ajax({
                url: window.location.origin + "/StackV-web/restapi/ready/reset",
                type: "PUT",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                success: function () {
                    page.props.resumeRefresh();
                },
            });
        }
    }

    resetDriverModal() {
        this.setState({ openDriver: undefined });
    }

    render() {
        let pageClasses = "stack-panel page page-drivers";
        if (this.state.loading) {
            pageClasses = "stack-panel page page-drivers loading";
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
            <DriverModal {...this.state.openDriver} reset={this.resetDriverModal} setType={this.setType} {...this.props} />
        </div>;
    }
}
Drivers.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Drivers;

function DriverElements(props) {


    const listItems = props.data.map((d) => <tr key={d.urn} id={d.urn} className={d.status === "Plugged" ? (d.errors > 5 ? "danger" : (d.errors == null || d.errors === undefined ? "warning" : (d.disabled || d.errors > 0 ? "info" : "success"))) : undefined} onClick={(e) => props.open(d.urn, e)}>
        <td>
            <i className={d.status === "Plugged" ? (d.errors === 0 ? "fas fa-lg driver-health-icon fa-check-circle pass" : (d.errors < 5 || d.errors == null || d.errors === undefined ? "fas fa-lg driver-health-icon fa-exclamation-triangle warn" : "fas fa-lg driver-health-icon fa-exclamation-circle fail")) : undefined}></i>
            {d.urn}
        </td>
        <td>{d.type}</td>
        <td>
            {d.xml !== "<driverInstance><properties></properties></driverInstance>" ? (
                <div className="btn-group pull-right" role="group" style={{ marginLeft: "5px", marginBottom: "5px" }}>
                    <button type="button" className={d.status === "Plugged" ? "btn btn-sm btn-default" : "btn btn-sm btn-primary"} onClick={(e) => props.unplug(d.urn, d.status, e)} disabled={d.status !== "Plugged"}>{d.status === "Plugged" ? "Unplug" : "Unplugged"}</button>
                    <button type="button" className={d.status === "Plugged" ? "btn btn-sm btn-primary" : "btn btn-sm btn-default"} onClick={(e) => props.plug(d.urn, d.status, e)} disabled={d.status === "Plugged"}>{d.status === "Plugged" ? "Plugged" : "Plug"}</button>
                </div>
            ) : (<button type="button" className="btn btn-sm btn-default" onClick={(e) => props.unplug(d.urn, d.status, e)}>Unplug (Manually Installed)</button>)}
            {d.status === "Plugged" &&
                <div className="btn-group pull-right" role="group">
                    <button type="button" className={d.disabled ? "btn btn-sm btn-danger" : "btn btn-sm btn-default"} onClick={(e) => props.disable(d.urn, d.disabled, e)} disabled={d.disabled}>{d.disabled ? "Disabled" : "Disable"}</button>
                    <button type="button" className={d.disabled ? "btn btn-sm btn-default" : "btn btn-sm btn-success"} onClick={(e) => props.enable(d.urn)} disabled={!d.disabled}>{d.disabled ? "Enable" : "Enabled"}</button>
                </div>
            }
        </td>
    </tr>
    );
    return <tbody>{listItems}</tbody>;
}