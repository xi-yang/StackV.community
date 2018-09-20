import React from "react";
import PropTypes from "prop-types";

class LoggingPanel extends React.PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return <div id="logging-panel">
            <div id="logging-header-div">
                Logs - Current time:
                <p id="log-time"></p>
                <div style={{ float: "right" }}>
                    <label htmlFor="logging-filter-level" style={{ fontWeight: "normal", marginLeft: "15px" }}>Logging Level</label>
                    <select id="logging-filter-level">
                        <option value="TRACE">TRACE</option>
                        <option defaultValue="INFO">INFO</option>
                        <option value="WARN">WARN</option>
                        <option value="ERROR">ERROR</option>
                    </select>
                </div>
            </div>
            <div id="logging-body-div">
                <table id="loggingData" className="table table-striped table-bordered display" cellSpacing="0" width="100%">
                    <thead>
                        <tr>
                            <th></th>
                            <th>Timestamp</th>
                            <th>Level</th>
                            <th>Event</th>
                            <th>Reference UUID</th>
                        </tr>
                    </thead>
                    <tfoot>
                        <tr>
                            <th></th>
                            <th>Timestamp</th>
                            <th>Level</th>
                            <th>Event</th>
                            <th>Reference UUID</th>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>;
    }
}
LoggingPanel.propTypes = {
    uuid: PropTypes.string
};
export default LoggingPanel;
