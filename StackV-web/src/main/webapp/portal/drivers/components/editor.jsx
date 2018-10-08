import React from "react";
import PropTypes from "prop-types";
import convert from "xml-js";

import iziModal from "izimodal";
$.fn.iziModal = iziModal;

import awsSchema from "../xml/aws.xml";

var driverConfig = {
    title: "Driver Wizard",
    icon: "fas fa-home",
    width: 750,
};

class DriverModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            advanced: false,
        };

        this.parseInputs = this.parseInputs.bind(this);
    }
    componentDidMount() {
        $("#driver-modal").iziModal(driverConfig);
    }
    componentWillUnmount() {
        $("#driver-modal").iziModal("destroy");
    }
    /*componentDidUpdate(prevProps) {
        if (prevProps.opened !== this.props.opened) {
            if (this.props.opened) {
                $("#driver-modal").iziModal("open");
            } else {
                $("#driver-modal").iziModal("close");
            }
        }
    }*/

    render() {
        let modalContent;
        if (this.state.advanced) {
            modalContent = (<textarea></textarea>);
        } else {
            modalContent = this.parseInputs();
        }

        return <div id="driver-modal">
            <div className="driver-modal-body">
                <p className="driver-modal-body-header">Select a service type:
                    <select id="driver-modal-body-select">
                        <option></option>
                        <option value="java:module/AwsDriver">AWS</option>
                        <option value="java:module/GenericRESTDriver">Generic</option>
                    </select>
                </p>
                <hr />
                <div className="driver-modal-body-content">
                    {modalContent}
                </div>
                <hr />
                <button className="button-driver-modal-submit btn btn-primary" >Submit</button>
            </div>
        </div>;
    }

    parseInputs() {
        let entries;
        entries = awsSchema.driverInstance.properties[0].entry;
        //let schema = JSON.parse(convert.xml2json(this.props.xml, { compact: true, spaces: 4 }));
        return entries.map((entry) => {
            let formatted = "Hey";
            return (<label key={entry.key[0]}>{formatted}<input className="form-control" name={entry.key[0]} /></label>);
        });
    }
}
DriverModal.propTypes = {
    opened: PropTypes.bool,
    id: PropTypes.string,
    status: PropTypes.string,
    type: PropTypes.string,
    urn: PropTypes.string,
    xml: PropTypes.string,
};
export default DriverModal;