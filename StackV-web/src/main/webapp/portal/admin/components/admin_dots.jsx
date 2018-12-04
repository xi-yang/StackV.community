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
    background: #333;
    border-radius: 0 0 10px 10px;
    box-shadow: 0 1px 10px 0px black;
`;
const dots = css`
    background: none;
    border: none;
    box-shadow: none;    
    margin: 5px;
`;
const text = css`
    transition: .4s;
    vertical-align: super;
    margin-right: 5px;
`;

class AdminDots extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <div className={override}>
            <div onClick={() => { this.props.setView("api"); }}>
                <FontAwesomeIcon className={dots} icon={this.props.view === "api" ? ["fas", "dot-circle"] : ["far", "dot-circle"]} size="lg" />
                <span className={text} style={this.props.view === "api" ? {} : { fontSize: 0 }}>API Testing</span>
            </div>
            <div onClick={() => { this.props.setView("logging"); }}>
                <FontAwesomeIcon className={dots} icon={this.props.view === "logging" ? ["fas", "dot-circle"] : ["far", "dot-circle"]} size="lg" />
                <span className={text} style={this.props.view === "logging" ? {} : { fontSize: 0 }}>Logging</span>
            </div>
        </div>;
    }
}
AdminDots.propTypes = {
    view: PropTypes.string.isRequired,
    setView: PropTypes.func.isRequired,
};
export default AdminDots;
