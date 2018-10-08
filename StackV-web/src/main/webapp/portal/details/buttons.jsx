import React from "react";
import PropTypes from "prop-types";

import { keycloak, page } from "../nexus";
import { resumeRefresh, reloadData, startLoading, stopLoading } from "../refresh";

var confirmConfig = {
    title: "Confirm Operation",
    subtitle: "Please confirm operation.",
    headerColor: "#BD5B5B",
    top: 300,
    timeout: 5000,
    timeoutProgressbar: true,
    transitionIn: "fadeInDown",
    transitionOut: "fadeOutDown",
    pauseOnHover: true
};

class ButtonPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};

        this.moderateState = this.moderateState.bind(this);
    }

    componentDidMount() {
        this.moderateState();
    }
    componentDidUpdate(prevProps) {
        if (!(this.props.super === prevProps.super && this.props.sub === prevProps.sub &&
            this.props.last === prevProps.last && this.props.isVerifying === prevProps.isVerifying)) {
            this.moderateState();
        }
    }
    componentWillUnmount() {
        $(".button-confirm-op").off("click");
    }

    moderateState() {
        let modList;
        // Substate moderation
        switch (this.props.sub) {
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
        switch (this.props.super) {
            case "CANCEL":
                if (modList.indexOf("cancel") > -1) {
                    modList.splice(modList.indexOf("cancel"), 1);
                    modList.push("reinstate");
                }
                break;
        }

        // Verification moderation
        if (this.props.isVerifying) {
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

    render() {
        return <div style={{ left: "10px" }} className="btn-group" role="group">
            <OpButton operation="Cancel" uuid={this.props.uuid} visible={this.state.cancel} />
            <OpButton operation="Force Cancel" uuid={this.props.uuid} visible={this.state.force_cancel} />
            <OpButton operation="Reinstate" uuid={this.props.uuid} visible={this.state.reinstate} />
            <OpButton operation="Modify" uuid={this.props.uuid} visible={this.state.modify} />
            <OpButton operation="Verify" uuid={this.props.uuid} visible={this.state.verify} />
            <OpButton operation="Unverify" label="Cancel Verification" uuid={this.props.uuid} visible={this.state.unverify} />
            <OpButton operation="Force Retry" uuid={this.props.uuid} visible={this.state.force_retry} />
            <OpButton operation="Propagate" uuid={this.props.uuid} visible={this.state.propagate} />
            <OpButton operation="Commit" uuid={this.props.uuid} visible={this.state.commit} />
            <OpButton operation="Delete" uuid={this.props.uuid} visible={this.state.delete} />
        </div>;
    }
}
ButtonPanel.propTypes = {
    uuid: PropTypes.string.isRequired,
    super: PropTypes.string.isRequired,
    sub: PropTypes.string.isRequired,
    last: PropTypes.string.isRequired,
    isVerifying: PropTypes.bool
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
        this.sendRequest = this.sendRequest.bind(this);

        let sendRequest = this.sendRequest;
        $(".button-confirm-op").click(function () {
            sendRequest($(this).data("uuid"), $(this).data("mode"));
            $("#modal-button-confirm").iziModal("close");
        });
        this.execute = this.execute.bind(this);
    }

    execute() {
        let command = this.props.operation.toLowerCase().replace(" ", "_");
        if (!this.state.confirmation) {
            // No confirmation required            
            this.sendRequest(this.props.uuid, command);
        } else {
            if (command === "cancel") {
                $("#modal-button-" + command).iziModal("setSubtitle", "Please confirm operation. Cancel will destruct service and deallocate resources.");
            } else if (command === "delete") {
                $("#modal-button-" + command).iziModal("setSubtitle", "Please confirm operation. For deconstruction of service, use Cancel instead of Delete.");
            }

            $("#modal-button-" + command).iziModal("open");
        }
    }

    sendRequest() {
        let command = this.props.operation.toLowerCase().replace(" ", "_");
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
        let command = this.props.operation.toLowerCase().replace(" ", "_");
        if (this.state.confirmation) {
            let butt = this;
            var $confirmModal = $("#modal-button-" + command).iziModal(confirmConfig);
            $confirmModal.iziModal("setContent", "<button class=\"button-confirm button-confirm-" + command + "-close btn btn-primary\" data-izimodal-close=\"\">Close</button><button class=\"button-confirm button-confirm-" + command + "-op btn btn-danger\">Confirm</button>");
            $(".button-confirm-" + command + "-close").click(function () {
                $("#modal-button-" + command).iziModal("close");
            });
            $(".button-confirm-" + command + "-op").click(function () {
                butt.sendRequest();
                $("#modal-button-" + command).iziModal("close");
            });
        }

        let classes;
        if (this.state.confirmation === undefined || this.state.confirmation === false) {
            classes = "btn btn-default";
        } else {
            classes = "btn btn-danger";
        }

        if (!this.props.visible) {
            return null;
        } else if (this.props.label) {
            return <button className={classes} id={"op-" + command} onClick={this.execute}>
                {this.props.label}
            </button>;
        } else {
            return <button className={classes} id={"op-" + command} onClick={this.execute}>
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
