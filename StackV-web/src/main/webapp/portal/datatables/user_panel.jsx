import React from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";
import ReactInterval from "react-interval";

import "./logging.css";

class UserPanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            apiUrl: window.location.origin + "/StackV-web/restapi/app/data/users/" + props.uuid
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
        if (($("#access-modal").data("bs.modal") || {}).isShown) {
            this.state.dataTable.ajax.reload(null, false);
        }
    }

    render() {
        return <div>
            <ReactInterval timeout={2000} enabled={true} callback={this.loadData} />
            <table id="userData" className="table table-striped table-bordered display" cellSpacing="0" width="100%">
                <thead>
                    <tr>
                        <th>Username</th>
                    </tr>
                </thead>
            </table>
        </div>;
    }

    initTable() {
        let panel = this;
        let dataTable = $("#userData").DataTable({
            "ajax": {
                url: panel.state.apiUrl,
                type: "GET",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                }
            },
            "buttons": ["csv"],
            "columns": [
                { "data": "username" },
            ],
            "createdRow": function (row, data, dataIndex) {
                $(row).addClass("user-row");
                $(row).attr("data-username", data.username);
                if (data.permitted) {
                    $(row).addClass("permitted");
                }
            },
            "dom": "Bfrtip",
            "initComplete": function (settings, json) {
                console.log("DataTables has finished its initialization.");
            },
            "ordering": false,
            "processing": true,
            "scroller": {
                deferRender: true,
                displayBuffer: 15,
                loadingIndicator: true
            },
            "scrollX": true,
            "scrollY": "calc(60vh - 130px)",
        });

        $("#userData tbody").on("click", "tr.user-row", function () {
            let user = $(this).attr("data-username");
            let apiUrl = window.location.origin + "/StackV-web/restapi/app/access/" + panel.props.uuid + "/" + user;
            let method = "PUT";
            if ($(this).hasClass("permitted")) {
                method = "DELETE";
            }
            $.ajax({
                url: apiUrl,
                type: method,
                beforeSend: function beforeSend(xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
                },
                success: function () {
                    panel.loadData();
                }
            });
        });

        panel.setState({ dataTable: dataTable }, () => { panel.setState({ refreshEnabled: true }); });
    }
}
UserPanel.propTypes = {
};
export default UserPanel;
