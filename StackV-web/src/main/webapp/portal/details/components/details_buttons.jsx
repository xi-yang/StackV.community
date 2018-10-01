import React from "react";
import PropTypes from "prop-types";

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

class ButtonPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};

        this.sendRequest = this.sendRequest.bind(this);
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
            <OpButton operation="Cancel" uuid={this.props.uuid} visible={this.state.cancel} send={this.sendRequest} />
            <OpButton operation="Force Cancel" uuid={this.props.uuid} visible={this.state.force_cancel} send={this.sendRequest} />
            <OpButton operation="Reinstate" uuid={this.props.uuid} visible={this.state.reinstate} send={this.sendRequest} />
            <OpButton operation="Modify" uuid={this.props.uuid} visible={this.state.modify} send={this.sendRequest} />
            <OpButton operation="Verify" uuid={this.props.uuid} visible={this.state.verify} send={this.sendRequest} />
            <OpButton operation="Unverify" label="Cancel Verification" uuid={this.props.uuid} visible={this.state.unverify} send={this.sendRequest} />
            <OpButton operation="Force Retry" uuid={this.props.uuid} visible={this.state.force_retry} send={this.sendRequest} />
            <OpButton operation="Propagate" uuid={this.props.uuid} visible={this.state.propagate} send={this.sendRequest} />
            <OpButton operation="Commit" uuid={this.props.uuid} visible={this.state.commit} send={this.sendRequest} />
            <OpButton operation="Delete" uuid={this.props.uuid} visible={this.state.delete} send={this.sendRequest} />
        </div>;
    }

    sendRequest(command) {
        let panel = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + this.props.uuid + "/" + command;
        $.ajax({
            url: apiUrl,
            type: "PUT",
            beforeSend: function beforeSend(xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
            },
            success: function () {
                switch (command) {
                    case "delete":
                    case "force_delete":
                        if (panel.props.page === "details") {
                            setTimeout(function () {
                                sessionStorage.removeItem("instance-uuid");
                                window.document.location = "/StackV-web/portal/";
                            }, 100);
                        } else {
                            if (panel.props.resume) { panel.props.resume(); }
                        }
                        break;
                    case "verify":
                        if (panel.props.page === "details") {
                            panel.props.load(5);
                        } else {
                            if (panel.props.resume) { panel.props.resume(); }
                        }
                        break;
                    default:
                        panel.props.load(.5);
                        setTimeout(function () {
                            panel.props.reload();
                        }, 300);
                }
            },
            error: function () {
                setTimeout(function () {
                    panel.props.reload();
                }, 100);
            }
        });
    }
}
ButtonPanel.propTypes = {
    uuid: PropTypes.string.isRequired,
    super: PropTypes.string.isRequired,
    sub: PropTypes.string.isRequired,
    last: PropTypes.string.isRequired,
    keycloak: PropTypes.object.isRequired,
    page: PropTypes.string.isRequired,
    isVerifying: PropTypes.bool,
    resume: PropTypes.func,
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
        let button = this;
        $(".button-confirm-op").click(function () {
            button.props.send($(this).data("mode"));
            $("#modal-button-confirm").iziModal("close");
        });
        this.execute = this.execute.bind(this);
    }

    execute() {
        let command = this.props.operation.toLowerCase().replace(" ", "_");
        if (!this.state.confirmation) {
            // No confirmation required            
            this.props.send(command);
        } else {
            $("#modal-button-" + command).iziModal("open");
        }
    }

    render() {
        let command = this.props.operation.toLowerCase().replace(" ", "_");
        if (this.state.confirmation) {
            let button = this;
            var $confirmModal = $("#modal-button-" + command).iziModal(confirmConfig);
            $confirmModal.iziModal("setContent", "<button class=\"button-confirm button-confirm-" + command + "-close btn btn-primary\" data-izimodal-close=\"\">Close</button><button class=\"button-confirm button-confirm-" + command + "-op btn btn-danger\">Confirm</button>");
            $(".button-confirm-" + command + "-close").click(function () {
                $("#modal-button-" + command).iziModal("close");
            });
            $(".button-confirm-" + command + "-op").click(function () {
                button.props.send(command);
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
    send: PropTypes.func.isRequired,
    label: PropTypes.string,
    visible: PropTypes.bool
};
export default ButtonPanel;
