import React from "react";
import PropTypes from "prop-types";

class VisualizationPanel extends React.PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return <div id="visual-panel"></div>;
    }
}
VisualizationPanel.propTypes = {
    verify: PropTypes.object.isRequired,
};
export default VisualizationPanel;
