import React from "react";
import PropTypes from "prop-types";

class InstructionPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};
        this.moderateState = this.moderateState.bind(this);
    }

    componentDidMount() {
        this.moderateState();
    }
    componentDidUpdate(prevProps) {
        if (!(this.props.super === prevProps.super && this.props.sub === prevProps.sub && this.props.verificationResult === prevProps.verificationResult)) {
            this.moderateState();
        }
    }

    moderateState() {
        let instruction;
        // Substate moderation
        switch (this.props.sub) {
            default:
            case "INIT":
            case "COMPILED":
                instruction = "Service is being initialized.";
                break;
            case "PROPAGATED":
                instruction = "Service delta has been sent to backend.";
                break;
            case "COMMITTING":
            case "COMMITTING-PARTIAL":
                instruction = "Service is currently being constructed.";
                break;
            case "COMMITTED":
                switch (this.props.verificationResult) {
                    case "-1":
                        instruction = "Service has been constructed, but could not be verified. Please attempt verification again.";
                        break;
                    case "0":
                        instruction = "Service has been constructed, and is now being verified.";
                        break;
                }
                break;
            case "FAILED":
                instruction = "Service has failed. Please see logging for more information.";
                break;
            case "READY":
                switch (this.props.verificationResult) {
                    case "-1":
                        instruction = "Service was not able to be verified.";
                        break;
                    case "1":
                        instruction = "Service has been successfully verified.";
                        break;
                }
                break;
        }

        // Superstate moderation
        /*switch (this.props.super) {
            case "CANCEL":
                break;
        }*/
        this.setState({ instruction: instruction });
    }

    render() {
        let res = this.state.instruction;
        if (this.props.verificationHasDrone) {
            res += " (Verification elapsed time: " + this.props.verificationElapsed + ")";
        }
        return <div>{res}</div>;
    }
}
InstructionPanel.propTypes = {
    uuid: PropTypes.string.isRequired,
    super: PropTypes.string.isRequired,
    sub: PropTypes.string.isRequired,
    verificationHasDrone: PropTypes.bool.isRequired,
    verificationResult: PropTypes.string,
    verificationElapsed: PropTypes.string
};
export default InstructionPanel;
