import React from "react";
import PropTypes from "prop-types";
import isEqual from "lodash.isequal";

import { keycloak, page } from "../nexus";
import { resumeRefresh, reloadData } from "../refresh";

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
                init.confirmation = false;
        }

        this.state = init;

        this.execute = this.execute.bind(this);
        this.sendRequest = this.sendRequest.bind(this);
    }

    execute() {
        if (this.state.confirmation === undefined) {
            // No confirmation required
            this.sendRequest();
        } else if (this.state.confirmation === false) {
            let button = this;
            // Confirmation required but not given; manipulate button
            setTimeout(function () {
                button.setState({ confirmation: false });
            }, 5000);
            this.setState({ confirmation: true });
        } else {
            // Confirmation given
            this.sendRequest();
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
                if (command === "delete" || command === "force_delete") {
                    if (page === "details") {
                        setTimeout(function () {
                            sessionStorage.removeItem("instance-uuid");
                            window.document.location = "/StackV-web/portal/";
                        }, 200);
                    }
                } else {
                    setTimeout(function () {
                        resumeRefresh();
                        reloadData();
                    }, 200);
                }
            },
            error: function () {
                setTimeout(function () {
                    resumeRefresh();
                    reloadData();
                }, 200);
            }
        });
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
