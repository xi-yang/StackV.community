import * as d3 from "d3";
import $ from "jquery";

import AllMockData from "./data.mock";
import DataModel from "./data-model/data-model";
import StackVGraphic from "./visual-model/visual-model";
import ServerData from "./data-model/server-data/server-data";

window.r = AllMockData;
window.server = ServerData;
window.d3 = d3;

window.d = DataModel;
window.v = StackVGraphic;

const view = new StackVGraphic(AllMockData[0], document.querySelector("#vis-panel"));
window.data = view.dataModel;
window.view = view;

d3.select("#reset-page").on("click", () => {
    view.restart();
});

d3.select("#del-state").on("click", () => {
    localStorage.clear();
    window.location.reload();
});

d3.select("#dynamic-load-1").on("click", () => {
    view.update(AllMockData[1]);
});

d3.select("#dynamic-load-2").on("click", () => {
    view.update(AllMockData[2]);
});

d3.select("#dynamic-load-3").on("click", () => {
    view.update(AllMockData[3]);
});

view.restart();
