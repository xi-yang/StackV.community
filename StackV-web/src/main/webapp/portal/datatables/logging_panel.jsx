import React from "react";
import PropTypes from "prop-types";
import ReactInterval from "react-interval";

import "./logging.css";

class LoggingPanel extends React.Component {
    constructor(props) {
        super(props);

        if (sessionStorage.getItem("LoggingPanel_level")) {
            this.state = { level: sessionStorage.getItem("LoggingPanel_level") };
        } else {
            this.state = { level: "INFO" };
        }

        this.loadData = this.loadData.bind(this);

        this.newLevel = this.newLevel.bind(this);
        this.filterLogs = this.filterLogs.bind(this);
        this.initTable = this.initTable.bind(this);
    }
    shouldComponentUpdate(nextProps, nextState) {
        if (this.props.active === false && nextProps.active === false) { return false; }
        return true;
    }
    componentDidMount() {
        this.initTable();
    }
    loadData() {
        if (this.props.active && this.state.dataTable.scroller.page().start === 0) {
            this.state.dataTable.ajax.reload(this.filterLogs(), false);
        }
    }

    newLevel() {
        sessionStorage.setItem("LoggingPanel_level", $("#logging-filter-level").val());
        this.setState({ level: $("#logging-filter-level").val() }, this.props.resumeRefresh());
        this.props.frameLoad(2500);
    }
    filterLogs() {
        let curr = this.state.dataTable.ajax.url();
        let paramArr = curr.split(/[?&]+/);
        let newURL = paramArr[0];

        let refEle;
        for (let x in paramArr) {
            if (paramArr[x].indexOf("refUUID") !== -1) {
                refEle = paramArr[x];
            }
        }

        newURL += "?level=" + this.state.level;
        if (refEle) {
            newURL += "&" + refEle;
        }

        this.state.dataTable.ajax.url(newURL);
    }
    liveClock() {
        $("#logging-clock").text(new Date().toLocaleTimeString());
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="logging-panel">
            <ReactInterval timeout={this.props.refreshTimer < 1500 ? 1500 : this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadData} />
            <ReactInterval timeout={1000} enabled={true} callback={this.liveClock} />
            <div id="logging-header-div">
                Instance Logs
                <div id="logging-clock"></div>
                <div style={{ float: "right" }}>
                    <label htmlFor="logging-filter-level" style={{ fontWeight: "normal", marginLeft: "15px", marginRight: "5px" }}>Logging Level</label>
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

    /* */
    downloadLogs() {
        var ret = [];
        if (this.state.dataTable) {
            var data = this.state.dataTable.rows().data();
            for (var i in data) {
                var log = data[i];

                ret.push(JSON.stringify(log));
            }
        }
        return ret;
    }
    initTable() {
        let apiUrl;
        if (this.props.uuid) {
            apiUrl = window.location.origin + "/StackV-web/restapi/app/logging/logs/serverside?refUUID=" + this.props.uuid + "&level=" + this.state.level;
        } else {
            apiUrl = window.location.origin + "/StackV-web/restapi/app/logging/logs/serverside?level=" + this.state.level;
        }

        let panel = this;
        let dataTable = $("#loggingData").DataTable({
            "ajax": {
                url: apiUrl,
                type: "GET",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                }
            },
            "buttons": ["csv"],
            "columns": [
                {
                    "className": "details-control",
                    "orderable": false,
                    "data": null,
                    "defaultContent": "",
                    "width": "20px"
                },
                { "data": "timestamp", "width": "150px" },
                { "data": "level", "width": "60px" },
                { "data": "event" },
                { "data": "referenceUUID", "width": "275px" },
                { "data": "message", "visible": false, "searchable": false }
            ],
            "createdRow": function (row, data, dataIndex) {
                $(row).addClass("row-" + data.level.toLowerCase());
            },
            "dom": "Bfrtip",
            "infoCallback": function (settings, start, end, max, total, pre) {
                if (start === 1) {
                    return "Showing live update of " + total + " entries";
                } else {
                    return pre;
                }
            },
            "order": [[1, "asc"]],
            "ordering": false,
            "processing": true,
            "scroller": {
                deferRender: true,
                displayBuffer: 15,
                loadingIndicator: true
            },
            "scrollX": true,
            "scrollY": "calc(60vh - 130px)",
            "serverSide": true
        });

        $("#loggingData").on("preXhr.dt", function () {
            // Event for opening loading animation
        });
        $("#loggingData").on("draw.dt", function () {
            // Event for closing loading animation
        });

        // Add event listener for opening and closing details
        $("#loggingData tbody").on("click", "td.details-control", function () {
            let tr = $(this).closest("tr");
            var row = dataTable.row(tr);
            if (row.child.isShown()) {
                // This row is already open - close it
                row.child.hide();
                tr.removeClass("shown");
                if ($("tr.shown").length === 0 && dataTable.scroller.page().start === 0) {
                    panel.props.resumeRefresh();
                }
            } else {
                panel.props.pauseRefresh();
                // Open this row
                row.child(formatChild(row.data())).show();
                tr.addClass("shown");
            }
        });

        $("div.dataTables_scrollBody").scroll(function () {
            if (dataTable.scroller.page().start === 0 && $("tr.shown").length === 0) {
                panel.props.resumeRefresh();
            } else {
                panel.props.pauseRefresh();
            }
        });

        function formatChild(d) {
            // `d` is the original data object for the row
            var retString = "<table cellpadding=\"5\" cellspacing=\"0\" border=\"0\">";
            if (d.message !== "{}") {
                retString += "<tr>" +
                    "<td style=\"width:10%\">Message:</td>" +
                    "<td style=\"white-space: normal\">" + d.message + "</td>" +
                    "</tr>";
            }
            if (d.exception !== "") {
                retString += "<tr>" +
                    "<td>Exception:</td>" +
                    "<td><textarea class=\"dataTables-child\">" + d.exception + "</textarea></td>" +
                    "</tr>";
            }
            if (d.referenceUUID !== "") {
                retString += "<tr>" +
                    "<td>UUID:</td>" +
                    "<td>" + d.referenceUUID + "</td>" +
                    "</tr>";
            }
            if (d.targetID !== "" && d.targetID !== undefined) {
                retString += "<tr>" +
                    "<td>Target:</td>" +
                    "<td>" + d.targetID + "</td>" +
                    "</tr>";
            }
            retString += "<tr>" +
                "<td>Logger:</td>" +
                "<td>" + d.logger + "</td>" +
                "</tr>" +
                "</table>";

            return retString;
        }

        panel.setState({ dataTable: dataTable }, () => { this.filterLogs(); panel.props.resumeRefresh(); });
    }
}
LoggingPanel.propTypes = {
    active: PropTypes.bool.isRequired,
    uuid: PropTypes.string
};
export default LoggingPanel;
