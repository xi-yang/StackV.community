/* global details_viz */
import React from "react";
import PropTypes from "prop-types";
import { Map } from "immutable";
import Mousetrap from "mousetrap";
import { css } from "emotion";
import { RotateLoader } from "react-spinners";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

import "./details.css";

import { resumeRefresh, initRefresh, pauseRefresh, refreshSync, setRefresh, timerChange } from "../refresh";

import DetailsPanel from "./components/details_panel";
import DetailsDots from "./components/details_dots";
import LoggingPanel from "../datatables/logging_panel";
import VisualizationPanel from "./components/visualization_panel";

var $intentModal = $("#details-intent-modal");
var intentConfig = {
    width: 750
};

const override = css`
    display: block;
    position: absolute;
    margin: auto;
    left: 50%;
    top: 30%;
    z-index: 100;
`;

class Details extends React.Component {
    constructor(props) {
        super(props);

        this.viewShift = this.viewShift.bind(this);
        this.load = this.load.bind(this);

        let page = this;
        Mousetrap.bind("left", function () { page.viewShift("left"); });
        Mousetrap.bind("right", function () { page.viewShift("right"); });

        this.init = this.init.bind(this);
        this.init();

        this.fetchData = this.fetchData.bind(this);
        this.state = this.fetchData();
        this.state.view = "details";

        this.state.refreshInterval = "live";
        this.state.loading = false;

        this.loadVisualization = this.loadVisualization.bind(this);
    }
    componentDidMount() {
        let page = this;
        let dataInterval = setInterval(function () {
            if (!(page.state.loading || page.state.refreshInterval === "paused")) {
                page.setState(page.fetchData());
            }
        }, (page.state.refreshInterval === "live" ? 500 : 1000 * page.state.refreshInterval));

        page.loadVisualization();
        let visInterval = setInterval(function () {
            page.loadVisualization();
        }, (10000));
        this.setState({ dataIntervalRef: dataInterval, visIntervalRef: visInterval });
    }
    componentWillUnmount() {
        clearInterval(this.state.dataIntervalRef);
        clearInterval(this.state.visIntervalRef);
    }

    load(seconds) {
        let page = this;
        this.setState({ loading: true });
        setTimeout(function () {
            page.setState({ loading: false });
        }, seconds * 1000);
    }

    render() {
        let modView = [];
        switch (this.state.view) {
            case "logging":
                modView = [true, false, false];
                break;
            case "details":
                modView = [false, true, false];
                break;
            case "visual":
                modView = [false, false, true];
                break;
        }
        let pageClasses = "page page-details";
        if (this.state.loading) {
            pageClasses = "page page-details loading";
        }
        return <div style={{ width: "100%", height: "100%" }}>
            <RotateLoader
                className={override}
                sizeUnit={"px"}
                size={15}
                color={"#7ED321"}
                loading={this.state.loading}
            />
            <div className={pageClasses}>
                <DetailsDots view={this.state.view}></DetailsDots>

                <LoggingPanel active={modView[0]} uuid={this.props.uuid}></LoggingPanel>
                <DetailsPanel active={modView[1]} uuid={this.props.uuid} meta={Map(this.state.meta)} state={Map(this.state.state)}
                    verify={Map(this.state.verify)} load={this.load} keycloak={this.props.keycloak} />
                <VisualizationPanel active={modView[2]} verify={Map(this.state.verify)}></VisualizationPanel>
                <div id="details-viz"></div>
            </div>
        </div>;
    }

    fetchData() {
        // Build 
        let page = this;
        let meta = {}, state = {}, verify = {};

        // Frontend superstate and metadata
        let apiUrl = window.location.origin + "/StackV-web/restapi/app/details/" + this.props.uuid + "/instance";
        $.ajax({
            url: apiUrl,
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (instance) {
                meta.alias = instance[1];
                meta.creation = instance[2];
                meta.owner = instance[3];
                state.super = instance[4];
                state.last = instance[5];

                let intent = instance[6];
                if (intent.length === 0) {
                    $("#button-view-intent").hide();
                } else {
                    $("#button-view-intent").show();

                    $("#details-intent-modal-text").text(intent);
                    var ugly = document.getElementById("details-intent-modal-text").value;
                    var obj = JSON.parse(ugly);
                    var pretty = JSON.stringify(obj, undefined, 4);
                    document.getElementById("details-intent-modal-text").value = pretty;
                }
            }
        });

        // Substate
        apiUrl = window.location.origin + "/StackV-web/restapi/app/service/" + this.props.uuid + "/substatus";
        $.ajax({
            url: apiUrl,
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (result) {
                state.sub = result;
            }
        });

        // Verification
        apiUrl = window.location.origin + "/StackV-web/restapi/app/details/" + this.props.uuid + "/verification";
        $.ajax({
            url: apiUrl,
            async: false,
            type: "GET",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
            },
            success: function (verification) {
                verify.state = verification[0];
                verify.result = verification[1];
                verify.run = verification[2];
                verify.time = verification[3];
                verify.addition = verification[4];
                verify.reduction = verification[5];
                verify.elapsed = verification[7];

                $.ajax({
                    url: apiUrl += "/drone",
                    async: false,
                    type: "GET",
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Authorization", "bearer " + page.props.keycloak.token);
                        xhr.setRequestHeader("Refresh", page.props.keycloak.refreshToken);
                    },
                    success: function (retCode) {
                        verify.drone = (retCode === "1");
                    }
                });
            }
        });

        return { meta: meta, state: state, verify: verify };
    }


    // =============== //    
    viewShift(dir) {
        switch (this.state.view) {
            case "logging":
                if (dir === "right") {
                    $("#logging-tab").removeClass("active");
                    $("#sub-details-tab").addClass("active");
                    $("#visual-tab").removeClass("active");
                    this.setState({ view: "details" });
                }
                break;
            case "details":
                switch (dir) {
                    case "left":
                        $("#logging-tab").addClass("active");
                        $("#sub-details-tab").removeClass("active");
                        $("#visual-tab").removeClass("active");
                        this.setState({ view: "logging" });
                        break;
                    case "right":
                        $("#logging-tab").removeClass("active");
                        $("#sub-details-tab").removeClass("active");
                        $("#visual-tab").addClass("active");
                        this.setState({ view: "visual" });
                        break;
                }
                break;
            case "visual":
                if (dir === "left") {
                    $("#logging-tab").removeClass("active");
                    $("#sub-details-tab").addClass("active");
                    $("#visual-tab").removeClass("active");
                    this.setState({ view: "details" });
                }
                break;
        }
    }

    init() {
        let page = this;
        $("#sub-nav").load("/StackV-web/nav/details_navbar.html", function () {
            initRefresh($("#refresh-timer").val());
        });

        $intentModal.iziModal(intentConfig);
        $intentModal.iziModal("setContent", "<textarea readonly id=\"details-intent-modal-text\"></textarea>");
    }

    loadVisualization() {
        details_viz(this.props.keycloak.token);
        let page = this;
        if (!(page.state.state.sub === "INIT" || (page.state.state.sub === "FAILED" && page.state.state.last === "INIT"))) {
            $("#details-viz").load("/StackV-web/details_viz.html", function () {
                document.getElementById("visual-panel").innerHTML = "";

                var States = {
                    "INIT": 0,
                    "COMPILED": 1,
                    "COMMITTING": 2,
                    "COMMITTING-PARTIAL": 2,
                    "COMMITTED": 3,
                    "FAILED": 4,
                    "READY": 5
                };

                var tabs = [
                    {
                        "name": "Service",
                        "state": "COMPILED",
                        "createContent": createVizTab.bind(undefined, "Service")
                    },
                    {
                        "name": "System",
                        "state": "COMMITTING",
                        "createContent": createVizTab.bind(undefined, "System")
                    },
                    {
                        "name": "Verification",
                        "state": "COMMITTED",
                        "createContent": createVizTab.bind(undefined, "Verification")
                    }
                ];

                createTabs();
                function createVizTab(viz_type) {
                    var div = document.createElement("div");
                    div.classList.add("viz");
                    div.id = "sd_" + viz_type;
                    div.appendChild(buildViz(viz_type + " Addition"));
                    div.appendChild(buildViz(viz_type + " Reduction"));

                    div.classList.add("tab-pane");
                    div.classList.add("fade");
                    div.classList.add("in");
                    div.classList.add("viz-tab-content");
                    return div;
                }

                function buildHeaderLink(id, text) {
                    var link = document.createElement("a");

                    link.href = "#";
                    link.classList.add("viz-hdr");
                    link.classList.add("unexpanded");
                    link.id = id;
                    link.text = text;
                    return link;
                }

                function buildViz(viz_type) {
                    var table = document.createElement("table");
                    table.classList.add("management-table");
                    table.classList.add("viz-table");
                    var headerRow = document.createElement("tr");
                    var vizRow = document.createElement("tr");
                    var additionHeader = document.createElement("th");
                    var reductionHeader = document.createElement("th");
                    var additionCell = document.createElement("td");
                    additionCell.classList.add("viz-cell");
                    var reductionCell = document.createElement("td");
                    reductionCell.classList.add("viz-cell");

                    let a;
                    switch (viz_type) {
                        case "System Addition":
                            table.id = "sd_System_Addition";

                            a = buildHeaderLink("sd_System_Addition_Link", "Addition");
                            additionHeader.appendChild(a);
                            additionCell.classList.add("viz-cell");
                            additionCell.id = "sd_System_Addition_Viz";

                            vizRow.appendChild(additionCell);

                            if (!$("#sysa_viz_div").hasClass("emptyViz")) {
                                var sysa_viz_div = document.getElementById("sysa_viz_div");
                                additionCell.appendChild(sysa_viz_div);
                                sysa_viz_div.classList.remove("hidden");
                            }
                            break;
                        case "System Reduction":
                            table.id = "sd_System_Reduction";

                            a = buildHeaderLink("sd_System_Reduction_Link", "Reduction");
                            reductionHeader.appendChild(a);
                            reductionCell.classList.add("viz-cell");
                            reductionCell.id = "sd_System_Reduction_Viz";

                            vizRow.appendChild(reductionCell);

                            if (!$("#sysr_viz_div").hasClass("emptyViz")) {
                                //  $(".system-delta-table").removeClass("hide");
                                var sysr_viz_div = document.getElementById("sysr_viz_div");
                                reductionCell.appendChild(sysr_viz_div);
                                sysr_viz_div.classList.remove("hidden");
                            }

                            break;
                        case "Service Addition":
                            table.id = "sd_Service_Addition";

                            a = buildHeaderLink("sd_Service_Addition_Link", "Addition");
                            additionHeader.appendChild(a);
                            additionCell.classList.add("viz-cell");
                            additionCell.id = "sd_Service_Addition_Viz";

                            vizRow.appendChild(additionCell);

                            if (!$("#serva_viz_div").hasClass("emptyViz")) {
                                // $(".service-delta-table").removeClass("hide");
                                var serva_viz_div = document.getElementById("serva_viz_div");
                                additionCell.appendChild(serva_viz_div);
                                serva_viz_div.classList.remove("hidden");
                            }

                            break;
                        case "Service Reduction":
                            table.id = "sd_Service_Reduction";

                            a = buildHeaderLink("sd_Service_Reduction_Link", "Reduction");
                            reductionHeader.appendChild(a);
                            reductionCell.classList.add("viz-cell");
                            reductionCell.id = "sd_Service_Addition_Viz";

                            vizRow.appendChild(reductionCell);

                            if (!$("#servr_viz_div").hasClass("emptyViz")) {
                                var servr_viz_div = document.getElementById("servr_viz_div");
                                reductionCell.appendChild(servr_viz_div);
                                servr_viz_div.classList.remove("hidden");
                            }
                            break;
                        case "Verification Addition":
                            a = buildHeaderLink("sd_Unverified_Addition_Link", "Unverified Addition");
                            additionHeader.appendChild(a);
                            additionCell.classList.add("viz-cell");
                            additionCell.id = "sd_Unverified_Addition_Viz";

                            vizRow.appendChild(additionCell);

                            a = buildHeaderLink("sd_Verified_Addition_Link", "Verified Addition");
                            reductionHeader.appendChild(a);
                            reductionCell.classList.add("viz-cell");
                            reductionCell.id = "sd_Verified_Addition_Viz";

                            vizRow.appendChild(reductionCell);


                            if (!$("#va_viz_div").hasClass("emptyViz") || !$("#ua_viz_div").hasClass("emptyViz")) {
                                var va_viz_div = document.getElementById("va_viz_div");
                                var ua_viz_div = document.getElementById("ua_viz_div");

                                additionCell.appendChild(ua_viz_div);
                                reductionCell.appendChild(va_viz_div);

                                ua_viz_div.classList.remove("hidden");
                                va_viz_div.classList.remove("hidden");

                            }

                            break;
                        case "Verification Reduction":
                            a = buildHeaderLink("sd_Unverified_Reduction_Link", "Unverified Reduction");
                            additionHeader.appendChild(a);
                            additionCell.classList.add("viz-cell");
                            additionCell.id = "sd_Unverified_Reduction_Viz";

                            vizRow.appendChild(additionCell);

                            a = buildHeaderLink("sd_Verified_Reduction_Link", "Verified Reduction");
                            reductionHeader.appendChild(a);
                            reductionCell.classList.add("viz-cell");
                            reductionCell.id = "sd_Verified_Reduction_Viz";

                            vizRow.appendChild(reductionCell);

                            if (!$("#vr_viz_div").hasClass("emptyViz") || !$("#ur_viz_div").hasClass("emptyViz")) {
                                var ur_viz_div = document.getElementById("ur_viz_div");
                                var vr_viz_div = document.getElementById("vr_viz_div");

                                additionCell.appendChild(ur_viz_div);
                                reductionCell.appendChild(vr_viz_div);

                                ur_viz_div.classList.remove("hidden");
                                vr_viz_div.classList.remove("hidden");
                            }
                            break;
                    }
                    if (viz_type.includes("Verification")) {
                        headerRow.appendChild(additionHeader);
                        headerRow.appendChild(reductionHeader);
                    } else if (viz_type.includes("Addition")) {
                        headerRow.appendChild(additionHeader);
                    } else {
                        headerRow.appendChild(reductionHeader);
                    }
                    table.appendChild(headerRow);
                    table.appendChild(vizRow);

                    return table;
                }



                function createTabs() {
                    $(".verification-table").addClass("hide");
                    $(".system-delta-table").addClass("hide");
                    $(".service-delta-table").addClass("hide");
                    $("#delta-Service").addClass("hide");
                    $("#delta-System").addClass("hide");


                    var tabBar = document.createElement("ul");
                    tabBar.classList.add("nav");
                    tabBar.classList.add("nav-tabs");

                    var tabContent = document.createElement("div");
                    tabContent.classList.add("tab-content");
                    tabContent.classList.add("viz-tab-content");

                    for (var i = 0; i < tabs.length; i++) {
                        var tab = tabs[i];

                        if ((tab.name === "Verification") && (page.state.verify.state === null))
                            continue;

                        if (States[tab.state] <= States[page.state.state.sub]) {
                            createTab(tab, tabBar);
                            tabContent.appendChild(tab.createContent());
                        }
                    }
                    if (tabBar.lastChild) {
                        tabBar.lastChild.classList.add("active");
                        tabContent.lastChild.classList.add("active");

                        var visualization_panel = document.getElementById("visual-panel");
                        visualization_panel.appendChild(tabBar);
                        visualization_panel.appendChild(tabContent);

                        setEvent();
                    }
                }

                function make_tab_id(tab) {
                    var id = tab.name.replace(/\s+/g, "");
                    return "sd_" + id;
                }
                function createTab(tab, tabBar) {
                    var li = document.createElement("li");
                    var a = document.createElement("a");
                    a.href = "#" + make_tab_id(tab);
                    a.text = tab.name;
                    a.setAttribute("data-toggle", "tab");
                    li.appendChild(a);
                    tabBar.appendChild(li);
                }

                function setEvent(container) {
                    //$(".viz-hdr")
                    //$(".details-viz-button").click();

                    $(".viz-hdr").on("click", function () {
                        var tab = $(this).closest(".tab-pane");

                        var hdr = $(this).closest("th");
                        var cell = hdr.closest("table").find("td").eq(hdr.index());
                        var table = $(this).closest("table");
                        var viz = cell.children().eq(0);
                        var text_model = viz.children(".details-viz-text-model");
                        var text_model_pre = text_model.children("pre").eq(0);


                        var text_model_pre_width = text_model_pre.width();
                        var text_model_pre_height = text_model_pre.height();

                        if (viz.hasClass("emptyViz"))
                            return;

                        var button = viz.children(".details-viz-recenter-button");

                        if ($(this).hasClass("unexpanded")) {
                            if (!$("#instance-details-table").hasClass("hide"))
                                $("#instance-details-table").addClass("hide");

                            tab.find(".viz-cell").not(cell).addClass("hide");
                            tab.find(".viz-hdr").closest("th").not(hdr).addClass("hide");
                            tab.find(".viz-table").not(table).addClass("hide");

                            viz.addClass("expanded-viz-div");
                            table.height("95%");
                            $(this).removeClass("unexpanded");
                            $(this).addClass("expanded");
                            button.trigger("click", [viz.width(), viz.height()]);

                            text_model_pre.width("inherit");
                            text_model_pre.addClass("expanded");
                            text_model_pre.height(viz.height() * 2);

                            pauseRefresh();
                        } else {
                            if ($("#instance-details-table").hasClass("hide") && !$(".viz-hdr.expanded").not(this).length) {
                                $("#instance-details-table").removeClass("hide");
                            }

                            tab.find(".viz-cell").not(cell).removeClass("hide");
                            tab.find(".viz-hdr").closest("th").not(hdr).removeClass("hide");
                            tab.find(".viz-table").not(".emptyVizTable").removeClass("hide");

                            table.height("10%");
                            viz.removeClass("expanded-viz-div");
                            $(this).removeClass("expanded");
                            $(this).addClass("unexpanded");
                            button.trigger("click", [viz.width(), viz.height()]);

                            text_model_pre.removeClass("expanded");
                            text_model_pre.width("initial");
                            text_model_pre.height(text_model_pre_height / 2.5);

                            resumeRefresh();
                        }

                    });
                }
            });
        }
    }
}
Details.propTypes = {
    keycloak: PropTypes.object.isRequired,
    uuid: PropTypes.string.isRequired,
};
export default Details;

// =================== //

function buildDeltaTable(type) {
    var panel = document.getElementById("details-panel");

    var table = document.createElement("table");
    table.className = "management-table details-table " + type.toLowerCase() + "-delta-table";
    table.id = type.toLowerCase() + "-delta-table";

    var thead = document.createElement("thead");
    thead.className = "delta-table-header";
    var row = document.createElement("tr");
    var head = document.createElement("th");
    head.innerHTML = type + " Delta";
    row.appendChild(head);

    head = document.createElement("th");
    head.innerHTML = "Verified";
    row.appendChild(head);

    head = document.createElement("th");
    head.innerHTML = "Unverified";
    row.appendChild(head);

    row.appendChild(head);

    thead.appendChild(row);
    table.appendChild(thead);

    var tbody = document.createElement("tbody");
    tbody.className = "delta-table-body";
    //tbody.id = "acl-body";

    row = document.createElement("tr");
    var prefix = type.substring(0, 4).toLowerCase();
    var add = document.createElement("td");
    row.appendChild(add);

    add = document.createElement("td");
    add.id = prefix + "-add";
    row.appendChild(add);

    var red = document.createElement("td");
    red.id = prefix + "-red";
    row.appendChild(red);

    tbody.appendChild(row);
    row = document.createElement("tr");
    var cell = document.createElement("td");
    cell.colSpan = "3";
    cell.innerHTML = "<button  class=\"details-model-toggle btn btn-default\" onclick=\"toggleTextModel('."
        + type.toLowerCase() + "-delta-table', '#delta-" + type + "');\">Toggle Text Model</button>";
    row.appendChild(cell);
    tbody.appendChild(row);

    table.appendChild(tbody);

    panel.appendChild(table);

}

function closeVisTabs() {
    $(".viz-hdr.expanded").click();
}

function toggleTextModel(viz_table, text_table) {
    if (!$(viz_table.toLowerCase()).length) {
        alert("Visualization not found");
    } else if (!$(text_table).length) {
        alert("Text model not found");
    } else {
        $(viz_table.toLowerCase()).toggleClass("hide");
        // delta-Service, service verification etc must always display before
        // everything else.
        if (text_table.toLowerCase().indexOf("service") > 0) {
            $(text_table).insertAfter("#instance-details-table");
        }
        $(text_table).toggleClass("hide");

    }
}