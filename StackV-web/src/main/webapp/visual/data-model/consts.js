import * as d3 from 'd3';

/**
 * Black list key name to prevent back-refer to parent when calling recursively
 *
 * @type {Array<string>}
 */
export const RECURSION_BLACK_LIST_KEY_NAME_LIST = [
  'id',
  'type',
  'value',
  'labeltype',
  'encoding',
  'belongsTo',
  'providedByService',
  'aggregateChildren',
  'parentList',
  'directParentList',
  'isPortOf',
];

/**
 * For local persistent purpose
 *
 * @type {Array<string>}
 */
export const D3_GRAPHIC_KEYS = [
  'x',
  'y',
  'dx',
  'dy',
  'vx',
  'vy',
  'fx',
  'fy',
  'index',
];

/**
 * The default collision radius (graphic)
 *
 * @type {number}
 */
export const DEFAULT_COLLISION_RADIUS = 30;

/**
 * D3 function to generate a smooth path for hull}
 * @type {Function|*}
 */
export const HULL_CURVE = d3.line().curve(d3.curveCatmullRomClosed);
/**
 * Distance to the edge of hull
 *
 * @type {number}
 */
export const HULL_OFFSET = 35;


/**
 * Will only expand these sub-keys
 * @type {Array<string>}
 */
// const EXPAND_KEYS = ['hasNode', 'hasTopology', 'hasBidirectionalPort'];
export const EXPAND_KEYS = ['hasNode', 'hasTopology'];
