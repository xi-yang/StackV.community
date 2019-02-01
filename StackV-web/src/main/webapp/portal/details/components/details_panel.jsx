import { is } from "immutable";
import PropTypes from "prop-types";
import React from "react";
import ButtonPanel from "./details_buttons";
import InstructionPanel from "./details_instructions";


class DetailsPanel extends React.Component {
    constructor(props) {
        super(props);
    }
    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.active === false && nextProps.active === false) { return false; }

        return !(this.props.uuid === nextProps.uuid && this.props.active === nextProps.active
            && is(this.props.meta, nextProps.meta)
            && is(this.props.state, nextProps.state)
            && is(this.props.verify, nextProps.verify));
    }

    viewIntent() {
        $("#details-intent-modal").iziModal("open");
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="details-panel">
            <table id="instance-details-table" className="management-table">
                <thead>
                    <tr>
                        <th colSpan="2">Instance Details<button className="btn btn-default" id="button-view-intent" onClick={this.viewIntent} style={{ float: "right" }}>View Service Definition</button></th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Instance Alias</td>
                        <td id="instance-alias">{this.props.meta.get("alias")}</td>
                    </tr>
                    <tr>
                        <td>Reference UUID</td>
                        <td id="instance-uuid">{this.props.uuid}</td>
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
                            <p style={{ display: "inline" }} id="instance-substate">{this.props.state.get("sub")}</p>{this.props.state.get("sub") === "FAILED" && <small id="instance-laststate"> (after {this.props.state.get("last")})</small>}
                        </td>
                    </tr>
                    <tr className="instruction-row">
                        <td colSpan="2">
                            <InstructionPanel uuid={this.props.uuid} super={this.props.state.get("super")} sub={this.props.state.get("sub")} verificationResult={this.props.verify.get("result")}
                                verificationHasDrone={this.props.verify.get("drone")} verificationElapsed={this.props.verify.get("elapsed")}></InstructionPanel>
                        </td>
                    </tr>
                    <tr className="button-row">
                        <td colSpan="2">
                            <ButtonPanel {...this.props} super={this.props.state.get("super")} sub={this.props.state.get("sub")} last={this.props.state.get("last")} owner={this.props.meta.get("owner")}
                                isVerifying={this.props.verify.get("drone")} page="details" />
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>;
    }
}
DetailsPanel.propTypes = {
    active: PropTypes.bool.isRequired,
    uuid: PropTypes.string.isRequired,
    meta: PropTypes.object.isRequired,
    state: PropTypes.object.isRequired,
    verify: PropTypes.object.isRequired,
    keycloak: PropTypes.object.isRequired,
};
export default DetailsPanel;
