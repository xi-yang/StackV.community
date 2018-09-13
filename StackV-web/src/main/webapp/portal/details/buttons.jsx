import React from "react";
import PropTypes from "prop-types";
import isEqual from "lodash.isequal";

import { keycloak, page } from "../nexus";
import { resumeRefresh, reloadData, startLoading, stopLoading } from "../refresh";

var confirmConfig = {
    title: "Confirm Operation",
    subtitle: "Please confirm operation. For deconstruction of service, use Cancel instead of Delete.",
    headerColor: "#BD5B5B",
    top: 300,
    timeout: 5000,
    timeoutProgressbar: true,
    transitionIn: "fadeInDown",
    transitionOut: "fadeOutDown",
    pauseOnHover: true
};
var $confirmModal = $("#modal-button-confirm").iziModal(confirmConfig);
$confirmModal.iziModal("setContent", "<button class=\"button-confirm-close btn btn-primary\" data-izimodal-close=\"\">Close</button><button class=\"button-confirm-op btn btn-danger\">Confirm</button>");
$(".button-confirm-close").click(function () {
    $("#modal-button-confirm").iziModal("close");
});

class ButtonPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};
        this.moderateState = this.moderateState.bind(this);
        this.sendRequest = this.sendRequest.bind(this);

        let sendRequest = this.sendRequest;
        $(".button-confirm-op").click(function () {
            sendRequest($(this).data("mode"));
            $("#modal-button-confirm").iziModal("close");
        });
    }

    componentDidMount() {
        this.moderateState();
    }
    componentDidUpdate(prevProps) {
        if (!isEqual(this.props, prevProps)) {
            this.moderateState();
        }
    }

    moderateState() {
        let superState, subState;
        if (this.props.state === undefined) {
            superState = "";
            subState = "";
        } else {
            superState = this.props.state.split("-")[0].trim();
            subState = this.props.state.split("-")[1].trim();
        }

        let modList;
        // Substate moderation
        switch (subState) {
            default:
            case "INIT":
            case "COMPILED":
            case "PROPAGATED":
                modList = ["delete"];
                break;
            case "COMMITTING-PARTIAL":
            case "COMMITTING":
                modList = ["delete", "force_cancel"];
                break;
            case "COMMITTED":
                modList = ["delete", "verify", "force_cancel"];
                break;
            case "READY":
                modList = ["delete", "cancel"];
                break;
            case "FAILED":
                switch (this.props.last) {
                    case "INIT":
                        modList = ["verify", "delete"];
                        break;
                    case "COMPILED":
                    case "PROPAGATED":
                        modList = ["verify", "delete", "force_retry"];
                        break;
                    case "COMMITTING-PARTIAL":
                    case "COMMITTING":
                        modList = ["verify", "delete", "force_retry", "force_cancel"];
                        break;
                    case "COMMITTED":
                    case "READY":
                        modList = ["delete", "verify", "force_cancel"];
                        break;
                }
                break;
        }

        // Superstate moderation
        switch (superState) {
            case "CANCEL":
                if (modList.indexOf("cancel") > -1) {
                    modList.splice(modList.indexOf("cancel"), 1);
                    modList.push("reinstate");
                }
                break;
        }

        // Verification moderation
        if (this.props.verify) {
            if (modList.indexOf("verify") > -1) {
                modList.splice(modList.indexOf("verify"), 1);
                modList.push("unverify");
            }
        }

        // Enable selected buttons
        let newOps = {
            cancel: false,
            force_cancel: false,
            reinstate: false,
            modify: false,
            verify: false,
            unverify: false,
            force_retry: false,
            propagate: false,
            commit: false,
            delete: false
        };
        for (let op of modList) {
            newOps[op] = true;
        }
        this.setState(newOps);
    }

    sendRequest(command) {
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + this.props.uuid + "/" + command;
        $.ajax({
            url: apiUrl,
            type: "PUT",
            beforeSend: function beforeSend(xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                xhr.setRequestHeader("Refresh", keycloak.refreshToken);
            },
            success: function () {
                switch (command) {
                    case "delete":
                    case "force_delete":
                        if (page === "details") {
                            setTimeout(function () {
                                sessionStorage.removeItem("instance-uuid");
                                window.document.location = "/StackV-web/portal/";
                            }, 100);
                        } else {
                            resumeRefresh();
                            reloadData();
                        }
                        break;
                    case "verify":
                        startLoading();
                        setTimeout(function () {
                            reloadData();
                            stopLoading();
                        }, 2000);
                        break;
                    default:
                        setTimeout(function () {
                            reloadData();
                        }, 100);
                }
            },
            error: function () {
                setTimeout(function () {
                    reloadData();
                }, 100);
            }
        });
    }

    render() {
        return <div style={{ left: "10px" }} className="btn-group" role="group">
            <OpButton operation="Cancel" uuid={this.props.uuid} visible={this.state.cancel} sendRequest={this.sendRequest} />
            <OpButton operation="Force Cancel" uuid={this.props.uuid} visible={this.state.force_cancel} sendRequest={this.sendRequest} />
            <OpButton operation="Reinstate" uuid={this.props.uuid} visible={this.state.reinstate} sendRequest={this.sendRequest} />
            <OpButton operation="Modify" uuid={this.props.uuid} visible={this.state.modify} sendRequest={this.sendRequest} />
            <OpButton operation="Verify" uuid={this.props.uuid} visible={this.state.verify} sendRequest={this.sendRequest} />
            <OpButton operation="Unverify" label="Cancel Verification" uuid={this.props.uuid} visible={this.state.unverify} sendRequest={this.sendRequest} />
            <OpButton operation="Force Retry" uuid={this.props.uuid} visible={this.state.force_retry} sendRequest={this.sendRequest} />
            <OpButton operation="Propagate" uuid={this.props.uuid} visible={this.state.propagate} sendRequest={this.sendRequest} />
            <OpButton operation="Commit" uuid={this.props.uuid} visible={this.state.commit} sendRequest={this.sendRequest} />
            <OpButton operation="Delete" uuid={this.props.uuid} visible={this.state.delete} sendRequest={this.sendRequest} />
        </div>;
    }
}
ButtonPanel.propTypes = {
    uuid: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    last: PropTypes.string.isRequired,
    verify: PropTypes.bool
};

class OpButton extends React.Component {
    constructor(props) {
        super(props);

        let init = {};
        switch (props.operation) {
            case "Cancel":
            case "Delete":
                init.confirmation = true;
        }

        this.state = init;

        this.execute = this.execute.bind(this);
    }

    execute() {
        if (!this.state.confirmation) {
            // No confirmation required
            let command = this.props.operation.toLowerCase().replace(" ", "_");
            this.props.sendRequest(command);
        } else {
            $("#modal-button-confirm").iziModal("open");
            $(".button-confirm-op").attr("data-mode", this.props.operation.toLowerCase().replace(" ", "_"));
        }
    }

    render() {
        let classes;
        if (this.state.confirmation === undefined || this.state.confirmation === false) {
            classes = "btn btn-default";
        } else {
            classes = "btn btn-danger";
        }

        if (!this.props.visible) {
            return null;
        } else if (this.props.label) {
            return <button className={classes} id={"op-" + this.props.operation.toLowerCase()} onClick={this.execute}>
                {this.props.label}
            </button>;
        } else {
            return <button className={classes} id={"op-" + this.props.operation.toLowerCase()} onClick={this.execute}>
                {this.props.operation}
            </button>;
        }
    }
}
OpButton.propTypes = {
    operation: PropTypes.string.isRequired,
    label: PropTypes.string,
    uuid: PropTypes.string.isRequired,
    visible: PropTypes.bool
};
export default ButtonPanel;
