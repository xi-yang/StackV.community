/* global XDomainRequest, TweenLite, Power2, details_viz */
import React from "react";
import PropTypes from "prop-types";
import { Map } from "immutable";
import Mousetrap from "mousetrap";

import { keycloak, page } from "../nexus";
import DetailsPanel from "./details_panel";
import LoggingPanel from "../logging_panel";
import VisualizationPanel from "./visualization_panel";

var $intentModal = $("#details-intent-modal");
var intentConfig = {
    width: 750
};
var tweenDetailsPanel = new TweenLite("#details-panel", 1, {
    ease: Power2.easeInOut,
    paused: true, top: "0px", opacity: "1", display: "block"
});
var tweenLoggingPanel = new TweenLite("#logging-panel", 1, {
    ease: Power2.easeInOut,
    paused: true, left: "0px", opacity: "1", display: "block"
});
var tweenVisualPanel = new TweenLite("#visual-panel", 1, {
    ease: Power2.easeInOut,
    paused: true, right: "0px", opacity: "1", display: "block"
});

Mousetrap.bind("shift+left", function () { window.location.href = "/StackV-web/portal/"; });
Mousetrap.bind("shift+right", function () { window.location.href = "/StackV-web/portal/driver/"; });

class Details extends React.PureComponent {
    constructor(props) {
        super(props);

        Mousetrap.bind("left", function () { this.viewShift("left"); });
        Mousetrap.bind("right", function () { this.viewShift("right"); });

        this.state = {
            view: "details"
        };
    }

    render() {
        switch (this.state.view) {
            case "logging":
                return <div>
                    <LoggingPanel></LoggingPanel>
                </div>;
            case "details":
                return <div>
                    <DetailsPanel uuid={this.props.uuid}></DetailsPanel>
                </div>;
            case "visual":
                return <div>
                    <VisualizationPanel uuid={this.props.uuid}></VisualizationPanel>
                </div>;
        }
    }

    viewShift(dir) {
        switch (this.state.view) {
            case "logging":
                if (dir === "right") {
                    this.setState({ view: "details" });
                }
                break;
            case "details":
                switch (dir) {
                    case "left":
                        this.setState({ view: "logging" });
                        break;
                    case "right":
                        this.setState({ view: "visual" });
                        break;
                }
                break;
            case "visual":
                if (dir === "left") {
                    this.setState({ view: "details" });
                }
                break;
        }
    }
}
Details.propTypes = {
    uuid: PropTypes.string.isRequired,
};
export default Details;
