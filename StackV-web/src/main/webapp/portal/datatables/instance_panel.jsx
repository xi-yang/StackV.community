import React from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";

import "./logging.css";
import ButtonPanel from "../details/components/details_buttons";

class InstancePanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            refreshInterval: "live",
            apiUrl: window.location.origin + "/StackV-web/restapi/app/logging/instances"
        };

        this.initTable = this.initTable.bind(this);
        this.initRefresh = this.initRefresh.bind(this);
    }
    componentDidMount() {
        this.initTable();
    }
    initRefresh() {
        this.state.dataTable.ajax.reload(null, false);

        let page = this;
        let dataInterval = setInterval(function () {
            if (!(page.state.loading || page.state.refreshInterval === "paused")) {
                page.state.dataTable.ajax.reload(null, false);
            }
        }, (page.state.refreshInterval === "live" ? 1000 : 1000 * page.state.refreshInterval));
        this.setState({ dataIntervalRef: dataInterval });
    }

    render() {
        return <table id="loggingData" className="table table-striped table-bordered display" cellSpacing="0" width="100%">
            <thead>
                <tr>
                    <th>Alias</th>
                    <th>Type</th>
                    <th>Reference UUID</th>
                    <th>State</th>
                </tr>
            </thead>
        </table>;
    }

    initTable() {
        let panel = this;
        let dataTable = $("#loggingData").DataTable({
            "ajax": {
                url: panel.state.apiUrl,
                type: "GET",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                }
            },
            "buttons": ["csv"],
            "columns": [
                { "data": "alias" },
                { "data": "type", width: "110px" },
                { "data": "referenceUUID", "width": "250px" },
                {
                    "data": function (row, type, val, meta) {
                        if (row.state.indexOf("FAILED") > -1) {
                            return row.state + "<br>(after " + row.lastState + ")";
                        } else {
                            return row.state;
                        }

                    }, "width": "150px",
                }
            ],
            "createdRow": function (row, data, dataIndex) {
                $(row).addClass("instance-row");
                $(row).attr("data-verification_state", data.verification);
                $(row).attr("data-last_state", data.lastState);
                $(row).attr("data-owner", data.owner);
            },
            "dom": "Bfrtip",
            "initComplete": function (settings, json) {
                console.log("DataTables has finished its initialization.");
            },
            "ordering": false,
            "pageLength": 6,
            "scrollX": true,
            "scrollY": "375px"
        });

        $("#loggingData tbody").on("click", "tr.instance-row", function () {
            //sessionStorage.setItem("instance-uuid", this.children[2].innerHTML);
            //window.document.location = "/StackV-web/portal/details/";
            var row = dataTable.row($(this));
            if (row.child.isShown()) {
                // This row is already open - close it
                ReactDOM.unmountComponentAtNode(document.getElementById("button-panel"));
                row.child.hide();
                $(this).removeClass("shown");
                panel.setState({ refreshInterval: "live" });
            } else {
                panel.setState({ refreshInterval: "paused" });
                if ($("tr.shown").length > 0) {
                    // Other details open, close first
                    ReactDOM.unmountComponentAtNode(document.getElementById("button-panel"));
                    let open = $("tr.shown")[0];
                    dataTable.row($(open)).child.hide();
                    $(open).removeClass("shown");
                }

                // Open this row
                row.child(formatChild(row.data())).show();

                let superState = row.data().state.split("-")[0].trim();
                let subState = row.data().state.split("-")[1].trim();

                ReactDOM.render(
                    React.createElement(ButtonPanel, {
                        uuid: row.data().referenceUUID, super: superState, sub: subState, last: row.data().lastState,
                        keycloak: panel.props.keycloak, page: "catalog", resume: () => { panel.setState({ refreshInterval: "live" }); }
                    }, null),
                    document.getElementById("button-panel")
                );

                $("#button-panel").append("<button style=\"float: right\" type=\"button\" class=\"btn btn-default button-instance-details\" data-uuid=" + row.data().referenceUUID + ">Full Details</button>");

                row.child().css("height", "50px");
                $(this).addClass("shown");
            }
        });

        $("#loggingData tbody").on("click", ".button-instance-details", function () {
            panel.props.switchPage("details", { uuid: $(this).attr("data-uuid") });
        });

        function formatChild(d) {
            return "<div id=\"button-panel\"></div>";
        }

        panel.setState({ dataTable: dataTable }, panel.initRefresh);
    }
}
InstancePanel.propTypes = {
    keycloak: PropTypes.object.isRequired,
    switchPage: PropTypes.func.isRequired
};
export default InstancePanel;
