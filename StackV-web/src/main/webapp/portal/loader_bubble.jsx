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

const divStyle = css`
    margin-right: 5px;
    display: inline;
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
            content: "<div><div class=" + divStyle + ">Data Refresh: </div><div class=\"btn-group\" role=\"group\">" +
                "<button type=\"button\" id=\"loader-bubble-enabled-off\" class=\"btn btn-default\">Paused</button>" +
                "<button type=\"button\" id=\"loader-bubble-enabled-on\" class=\"btn btn-default\">Active</button></div></div>" +
                "<div><div class=" + divStyle + ">Refresh Interval (in ms): </div><div class=\"btn-group\" role=\"group\">" +
                "<input style=\"width: 75px;margin-top: 10px;\" type=\"number\" id=\"loader-bubble-timer\" class=\"form-control\"/></div></div>",
            html: true,
            placement: "bottom",
            title: "Reload Settings",
            trigger: "focus"
        });

        $(document).on("click", "#loader-bubble-enabled-off", () => {
            this.props.pauseRefresh();
        });
        $(document).on("click", "#loader-bubble-enabled-on", () => {
            this.props.resumeRefresh();
        });
        $(document).on("change", "#loader-bubble-timer", () => {
            this.props.setRefresh($("#loader-bubble-timer").val());
        });
    }

    getStatus() {
        // Reload is live
        if (this.props.refreshEnabled) {
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
                return <FontAwesomeIcon className={live} id="loader-bubble" icon={faCog} size="lg" onClick={(e) => this.popover(e)} spin />;
            case "paused":
                return <FontAwesomeIcon className={paused} id="loader-bubble" icon={faCog} size="lg" onClick={(e) => this.popover(e)} />;
        }

    }

    moderatePopover() {
        if (this.props.refreshEnabled) {
            $("#loader-bubble-enabled-off").addClass("btn-default");
            $("#loader-bubble-enabled-off").removeClass("btn-primary");
            $("#loader-bubble-enabled-on").removeClass("btn-default");
            $("#loader-bubble-enabled-on").addClass("btn-primary");
        } else {
            $("#loader-bubble-enabled-on").addClass("btn-default");
            $("#loader-bubble-enabled-on").removeClass("btn-primary");
            $("#loader-bubble-enabled-off").removeClass("btn-default");
            $("#loader-bubble-enabled-off").addClass("btn-primary");
        }

        $("#loader-bubble-timer").val(this.props.refreshTimer);
    }
}
LoaderBubble.propTypes = {
    refreshTimer: PropTypes.number.isRequired,
    refreshEnabled: PropTypes.bool.isRequired,
};
export default LoaderBubble;
