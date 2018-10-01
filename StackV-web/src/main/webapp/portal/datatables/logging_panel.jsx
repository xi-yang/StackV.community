import React from "react";
import PropTypes from "prop-types";

import "./logging.css";

import { loadLoggingDataTable, reloadLogs, filterLogs } from "../datatables/logging";

class LoggingPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            level: "INFO"
        };
    }
    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.active === false && nextProps.active === false) { return false; }
        return true;
    }
    componentDidMount() {
        var apiUrl = window.location.origin + "/StackV-web/restapi/app/logging/logs/serverside?refUUID=" + this.props.uuid;
        loadLoggingDataTable(apiUrl);
        reloadLogs();
    }

    newLevel(sel) {
        filterLogs(sel);
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="logging-panel">
            <div id="logging-header-div">
                Logs - Current time:
                <p id="log-time"></p>
                <div style={{ float: "right" }}>
                    <label htmlFor="logging-filter-level" style={{ fontWeight: "normal", marginLeft: "15px" }}>Logging Level</label>
                    <select id="logging-filter-level" value={this.state.level} onChange={this.newLevel}>
                        <option value="TRACE">TRACE</option>
                        <option value="INFO">INFO</option>
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
    active: PropTypes.bool.isRequired,
    uuid: PropTypes.string
};
export default LoggingPanel;
