import * as d3 from "d3";

import AllMockData from "./data.mock";
import DataModel from "./data-model/data-model";
import VisualModel from "./visual-model/visual-model";

window.r = AllMockData;
window.d3 = d3;

const data = new DataModel(AllMockData[0], { deepCopy: true });
window.data = data;
const view = new VisualModel(data, $("#vis-panel")[0]);
window.view = view;

d3.select("#reset-page").on("click", () => {
    view.restart();
});

d3.select("#del-state").on("click", () => {
    localStorage.clear();
    window.location.reload();
});

view.restart();