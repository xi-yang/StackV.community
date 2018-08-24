/**
 * In some situation (XML convert to JSON issue), some KEY did contain an Array
 * with only 1 child element ONLY
 *
 * For better data modelling, we are removing those ONE-ELEMENT-ARRAY,
 * and replace the ONE-ELEMENT-ARRAY with the ONLY-CHILD data
 *
 * For example
 *
 * {
 *   "id": "aws-asia-s3",
 *   "isAlias": [ { value: "aws-na-s3" } ],  // Useless ONE-ELEMENT-ARRAY
 * }
 *
 * would become
 *
 * {
 *   "id": "aws-asia-s3",
 *   "isAlias": "aws-na-s3",  // REPLACE IT, YAY!
 * }
 *
 * @returns {boolean} Should replace ONE-ELEMENT-ARRAY OR not
 */
export function shouldRemoveOneElementArray(keyName) {
  return shouldRemoveOneElementArray_keyList.indexOf(keyName) !== -1;
}

/**
 * Private constant for shouldRemoveArray(keyName) function
 *
 * @type {Array<string>}
 */
const shouldRemoveOneElementArray_keyList = ['value', 'type', 'name', 'isAlias', 'labeltype'];

/**
 * Determine if the string have useless schema URL
 *
 * @param {string} str - A string
 * @returns {boolean} If the the string contains useless schema URL OR not
 */
export function containSchemaURL(str) {
  return typeof(str) === 'string' &&
      (str.startsWith('http://schemas.ogf.org') || str.startsWith('http://www.w3.org'));
}

/**
 * Remove the scheme URL in string
 * @param {string} str - A string
 * @returns {string} A string without scheme URL
 */
export function removeSchemeURL(str) {
  return str.replace(/^http(.+)#/gi, '');
}

/**
 * Determine if the (Object) need to assign NODE-ID
 *
 * @param {string} keyName - (Object) KEY NAME
 * @returns {boolean} If the KEY NAME should assign a NODE-ID to information body OR not
 */
export function shouldAssignNodeId(keyName) {
  return keyName.startsWith('urn:ogf') || keyName.startsWith('_:');
}