import React from "react";
import PropTypes from "prop-types";

class VisualizationPanel extends React.Component {
    constructor(props) {
        super(props);
    }
    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.active === false && nextProps.active === false) { return false; }
        return true;
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="visual-panel"></div>;
    }
}
VisualizationPanel.propTypes = {
    active: PropTypes.bool.isRequired,
    verify: PropTypes.object.isRequired,
};
export default VisualizationPanel;
