import React from "react";
import PropTypes from "prop-types";
import { css } from "emotion";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

const override = css`
    display: inline-flex;
    position: absolute;
    padding: 5px;
    left: 20px;
    top: 0;
    z-index: 50;
    color: white;
`;
const dots = css`
    background: none;
    border: none;
    box-shadow: none;    
    margin: 5px;
`;
const text = css`
    transition: 1s;
    vertical-align: super;
    margin-right: 5px;
`;

class DetailsDots extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <div className={override}>
            <div>
                <FontAwesomeIcon className={dots} icon={this.props.view === "logging" ? ["fas", "dot-circle"] : ["far", "dot-circle"]} size="lg" />
                <span className={text} style={this.props.view === "logging" ? {} : { fontSize: 0 }}>Logging</span>
            </div>
            <div>
                <FontAwesomeIcon className={dots} icon={this.props.view === "details" ? ["fas", "dot-circle"] : ["far", "dot-circle"]} size="lg" />
                <span className={text} style={this.props.view === "details" ? {} : { fontSize: 0 }}>Details</span>
            </div>
            <div>
                <FontAwesomeIcon className={dots} icon={this.props.view === "visual" ? ["fas", "dot-circle"] : ["far", "dot-circle"]} size="lg" />
                <span className={text} style={this.props.view === "visual" ? {} : { fontSize: 0 }}>Visualization</span>
            </div>
        </div>;
    }
}
DetailsDots.propTypes = {
    view: PropTypes.string.isRequired
};
export default DetailsDots;
