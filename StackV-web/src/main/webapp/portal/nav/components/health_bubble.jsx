import React from "react";
import PropTypes from "prop-types";
import { cx, css } from "emotion";

import { faPowerOff, faToggleOff, faToggleOn } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

const pass = css`
    color: #91ef5a;    
`;
const fail = css`
    color: #f54e49;    
`;
const liStyle = css`
    border-right: 1px solid #eeeeee;
    box-shadow: 4px 0 4px -2px #777;
    pointer-events: none;
`;
const healthStyle = css`
    margin: 10px 10px 10px 5px;
    float: left;
    color: white;
`;
const textStyle = css`
    bottom: 5px;
    display: inline;
    position: relative;
`;

class HealthBubble extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};
    }
    componentDidMount() {

    }

    render() {
        let style, icon;
        if (this.props.systemHealth) {
            style = pass;
            icon = faToggleOn;
        } else {
            style = fail;
            icon = faToggleOff;
        }

        return <li className={liStyle}><div className={healthStyle}><div className={textStyle}>System Health: </div><FontAwesomeIcon className={style} id="health-bubble" icon={icon} size="2x" /></div></li>;
    }
}
export default HealthBubble;
