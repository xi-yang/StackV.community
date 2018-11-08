import React from "react";

class APIPanel extends React.Component {
    constructor(props) {
        super(props);

        this.executeRequest = this.executeRequest.bind(this);
    }

    render() {
        return <div className={this.props.active ? "top" : "bottom"} id="API-panel">
            <div id="API-header-div">API Module</div>
            <div id="logging-body-div">
                <select id="API-request">
                    <option value="GET">GET</option>
                    <option value="PUT">PUT</option>
                    <option value="POST">POST</option>
                    <option value="DELETE">DELETE</option>
                </select>
                <input type="text" placeholder="URL" id="URL" />
                <button id="SEND" type="button" className="action-button">Send</button>
            </div>
            <div id="logging-body-div">
                <textarea id="api_result" style={{ "color": "black" }}></textarea>
            </div>
        </div>;
    }
    componentDidMount() {
        let panel = this;
        $("#API-panel .action-button").click(function () {
            panel.executeRequest();
        });

        $(".checkbox-level").change(function () {
            if ($(this).is(":checked")) {
                $("#log-div").removeClass("hide-" + this.name);
            } else {
                $("#log-div").addClass("hide-" + this.name);
            }
        });
    }

    executeRequest() {
        let panel = this;
        var url_request = $("#API-request").val();
        var url = $("#URL").val();
        //var apiUrl = window.location.origin + '/StackV-web/restapi/app/'+"option" + document.getElementById("URL").value;
        var apiUrl = window.location.origin + "/StackV-web/restapi" + url;
        var type = url_request;

        var input = $("#api_result").val();

        let url_arr = url.split("/");

        if (type === "GET") {
            $.ajax({
                url: apiUrl,
                type: type,
                dataType: "text",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
                },
                success: function (result) {

                    if (typeof result === "string") {
                        if (url_arr[0] === "keycloak") {
                            var resultArr = String(result).split(",");
                            var jsonStr = "[";
                            var index = 0;
                            for (index = 0; index < resultArr.length; index += 2) {
                                if (index + 2 === resultArr.length) {
                                    jsonStr += "[" + "\"" + resultArr[index + 1] + "\"" + " , " + "\"" + resultArr[index] + "\"" + "]";
                                } else {
                                    jsonStr += "[" + "\"" + resultArr[index + 1] + "\"" + " , " + "\"" + resultArr[index] + "\"" + "],";
                                }
                            }
                            jsonStr += "]";
                            var jsonFormat = JSON.parse(jsonStr);

                            $("#api_result").val(JSON.stringify(jsonFormat, null, 2));
                        } else {
                            $("#api_result").val(result);
                        }
                    } else {
                        $("#api_result").val(JSON.stringify(result, null, 2));
                    }

                },
                error: function () {
                    $("#api_result").val("Failure");
                }
            });
        } else if (type === "PUT" || type === "POST") {
            $.ajax({
                url: apiUrl,
                type: type,
                data: input,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
                },
                success: function (result) {
                    if (type === "POST") {
                        $("#api_result").val(result);
                    } else {
                        $("#api_result").val("Success");
                    }
                },
                error: function () {
                    $("#api_result").val("Failure");
                }
            });
        } else if (type === "DELETE") {
            $.ajax({
                url: apiUrl,
                type: type,
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
                },
                success: function (result) {
                    $("#api_result").val("success");
                },
                error: function () {
                    $("#api_result").val("failure");
                }
            });
        } else if (type === "DELETE") {
            $.ajax({
                url: apiUrl,
                type: type,
                data: input,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + panel.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", panel.props.keycloak.refreshToken);
                },
                success: function (result) {
                    $("#api_result").val("Success");
                },
                error: function () {
                    $("#api_result").val("Failure");
                }
            });
        }
    }
}
export default APIPanel;
