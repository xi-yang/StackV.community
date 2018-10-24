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

        this.parseInputFields = this.parseInputFields.bind(this);
        this.changeType = this.changeType.bind(this);
        this.save = this.save.bind(this);
        this.delete = this.delete.bind(this);
    }
    shouldComponentUpdate(nextProps, nextState) {
        return (this.props.xml !== nextProps.xml)
            || (this.state.type !== nextState.type)
            || (this.state.advanced !== nextState.advanced);
    }
    componentDidMount() {
        let modal = this;
        $("#driver-modal").on("hidden.bs.modal", function (e) {
            modal.props.reset();
            $("#button-editor-save").text("Save");
            $("#button-editor-save")[0].className = "btn btn-primary";
        });
    }

    save() {
        let page = this;
        let schema;
        let type;

        // Retrieve schema
        if (this.props.type) {
            type = this.props.type;
        } else {
            type = this.state.type;
        }
        switch (type) {
            case "java:module/AwsDriver":
                schema = JSON.parse(JSON.stringify(awsSchema));
                break;
            case "java:module/GenericRESTDriver":
                schema = JSON.parse(JSON.stringify(genericSchema));
                break;
        }

        // Map input values to schema
        let entries = schema.driverInstance.properties[0].entry;
        for (let input of $(".driver-modal-body-content input")) {
            let obj = entries.find(x => x.key[0] === input.name);
            if (obj) { obj.value[0] = input.value; }
        }

        let xml = convert.json2xml(schema, { compact: true, spaces: 4 });
        console.log(xml);

        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        let data = { urn: entries.find(x => x.key[0] === "topologyUri").value[0], type: type, xml: xml };
        $.ajax({
            url: apiUrl,
            type: "PUT",
            contentType: "application/json",
            data: JSON.stringify(data),
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                $("#driver-modal").modal("hide");
            }
        });
    }
    delete() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        let data = { urn: $("input[name=\"topologyUri\"]").val() };
        $.ajax({
            url: apiUrl,
            type: "DELETE",
            contentType: "application/json",
            data: JSON.stringify(data),
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                $("#driver-modal").modal("hide");
            }
        });
    }

    render() {
        let modalContent;
        if (this.state.advanced) {
            modalContent = (<textarea></textarea>);
        } else {
            modalContent = this.parseInputFields();
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
                        <small style={{ marginRight: "7px", color: "#777" }}>To save as a new driver, change the Topology URN.</small>
                        <div className="btn-group" role="group">
                            <button type="button" className="btn btn-primary" id="button-editor-save" onClick={this.save}>Save</button>
                            <button type="button" className="btn btn-danger" id="button-editor-delete" disabled={this.props.status === "Plugged" ? true : undefined} onClick={this.delete}>Delete</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>;
    }

    parseInputFields() {
        let entries, savedEntries;
        let type;
        if (this.props.type) {
            // Opened profile
            type = this.props.type;
            $("#driver-modal-body-select").val(type);
            savedEntries = JSON.parse(convert.xml2json(this.props.xml, { compact: true, spaces: 4 })).driverInstance.properties.entry;
        } else {
            // New profile
            type = this.state.type;
        }
        switch (type) {
            case "java:module/AwsDriver":
                entries = awsSchema.driverInstance.properties[0].entry;
                break;
            case "java:module/GenericRESTDriver":
                entries = genericSchema.driverInstance.properties[0].entry;
                break;
        }

        if (entries) {
            return entries.map(this.editCallback.bind(null, savedEntries, this.props.status === "Plugged"));
        } else {
            return <div></div>;
        }
    }



    editCallback(saved, plugged, entry) {
        let formatted, key, value, savedValue;
        if (Object.prototype.toString.call(entry.key) === "[object Object]") {
            key = entry.key._text;
            value = entry.value._text;
        } else {
            key = entry.key[0];
            value = entry.value[0];
        }
        if (saved) {
            let save = saved.find(x => x.key._text === key);
            savedValue = save ? save.value._text : undefined;
        }

        if (key.indexOf("_") > -1) {
            formatted = key.replace(/_(\w)/g, function (v) { return v[1].toUpperCase(); }).replace(/([A-Z])/g, " $1");
        } else {
            formatted = key.replace(/([A-Z])/g, " $1");
        }
        formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);

        switch (key) {
            case "topologyUri":
                return (<label key={key}>Topology URN<input className="form-control" name={key} data-original={savedValue} defaultValue={savedValue} onChange={(e) => urnChange(e)} /><br /></label>);
            case "driverEjbPath":
                return (<label key={key}>{formatted}<input className="form-control" name={key} defaultValue={value} readOnly /></label>);
            default:
                return (<label key={key}>{formatted}<input className="form-control" name={key} defaultValue={savedValue} readOnly={plugged ? true : undefined} /></label>);
        }

        function urnChange(e) {
            let changed = e.target.dataset.original !== e.target.value;
            let save = $("#button-editor-save").text() === "Save";

            if (changed) {
                // Save as
                if (save) {
                    $("#button-editor-save").text("Save As");
                    $("#button-editor-save")[0].className = "btn btn-success";
                }
            } else {
                // Original
                if (!save) {
                    $("#button-editor-save").text("Save");
                    $("#button-editor-save")[0].className = "btn btn-primary";
                }
            }
        }
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
    reset: PropTypes.func.isRequired
};
export default DriverModal;