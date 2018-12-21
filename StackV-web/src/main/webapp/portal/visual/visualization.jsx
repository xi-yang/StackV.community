import React from "react";
import PropTypes from "prop-types";
import * as d3 from "d3";
import StackVGraphic from "stackv-visualization";

import "./visualization.css";

class Visualization extends React.Component {
    constructor(props) {
        super(props);

        this.initNew = this.initNew.bind(this);
        this.fetchDomains = this.fetchDomains.bind(this);
        this.fetchNewData = this.fetchNewData.bind(this);
    }
    componentDidMount() {
        if (this.props.visualMode === "new") {
            this.initNew("#vis-panel");
        }
    }

    initOld(selector) {

    }
    initNew(selector) {
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
                console.log(result);
                const view = new StackVGraphic(result, document.querySelector(selector));
                window.data = view.dataModel;
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

    render() {
        return <div id="vis-panel">
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
                domains.length = 0;
                for (let i = 2; i < result.length; i += 3) {
                    domains.push(result[i]);
                }
            },
        });

        let domainData = {};
        for (let domain of domains) {
            domainData[domain] = "null";
        }

        window.domainData = domainData;
    }

    fetchNewData() {
        let page = this;
        this.fetchDomains();
        var apiUrl = window.location.origin + "/StackV-web/restapi/model/refresh";
        var ret;
        $.ajax({
            url: apiUrl,
            type: "POST",
            async: false,
            data: JSON.stringify(window.domainData),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                console.log(result);
                ret = result;
            },
        });

        window.view.update(ret);
    }
}
Visualization.propTypes = {
    keycloak: PropTypes.object.isRequired,
    visualMode: PropTypes.string.isRequired,
};
export default Visualization;
