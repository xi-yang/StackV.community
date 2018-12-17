import React from "react";
import PropTypes from "prop-types";
import convert from "xml-js";

import schemaXML from "../data/schemas.xml";
var driverConfig = {
    title: "Driver Wizard",
    icon: "fas fa-home",
    width: 750,
};
var schemas = schemaXML.catalog.driverInstance;

class DriverModal extends React.Component {
    constructor(props) {
        super(props);

        this.parseInputFields = this.parseInputFields.bind(this);
        this.save = this.save.bind(this);
        this.update = this.update.bind(this);
        this.importRawDriver = this.importRawDriver.bind(this);
        this.delete = this.delete.bind(this);
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

        if (this.props.type) {
            let type = this.props.type;
            if (type === "raw") { return this.importRawDriver(); }

            if ($("#driver-modal-raw").size() === 1) {
                let xml = $("#driver-modal-raw").val();
                let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
                let data = { urn: /<key>topologyUri<\/key><value>(.*?)<\/value>/g.exec(xml)[1], type: /<key>driverEjbPath<\/key><value>(.*?)<\/value>/g.exec(xml)[1], xml: xml };
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
            } else {
                // Retrieve schema
                schema = JSON.parse(JSON.stringify(getSchema(type)));

                // Map input values to schema
                let entries = schema.driverInstance.properties[0].entry;
                for (let input of $(".driver-modal-body-content input")) {
                    let obj = entries.find(x => x.key[0] === input.name);
                    if (obj) { obj.value[0] = input.value; }
                }

                let xml = convert.json2xml(schema, { compact: true, spaces: 4 });
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
        }
    }
    update() {
        let page = this;
        $(".driver-modal-body-content input").each((i, ele) => {
            if ($(ele).val() !== $(ele).attr("value")) {
                console.log($(ele).prop("name") + " changed!");
                // Value changed, update property
                let apiURL = window.location.origin + "/StackV-web/restapi/driver/" + this.props.urn + "/" + $(ele).prop("name") + "/" + encodeURIComponent($(ele).val());
                $.ajax({
                    url: apiURL,
                    type: "PUT",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                        xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                    },
                    success: function (result) {

                    },
                });
            }
        });

        this.save();
    }
    delete() {
        let page = this;
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        let data = ($("#driver-modal-raw").size() === 1 ? { urn: /<key>topologyUri<\/key><value>(.*?)<\/value>/g.exec($("#driver-modal-raw").val())[1] } : { urn: $("input[name=\"topologyUri\"]").val() });
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
        if (this.props.type === "raw") {
            modalContent = (<textarea id="driver-modal-raw"></textarea>);
        } else {
            modalContent = this.parseInputFields();
        }

        return <div className="modal fade" id="driver-modal" style={{ top: "7%" }}>
            <div className="modal-dialog" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4 className="modal-title" id="driver-modal-label">Driver Editor</h4>
                    </div>
                    <div className="modal-body">
                        <p className="driver-modal-body-header">Select a service type:
                            <select id="driver-modal-body-select" value={this.props.type} onChange={(e) => this.changeType(e)}>
                                <option ></option>
                                <OptionElements />
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
                            <button type="button" className="btn btn-primary" id="button-editor-save" onClick={this.props.status === "Plugged" ? this.update : this.save}>Save</button>
                            <button type="button" className="btn btn-danger" id="button-editor-delete" disabled={this.props.status === "Plugged" ? true : undefined} onClick={this.delete}>Delete</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>;
    }

    parseInputFields() {
        let savedEntries;
        if (this.props.type) {
            $("#driver-modal-body-select").val(this.props.type);
            if (this.props.xml) { savedEntries = JSON.parse(convert.xml2json(this.props.xml, { compact: true, spaces: 4 })).driverInstance.properties.entry; }

            let schema = getSchema(this.props.type);
            if (schema.driverInstance) {
                let entries = schema.driverInstance.properties[0].entry;
                return entries.map(this.editCallback.bind(null, savedEntries, this.props.status === "Plugged", this.props.disabled));
            } else {
                return <textarea id="driver-modal-raw">{this.props.xml}</textarea>;
            }
        }
    }



    editCallback(saved, plugged, disabled, entry) {
        let formatted, key, value, savedValue, editable = false, required = false;
        if (entry.$) { editable = entry.$.editable === "true"; required = entry.$.required === "true"; }
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

        let modReadOnly = (editable
            ? (plugged && !disabled ? true : undefined)
            : (plugged ? true : undefined));

        switch (key) {
            case "topologyUri":
                return (<label style={{ width: "100%" }} key={key}>Topology URN<input className="form-control" name={key} data-original={savedValue} defaultValue={savedValue} onChange={(e) => urnChange(e)} /><br /></label>);
            case "driverEjbPath":
                return (<label key={key}>{formatted}<input className="form-control" name={key} value={value} readOnly /></label>);
            default:
                return (<label key={key}>{formatted}<input className="form-control" name={key} defaultValue={savedValue} readOnly={modReadOnly} /></label>);
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
        this.props.setType(e.target.value);
    }

    importRawDriver() {
        let page = this;
        let raw = $("#driver-modal-raw").val().replace(/\n| /g, "");

        let uriReg = /<key>topologyUri<\/key><value>(.*?)<\/value>/g;
        let urn = uriReg.exec(raw)[1];
        let ejbReg = /<key>driverEjbPath<\/key><value>(.*?)<\/value>/g;
        let type = ejbReg.exec(raw)[1];

        let apiUrl = window.location.origin + "/StackV-web/restapi/app/drivers/";
        let data = { urn: urn, type: type, xml: $("#driver-modal-raw").val() };
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

function OptionElements() {
    let map = schemas.map((d) => <option key={d.meta[0].name[0]} value={d.properties[0].entry.find(x => x.key[0] === "driverEjbPath").value[0]}>{d.meta[0].name[0]}</option>);
    map.push(<option key="raw" value="raw">Raw</option>);
    return map;
}

// ----- //
function getSchema(type) {
    return { driverInstance: schemas.find(x => x.properties[0].entry.find(y => y.key[0] === "driverEjbPath" && y.value[0] === type)) };
}