import React from "react";
import PropTypes from "prop-types";
import { cx, css } from "emotion";

import { faCog } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

const live = css`
    color: #91ef5a;    
`;
const paused = css`
    color: #e2e210;    
`;

class LoaderBubble extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};

        this.popover = this.popover.bind(this);
        this.getStatus = this.getStatus.bind(this);
        this.moderatePopover = this.moderatePopover.bind(this);
    }
    componentDidMount() {
        $("#loader-bubble").popover({
            content: this.state.content,
            html: true,
            placement: "bottom",
            title: "Reload Settings",
            trigger: "focus"
        });
    }

    getStatus() {
        // Reload is live
        if (!this.props.refreshLoading && this.props.refreshEnabled) {
            return "live";
        }
        // Reload is paused
        else if (!this.props.refreshEnabled) {
            return "paused";
        }
    }

    popover(e) {
        $("#loader-bubble").popover("toggle");
        this.moderatePopover();
    }

    render() {
        let status = this.getStatus();
        this.moderatePopover();
        switch (status) {
            case "live":
                return <FontAwesomeIcon className={live} id="loader-bubble" icon={faCog} size="lg" spin onClick={(e) => this.popover(e)} />;
            case "paused":
                return <FontAwesomeIcon className={paused} id="loader-bubble" icon={faCog} size="lg" />;
        }

    }

    moderatePopover() {
        if (this.props.refreshEnabled) {
            $(".popover-content").html("<div>REFRESH ON</div>");
        } else {
            $(".popover-content").html("<div>REFRESH OFF</div>");
        }
    }
}
LoaderBubble.propTypes = {
    refreshTimer: PropTypes.number.isRequired,
    refreshEnabled: PropTypes.bool.isRequired,
    refreshLoading: PropTypes.bool.isRequired,
};
export default LoaderBubble;
