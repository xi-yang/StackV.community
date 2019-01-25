import iziToast from "izitoast";
import React from "react";
import ReactInterval from "react-interval";

class SettingsPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = { "registered": true };

        this.loadSettings = this.loadSettings.bind(this);
        this.saveSettings = this.saveSettings.bind(this);

        this.checkRegistration = this.checkRegistration.bind(this);
        this.register = this.register.bind(this);
        this.deregister = this.deregister.bind(this);
    }
    componentDidMount() {
        this.loadSettings();
        this.checkRegistration();
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="settings-panel">
            <ReactInterval timeout={this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.checkRegistration} />;
            <div id="settings-header-div">Global Settings</div>
            <div id="settings-body-div">
                <label style={{ width: "100%", margin: "5px 0", textAlign: "left" }}>StackV Orchestrator Name</label>
                <div className="input-group">
                    <input className="form-control" name="system.name" disabled={this.state.registered}></input>
                    <div className="input-group-btn">
                        <button type="button" onClick={() => { this.deregister(); }} className={this.state.registered ? "btn btn-default" : "btn btn-primary"} disabled={!this.state.registered}>{this.state.registered ? "Deregister" : "Deregistered"}</button>
                        <button type="button" onClick={() => { this.register(); }} className={this.state.registered ? "btn btn-primary" : "btn btn-default"} disabled={this.state.registered}>{this.state.registered ? "Registered" : "Register"}</button>
                    </div>
                </div>
                <hr /><h3>Keycloak</h3>
                <label style={{ width: "50%" }}>Server URL<input className="form-control" name="system.keycloak"></input></label>
                <hr /><h3>MD2</h3>
                <label style={{ width: "70%" }}>Server URL<input className="form-control" name="ipa.server"></input></label>
                <label style={{ width: "30%" }}>Username<input className="form-control" name="ipa.username"></input></label>
                <label style={{ width: "30%" }}>Password<input className="form-control" name="ipa.password"></input></label>
            </div>
            <div id="settings-footer-div">
                <hr />
                <button style={{ float: "right" }} className="btn btn-primary" onClick={this.saveSettings}>Save</button>
            </div>
        </div>;
    }

    loadSettings() {
        let page = this;
        $.ajax({
            url: window.location.origin + "/StackV-web/restapi/config",
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (config) {
                $("#settings-body-div input").each((i, ele) => {
                    let $input = $(ele);
                    let val = config[$input.attr("name")];
                    $input.val(val);
                    $input.attr("data-original", val);
                });
            }
        });
    }
    saveSettings() {
        let page = this;
        let fail = false;
        $("#settings-body-div input").each((i, ele) => {
            if ($(ele).val() !== "") {
                $.ajax({
                    url: window.location.origin + "/StackV-web/restapi/config/" + $(ele).attr("name") + "/" + encodeURIComponent($(ele).val()),
                    async: false,
                    type: "PUT",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                        xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                    },
                    success: function (config) {
                        $.ajax({
                            url: window.location.origin + "/StackV-web/restapi/app/reload",
                            async: false,
                            type: "PUT",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                            }
                        });
                        $.ajax({
                            url: window.location.origin + "/StackV-web/restapi/md2/reload",
                            async: false,
                            type: "PUT",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                            }
                        });
                    },
                    error: function () {
                        fail = true;
                        return;
                    }
                });
            }
        });
        if (fail) {
            iziToast.error({
                timeout: 3000,
                title: "Error",
                message: "Some settings unable to be saved. Please try again.",
                position: "topRight",
                displayMode: 2,
                pauseOnHover: false
            });
        } else {
            iziToast.success({
                timeout: 2000,
                title: "Success",
                message: "Settings saved!",
                position: "topRight",
                displayMode: 2,
                pauseOnHover: false
            });
        }
    }

    checkRegistration() {
        let page = this;
        $.ajax({
            url: window.location.origin + "/StackV-web/restapi/md2/register",
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                page.setState({ "registered": (result == "true") });
            }
        });
    }

    register() {
        let page = this;
        $.ajax({
            url: window.location.origin + "/StackV-web/restapi/md2/register",
            async: false,
            type: "POST",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            }, success: function () {
                page.props.frameLoad(1000);
            }
        });
    }
    deregister() {
        let page = this;
        $.ajax({
            url: window.location.origin + "/StackV-web/restapi/md2/register",
            async: false,
            type: "DELETE",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            }, success: function () {
                page.props.frameLoad(1000);
            }
        });
    }
}
export default SettingsPanel;
