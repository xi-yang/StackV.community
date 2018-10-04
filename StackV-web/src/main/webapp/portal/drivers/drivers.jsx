import React from "react";
import PropTypes from "prop-types";
import iziToast from "izitoast";
import ReactInterval from "react-interval";
import { Set } from "immutable";

import DriverModal from "./components/editor";
import "./drivers.css";

class Drivers extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            refreshTimer: 1000,
            refreshEnabled: true,
            loading: false,
            data: [],
        };

        this.openDriver = this.openDriver.bind(this);
        this.loadData = this.loadData.bind(this);
    }
    componentDidMount() {
        this.loadData();
        this.setState({ refreshEnabled: true });
    }
    loadData() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/driver/";
        $.ajax({
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                //fill installed table
                let data = [];
                for (let i = 0; i < result.length; i += 3) {
                    let ret = {};
                    ret.id = result[i];
                    ret.urn = result[i + 2];
                    ret.type = result[i + 1];
                    ret.xml = "<driverInstance><properties><entry><key>topologyUri</key><value>urn:ogf:network:aws.amazon.com:aws-cloud</value></entry><entry><key>driverEjbPath</key><value>java:module/AwsDriver</value></entry><entry><key>aws_access_key_id</key><value>AKIAJZL23XWIXC6KFHEA</value></entry><entry><key>aws_secret_access_key</key><value>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</value></entry><entry><key>region</key><value>us-east-1</value></entry><entry><key>defaultInstanceType</key><value>m4.large</value></entry><entry><key>defaultImage</key><value>ami-146e2a7c</value></entry><entry><key>defaultKeyPair</key><value>driver_key</value></entry><entry><key>defaultSecGroup</key><value>geni</value></entry></properties></driverInstance>";
                    ret.status = "Plugged";

                    data.push(ret);
                }

                page.setState({ data: data });
            }
        });
    }

    openDriver(urn) {
        this.setState({ openDriver: this.state.data.find(x => x.urn === urn) });
        $("#driver-modal").iziModal("open");
    }

    render() {
        let pageClasses = "stack-panel page page-details";
        if (this.state.loading) {
            pageClasses = "stack-panel page page-details loading";
        }
        return <div className={pageClasses}>
            <ReactInterval timeout={this.state.refreshTimer} enabled={this.state.refreshEnabled} callback={this.loadData} />

            <div className="stack-header">
                <b>System Drivers</b>
            </div>
            <div className="stack-body" style={{ padding: "20px 10px" }}>
                <div id="drivers-table-div">
                    <table className="table table-hover">
                        <thead>
                            <tr>
                                <th>Driver ID</th>
                                <th>Type</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <DriverElements data={Set(this.state.data)} func={this.openDriver} />
                        <tfoot>
                            <tr>
                                <th colSpan="3">
                                    <div id="drivers-table-footer" style={{ cursor: "pointer" }} onClick={() => this.openDriver(null)}><i className="fas fa-plus-circle" style={{ marginRight: "10px" }} />Add New Driver</div>
                                </th>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <DriverModal {...this.state.openDriver} />
        </div>;
    }
}
Drivers.propTypes = {
    keycloak: PropTypes.object.isRequired,
};
export default Drivers;

function DriverElements(props) {
    const listItems = props.data.map((d) => <tr key={d.urn} id={d.urn} className={d.status === "Plugged" ? "success" : undefined} onClick={() => props.func(d.urn)}>
        <td>{d.urn}</td>
        <td>{d.type}</td>
        <td>{d.status}</td>
    </tr>
    );
    return <tbody>{listItems}</tbody>;
}