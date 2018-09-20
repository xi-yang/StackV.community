import React from "react";
import PropTypes from "prop-types";

import ButtonPanel from "./details_buttons";
import InstructionPanel from "./details_instructions";

class DetailsPanel extends React.PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return <div id="details-panel">
            <table id="instance-details-table" className="management-table">
                <thead>
                    <tr>
                        <th>Instance Details</th>
                        <th>
                            <button className="btn btn-default" id="button-view-intent" style={{ float: "right" }}>View Service Definition</button>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Instance Alias</td>
                        <td id="instance-alias">{this.props.meta.get("alias")}</td>
                    </tr>
                    <tr>
                        <td>Reference UUID</td>
                        <td id="instance-uuid">{this.props.meta.get("uuid")}</td>
                    </tr>
                    <tr>
                        <td>Owner</td>
                        <td id="instance-owner">{this.props.meta.get("owner")}</td>
                    </tr>
                    <tr>
                        <td>Creation Time</td>
                        <td id="instance-creation-time">{this.props.meta.get("creation")}</td>
                    </tr>
                    <tr>
                        <td>Instance State</td>
                        <td id="instance-superstate">{this.props.state.get("super")}</td>
                    </tr>
                    <tr>
                        <td>Operation Status</td>
                        <td>
                            <p style={{ display: "inline" }} id="instance-substate">{this.props.state.get("sub")}</p><small id="instance-laststate"> (after {this.props.state.get("last")})</small>
                        </td>
                    </tr>
                    <tr className="instruction-row">
                        <td colSpan="2">
                            <InstructionPanel uuid={this.props.meta.get("uuid")} super={this.props.state.get("super")} sub={this.props.state.get("sub")} verificationResult={this.props.verify.get("result")}
                                verificationHasDrone={this.props.verify.get("drone")} verificationElapsed={this.props.verify.get("elapsed")}></InstructionPanel>
                        </td>
                    </tr>
                    <tr className="button-row">
                        <td colSpan="2">
                            <ButtonPanel uuid={this.props.meta.get("uuid")} super={this.props.state.get("super")} sub={this.props.state.get("sub")} last={this.props.state.get("last")} isVerifying={this.props.verify.get("drone")}></ButtonPanel>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>;
    }
}
DetailsPanel.propTypes = {
    meta: PropTypes.object.isRequired,
    state: PropTypes.object.isRequired,
    verify: PropTypes.object.isRequired,
};
export default DetailsPanel;
