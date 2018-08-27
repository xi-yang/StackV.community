import React from "react";
import PropTypes from "prop-types";

class ButtonPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    moderate() {
        let superState = this.props.state.split("-")[0].trim();
        let subState = this.props.state.split("-")[1].trim();

        switch (subState) {
        default:
            break;
        }
    }

    render() {
        return <div style={{ left: "10px" }} className="btn-group" role="group">
            <OpButton operation="Modify" />
            <OpButton operation="Verify" />
            <OpButton operation="Propagate" />
            <OpButton operation="Commit" />
            <OpButton operation="Cancel" />
            <OpButton operation="Reinstate" />
            <OpButton operation="Delete" />
        </div>;
    }
}
ButtonPanel.propTypes = {
    uuid: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired
};

class OpButton extends React.Component {
    constructor(props) {
        super(props);

        let confirm = false;
        switch (props.operation) {
        case "Cancel":
        case "Delete":
            confirm = true;
        }

        this.state = {
            display: false,
            confirmation: confirm
        };
    }

    render() {
        let classes = "btn btn-default";
        if (!this.state.display) {
            classes += " hide";
        }
        return <button className={classes}>
            {this.props.operation}
        </button>;
    }

    execute(command) {

    }
}
OpButton.propTypes = {
    uuid: PropTypes.string.isRequired,
    operation: PropTypes.string.isRequired,
    confirmation: PropTypes.bool.isRequired
};
export default ButtonPanel;
