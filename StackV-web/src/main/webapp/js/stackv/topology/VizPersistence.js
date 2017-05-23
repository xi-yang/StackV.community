"use strict";
define(["local/stackv/utils",
    "local/stackv/topology/Module"
    ], function (utils, Module) {
    // Utility functions
    var bsShowFadingMessage = utils.bsShowFadingMessage;
    var loadCSS = utils.loadCSS;
    var renderTemplate = utils.renderTemplate;
    var getAuthentication = utils.getAuthentication;
    var loadSettings = utils.loadSettings;
    
    // should have option for seperate css file as well 
    function VizPersistence() {
        var initToken;
        /* Settings: 
          *   Users can provide optional settings. 
          *   
          *   Among other things, this object contains identifiers and classes 
          *   that are used by the component's logic.  If the user decides 
          *   to change the html or csss of the component, they can simply 
          *   pass in the changed values and the component's logic will be 
          *   intact. Styling and markup issues that don't affect the component's
          *   logic are handled in its css and html files. 
          *       
          * Settings: {
          *    // general 
          *       template_root: path of html
          *       template_filename: filename of html
          *       css_root: path of css 
          *       css_filename: filename of css
          *       
          *    // ids
          *      root_container: id of root container for component
          *      opener: id of component that opens tag panel on click
          *      clear_tags_button: button that clears tags on click
          *      tag_list:  id of unordered list (ul) that stores tags 
          *    
          *    // classes 
          *      tag_class: css class for tags
          *      color_box_class: css class for color selection classes
          *      color_box_highlighted_class: css class for color highlihgt effect
          *      tag_deletion_icon_classes: ex. fa fa-times tagDeletionIcon
          */

         var defaults = {
            parent_container: "tag-panel",
            root_container: "displayPanel",
            
            opener: "displayPanel-tab",
                    
                    
            uri_label: "displayName",
            tree: "treeMenu",
            
            template_root: "/StackV-web/data/viz_templates/",
            template_filename: "DisplayPanel.html",
            css_root: "/StackV-web/css/visualization/modules/", 
            css_filename: "DisplayPanel.css"
         };
        
         var settings = {};
         var auth = {}; // keycloack authentication info
         var userName;
         var persistant_data;

         function init(Name, Settings) {
            settings = loadSettings(Settings, defaults);       
             // Idea, specific "build" function that is used as callback 
             // after rendering of template. Perhaps have all of these modules
             // inherit from a Component module that has all of these 
            vizPeristence.name = Name;
            vizPeristence.initNotify(Name);
            subscribeToMediatior();
            
            window.onbeforeunload = function(){ 
                persistVisualization();
            };

            persistant_data = localStorage.getItem("viz-data");

         };
        
        // clear 
        // 
        function subscribeToMediatior() {
               /*
                * initerface
                * name:       
                */
               vizPeristence._Mediator.subscribe('VizPersistence_clear', function(msg, data) {
                   persistant_data = "undefined";
               });
               
               // width 
               // height 
               vizPeristence._Mediator.subscribe('VizPersistence_load', function(msg, data) {
                 if (!loadPersistedVisualization(vizPeristence._OutputApi, vizPeristence._Model, data.width, data.height)) {       
                    vizPeristence._Layout.doLayout(vizPeristence._Model, null, data.width, data.height);
                    vizPeristence._Layout.doLayout(vizPeristence._Model, null, data.width, data.height);
                    vizPeristence._Render.doRender(vizPeristence._OutputApi, vizPeristence._Model, undefined, undefined, undefined, vizPeristence._Mediator);
                 }
               });
        }
        
        function allNodesMatch(nodePositions, nodes){
           // may want to include intersect here 
           for (var node in nodePositions) {
               if (nodePositions[node].name !== nodes[node].getName())
                   return false;
           }
           return true;
       }
       // if they dont all match, we want to find the one that doesn't. 

      function removeOldFromPersist(nodePositions, nodeNames){   
           var pos = nodePositions;
           for (var i = 0; i < pos.length; i++) {
               if (!nodeNames.includes(pos[i].name)) {
                   pos.splice(i, 1);
                   i = 0;
               }
           }
           return pos;
       }     

       function getNewNodes(nodePositions, nodeNames) {
           var totalNodes = [];
           var newNodes = [];

           for (var i = 0; i < nodePositions.length; i++) {
               totalNodes.push(nodePositions[i].name);
           }    

           for (var i = 0; i < nodeNames.length; i++) {
               if (!totalNodes.includes(nodeNames[i])) {
                   newNodes.push(nodeNames[i]);
               }
           }

           return newNodes;
       }

       function AddNewToPersist(nodePositions, nodeNames, width, height, nodeSize) {
           var newNodes = getNewNodes(nodePositions, nodeNames);
           var newTopLevelTopologies = [];

           for (var i = 0; i < newNodes.length; i++) {
               var node = vizPeristence._Model.nodeMap[newNodes[i]];
               if (node.isTopology && node._parent === null) {
                   newTopLevelTopologies.push(newNodes[i]);
                   newNodes.splice(i, 1);
               }
           }

           // position new topologies 
           var top_offset = 0;
           for (var i = 0; i< newTopLevelTopologies.length; i++) {
               var pos = {};
               pos.name = newTopLevelTopologies[i];
               var totalTopoSize = 0;
               pos.x = width/2 + top_offset;
               pos.y = height/2 + top_offset;
               pos.dx = 0;
               pos.dy = 0;

               var node = vizPeristence._Model.nodeMap[newTopLevelTopologies[i]];
               var children = node.children;
               var maxSize = children.length * nodeSize;
               for (var j = 0; j < children.length; j++) {
                   var randX = (Math.random() * (maxSize/2)) + pos.x;
                   var randY = (Math.random() * (maxSize/2)) + pos.y;   
                   var childPos = {};
                   childPos.x = randX;
                   childPos.y = randY;
                   childPos.dx = 0;
                   childPos.dy = 0;
                   childPos.name = children[j].getName();
                   var index = newNodes.indexOf(childPos.name);
                   newNodes.splice(index, 1);
                   vizPeristence._Model.nodeMap[childPos.name].setPos(childPos);
               }
               var plusOrMinus = Math.random() < 0.5 ? -1 : 1;
               top_offset = pos.x + (plusOrMinus * (maxSize / 2));

               pos.size = maxSize / 4;
               vizPeristence._Model.nodeMap[pos.name].setPos(pos);
           }

           for (var i = 0; i< newNodes.length; i++) {
               var node = vizPeristence._Model.nodeMap[newNodes[i]];
               var maxSize = (node._parent.children.length * nodeSize) /3;
               var randX = (Math.random() * maxSize) + node._parent.x;
               var randY = (Math.random() * maxSize) + node._parent.y; 

               var pos = {};
               pos.x = randX;
               pos.y = randY;
               pos.dx = 0;
               pos.dy = 0;
               pos.size = nodeSize;
               pos.name = newNodes[i];                           
               nodePositions.push(pos);
               vizPeristence._Model.nodeMap[pos.name].setPos(pos);
           }

           //return nodePositions;                        
       }


       function loadPersistedVisualization(outputApi, model, width, height) {
           if (persistant_data !== undefined && persistant_data !== "undefined") {
               var viz_data = JSON.parse(persistant_data);
               if (viz_data !== null) {                    
                   var nodePositions = JSON.parse(viz_data['nodes']);

                   var nodes = model.listNodes();
                   var sameNodes = !(nodePositions.length !== nodes.length || !allNodesMatch(nodePositions, nodes));
                   width = width / parseFloat(viz_data.zoom);
                   height = height / parseFloat(viz_data.zoom);
                   if (sameNodes)  {
                       for (var i = 0; i < nodePositions.length; i++) {
                          var name = nodePositions[i].name;
                          model.nodeMap[name].setPos(nodePositions[i]);
                       }
                       vizPeristence._Layout.doPersistLayout(model, null, width, height);
                       vizPeristence._Layout.doPersistLayout(model, null, width, height);
                       vizPeristence._OutputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
                       vizPeristence._Render.doRender(outputApi, model, undefined, undefined, undefined, vizPeristence._Mediator);
                       outputApi.setZoom(parseFloat(viz_data.zoom));

                   } else {
                       var nodeNames = model.listNodeNames();

                       nodePositions = removeOldFromPersist(nodePositions, nodeNames);
                       AddNewToPersist(nodePositions, nodeNames, width, height, 21);

                       for (var i = 0; i < nodePositions.length; i++) {
                          var name = nodePositions[i].name;
                          model.nodeMap[name].setPos(nodePositions[i]);
                       }
                       vizPeristence._Layout.doPersistLayout(model, null, width, height);
                       vizPeristence._Layout.doPersistLayout(model, null, width, height);
                       vizPeristence._OutputApi.setOffsets(parseFloat(viz_data.offsetX), parseFloat(viz_data.offsetY)); 
                       vizPeristence._Render.doRender(vizPeristence._OutputApi, model, undefined, undefined, undefined, vizPeristence._Mediator);
                       outputApi.setZoom(parseFloat(viz_data.zoom));

                       //return false;
                   }
               } else {
                   return false;
               }
           } else {
               return false;
           }
           return true;
       }

       function persistVisualization() {
           var nodePositions = [];
           var nodes = vizPeristence._Model.listNodes();

           // doing this for security purposes, dont want to persist model 
           // data on client, just positions 
           for (var i = 0; i < nodes.length; i++) {
               var nodePos = nodes[i].getRenderedObj();
               nodePos.name = nodes[i].getName();
               nodePositions[i] = nodePos;
           }
           var toPersist = JSON.stringify(nodePositions);
           var offsets = vizPeristence._OutputApi.getOffset();

           var viz_data = {
               "nodes" : toPersist,
               "zoom"  : vizPeristence._OutputApi.getZoom(),
               "offsetX" : offsets[0],
               "offsetY" : offsets[1]
           };

           try {
               localStorage.setItem("viz-data", JSON.stringify(viz_data));
           } catch (err) {
               console.log(err);
           }                
       }
        
         var vizPeristence =  {
            initMediator: function() {
                if (vizPeristence.initArgs !== null) {
                    initToken = vizPeristence._Mediator.subscribe(vizPeristence.initArgs[0] + ".init", function(message, data) {
                        init.apply(null, vizPeristence.initArgs);
                    });
                }
            },                          
            init: init         
        };
        // We make the Module the prototpe of the public interface because 
        // it consists mostly of public fields and methods 
        vizPeristence.__proto__ = Module();
        return vizPeristence;
       };
       return VizPersistence;
     });
    