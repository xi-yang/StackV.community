import React from "react";
import PropTypes from "prop-types";
import * as d3 from "d3";
import StackVGraphic from "stackv-visualization";
import ReactInterval from "react-interval";

import "./visualization.css";

class Visualization extends React.Component {
    constructor(props) {
        super(props);

        this.init = this.init.bind(this);
        this.fetchDomains = this.fetchDomains.bind(this);
        this.fetchNewData = this.fetchNewData.bind(this);
    }
    componentDidMount() {
        if (this.props.visualMode === "new") {
            this.init("#vis-panel");
        }
    }
    init(selector) {
        this.fetchDomains();
        window.d3 = d3;
        window.v = StackVGraphic;

        let page = this;
        var apiUrl = window.location.origin + "/StackV-web/restapi/model/refresh/all";
        var ret;
        $.ajax({
            url: apiUrl,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                const view = new StackVGraphic(result, document.querySelector(selector));
                window.data = view._dataModel;
                window.view = view;

                d3.select("#reset-page").on("click", () => {
                    view.restart();
                });

                d3.select("#del-state").on("click", () => {
                    localStorage.clear();
                    window.location.reload();
                });

                $(".stackv-graphic").css("height", "100%");
                $(".stackv-graphic").css("width", "100%");

                view.restart();
            },
        });
    }
    fetchNewData() {
        let page = this;
        this.fetchDomains();
        for (let domain of Object.keys(window.domainData)) {
            let apiURL = window.location.origin + "/StackV-web/restapi/model/refresh/" + encodeURIComponent(domain);
            $.ajax({
                url: apiURL,
                type: "GET",
                async: false,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                    xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                },
                success: function (result) {
                    window.domainData[domain] = result[domain];
                },
            });
        }
        window.view._dataModel._provideRawServerData(window.domainData);
        //window.view.restart();
    }

    render() {
        return <div id="vis-panel">
            <ReactInterval timeout={this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.fetchNewData} />
            <div id="render-indicator">
                <span className="circle"></span>
                <span className="text"></span>
            </div>
        </div>;
    }

    /* */
    fetchDomains() {
        let page = this;
        var domains = [];
        var apiUrl = window.location.origin + "/StackV-web/restapi/driver";
        $.ajax({
            url: apiUrl,
            type: "GET",
            async: false,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                for (let domain of result) {
                    domains.push(domain.topologyUri);
                }
            },
        });

        let domainData = {};
        for (let domain of domains) {
            domainData[domain] = "null";
        }

        window.domainData = domainData;
    }
}
Visualization.propTypes = {
    keycloak: PropTypes.object.isRequired,
    visualMode: PropTypes.string.isRequired,
};
export default Visualization;
