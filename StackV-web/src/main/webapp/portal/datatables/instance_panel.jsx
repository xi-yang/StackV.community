import React from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";
import ReactInterval from "react-interval";

import "./logging.css";
import ButtonPanel from "../details/components/details_buttons";

class InstancePanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            apiUrl: window.location.origin + "/StackV-web/restapi/app/logging/instances"
        };

        this.initTable = this.initTable.bind(this);
        this.loadData = this.loadData.bind(this);
    }
    componentDidMount() {
        this.initTable();
    }
    componentWillUnmount() {
        //ReactDOM.unmountComponentAtNode(document.getElementById("button-panel"));
    }
    loadData() {
        if (this.props.refreshEnabled) {
            this.state.dataTable.ajax.reload(null, false);
        }
    }

    render() {
        return <div>
            <ReactInterval timeout={this.props.refreshTimer < 1500 ? 1500 : this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadData} />
            <table id="loggingData" className="table table-striped table-bordered display" cellSpacing="0" width="100%">
                <thead>
                    <tr>
                        <th>Alias</th>
                        <th>Type</th>
                        <th>Reference UUID</th>
                        <th>State</th>
                    </tr>
                </thead>
            </table>
        </div>;
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
            "ordering": false,
            "pageLength": 6,
            "scrollX": true,
            "scrollY": "375px"
        });

        $("#loggingData tbody").on("click", "tr.instance-row", function () {
            panel.props.pauseRefresh();
            //sessionStorage.setItem("instance-uuid", this.children[2].innerHTML);
            //window.document.location = "/StackV-web/portal/details/";
            var row = dataTable.row($(this));
            if (row.child.isShown()) {
                // This row is already open - close it
                ReactDOM.unmountComponentAtNode(document.getElementById("button-panel"));
                row.child.hide();
                $(this).removeClass("shown");
                panel.props.resumeRefresh();
            } else {
                if ($("tr.shown").length > 0) {
                    // Other details open, close first
                    ReactDOM.unmountComponentAtNode(document.getElementById("button-panel"));
                    let open = $("tr.shown")[0];
                    dataTable.row($(open)).child.hide();
                    $(open).removeClass("shown");
                }
                $(this).addClass("shown");

                // Open this row
                row.child(formatChild(row.data())).show();

                let superState = row.data().state.split("-")[0].trim();
                let subState = row.data().state.split("-")[1].trim();

                ReactDOM.render(
                    React.createElement(ButtonPanel, {
                        uuid: row.data().referenceUUID, super: superState, sub: subState, last: row.data().lastState, owner: row.data().owner,
                        keycloak: panel.props.keycloak, page: "catalog", resume: panel.props.resumeRefresh, switchPage: panel.props.switchPage,
                    }, null),
                    document.getElementById("button-panel")
                );

                row.child().css("height", "50px");
            }
        });

        $("#loggingData tbody").on("click", ".button-instance-details", function () {
            panel.props.switchPage("details", { uuid: $(this).attr("data-uuid") });
        });

        function formatChild(d) {
            return "<div id=\"button-panel\"></div>";
        }

        panel.setState({ dataTable: dataTable }, () => { panel.setState({ refreshEnabled: true }); });
    }
}
InstancePanel.propTypes = {
    keycloak: PropTypes.object.isRequired,
    switchPage: PropTypes.func.isRequired
};
export default InstancePanel;
