import React from "react";
import ReactInterval from "react-interval";

class SettingsPanel extends React.Component {
    constructor(props) {
        super(props);

        this.loadSettings = this.loadSettings.bind(this);
        this.saveSettings = this.saveSettings.bind(this);
    }
    componentDidMount() {
        this.loadSettings();
    }

    render() {
        //<ReactInterval timeout={this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadSettings} />;
        return <div className={this.props.active ? "top" : "bottom"} id="settings-panel">
            <div id="settings-header-div">Global Settings</div>
            <div id="settings-body-div">
                <label style={{ width: "95%" }}>Keycloak Server URL<input className="form-control" name="system.keycloak"></input></label>
                <label style={{ width: "95%" }}>IPA Server URL<input className="form-control" name="ipa.server"></input></label>
                <label style={{ width: "45%" }}>IPA Username<input className="form-control" name="ipa.username"></input></label>
                <label style={{ width: "45%" }}>IPA Password<input className="form-control" name="ipa.password"></input></label>
            </div>
            <div id="settings-footer-div">
                <hr />
                <button style={{ float: "right" }} className="btn btn-primary" onClick={this.saveSettings}>Save</button>
            </div>
        </div>;
    }

    loadSettings() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/config";
        $.ajax({
            url: apiUrl,
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
        let apiUrl = window.location.origin + "/StackV-web/restapi/config/";
        $("#settings-body-div input").each((i, ele) => {
            if ($(ele).val() !== "") {
                $.ajax({
                    url: apiUrl + $(ele).attr("name") + "/" + encodeURIComponent($(ele).val()),
                    async: false,
                    type: "PUT",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                        xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                    },
                    success: function (config) {

                        let apiUrl = window.location.origin + "/StackV-web/restapi/app/reload";
                        $.ajax({
                            url: apiUrl,
                            async: false,
                            type: "PUT",
                            beforeSend: function (xhr) {
                                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                            }
                        });
                    }
                });
            }
        });
    }
}
export default SettingsPanel;
