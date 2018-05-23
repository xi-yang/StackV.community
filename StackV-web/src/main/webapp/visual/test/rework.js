import * as d3 from 'd3';

import AllMockData from './data.mock';
import DataModel from './data-model/data-model';
import VisualModel from './visual-model/visual-model';


const data = new DataModel(AllMockData[1], { deepCopy: true });
const view = new VisualModel(data, document.getElementById("vis-panel"));

d3.select('#reset-page').on('click', () => {
  view.restart();
});

d3.select('#del-state').on('click', () => {
  localStorage.clear();
  window.location.reload();
});

view.restart();

window.data = data;
window.view = view;
window.d3 = d3;