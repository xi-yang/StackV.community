import React from "react";
import PropTypes from "prop-types";
import convert from "xml-js";

import awsSchema from "../xml/aws.xml";
import genericSchema from "../xml/generic.xml";

var driverConfig = {
    title: "Driver Wizard",
    icon: "fas fa-home",
    width: 750,
};

class DriverModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            opened: false,
            advanced: false,
        };

        this.parseInputs = this.parseInputs.bind(this);
        this.changeType = this.changeType.bind(this);
    }
    shouldComponentUpdate(nextProps, nextState) {
        return (this.props.xml !== nextProps.xml)
            || (this.state.type !== nextState.type)
            || (this.state.advanced !== nextState.advanced);
    }

    render() {
        let modalContent;
        if (this.state.advanced) {
            modalContent = (<textarea></textarea>);
        } else {
            modalContent = this.parseInputs();
        }

        return <div className="modal fade" id="driver-modal">
            <div className="modal-dialog" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4 className="modal-title" id="driver-modal-label">Driver Editor</h4>
                    </div>
                    <div className="modal-body">
                        <p className="driver-modal-body-header">Select a service type:
                            <select id="driver-modal-body-select" onChange={(e) => this.changeType(e)}>
                                <option></option>
                                <option value="java:module/AwsDriver">AWS</option>
                                <option value="java:module/GenericRESTDriver">Generic</option>
                            </select>
                        </p>
                        <hr />
                        <div className="driver-modal-body-content">
                            {modalContent}
                        </div>
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
                        <button type="button" className="btn btn-primary">Save changes</button>
                    </div>
                </div>
            </div>
        </div>;
    }

    parseInputs() {
        let entries = [];
        let type, profile;
        if (this.props.type) {
            // Opened profile
            type = this.props.type;
            $("#driver-modal-body-select").val(type);
            profile = JSON.parse(convert.xml2json(this.props.xml, { compact: true, spaces: 4 }));
        } else {
            // New profile
            type = this.state.type;
            $("#driver-modal-body-select").val(null);
        }
        switch (type) {
            case "java:module/AwsDriver":
                entries = awsSchema.driverInstance.properties[0].entry;
                break;
            case "java:module/GenericRESTDriver":
                entries = genericSchema.driverInstance.properties[0].entry;
                break;
        }


        return entries.map((entry) => {
            let formatted;
            if (entry.key[0].indexOf("_") > -1) {
                formatted = entry.key[0].replace(/_(\w)/g, function (v) { return v[1].toUpperCase(); }).replace(/([A-Z])/g, " $1");
            } else {
                formatted = entry.key[0].replace(/([A-Z])/g, " $1");
            }
            formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);

            return (<label key={entry.key[0]}>{formatted}<input className="form-control" name={entry.key[0]} /></label>);
        });
    }

    changeType(e) {
        this.setState({ type: e.target.value });
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