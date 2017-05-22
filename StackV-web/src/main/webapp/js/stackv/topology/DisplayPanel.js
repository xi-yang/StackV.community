"use strict";
define(["local/stackv/utils",
    "local/stackv/topology/Module",
    "local/stackv/topology/DropDownTree"
    ], function (utils, Module, DropDownTree) {
    // Utility functions
    var bsShowFadingMessage = utils.bsShowFadingMessage;
    var loadCSS = utils.loadCSS;
    var renderTemplate = utils.renderTemplate;
    var getAuthentication = utils.getAuthentication;
    var loadSettings = utils.loadSettings;
    
    // should have option for seperate css file as well 
    function DisplayPanel() {
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
         var displayTree; 
         
         function init(Name, Settings) {
            settings = loadSettings(Settings, defaults);       
             // Idea, specific "build" function that is used as callback 
             // after rendering of template. Perhaps have all of these modules
             // inherit from a Component module that has all of these 
            renderTemplate(settings, buildPanel.bind(undefined));
            displayPanel.name = Name;
            displayPanel.initNotify(Name);
            subscribeToMediatior();

         };
         
                          
         function buildPanel() {
            displayTree =  new DropDownTree(document.getElementById(settings.tree), displayPanel._OutputApi);
            displayTree.renderApi = displayPanel._Render.API;
            //displayTree.contextMenu = this.contextMenu;



   
            $("#" + settings.opener).click(function (evt) {
                openPanel();
                evt.preventDefault();
            });

         };  
          
         
        function openPanel() {
          $("#" + settings.opener).toggleClass("display-open");  
          $("#" + settings.root_container).toggleClass("display-open");  
        }

        function setDisplayName(name) {
           document.getElementById(settings.uri_label).innerText = name;
        } 
        
        function subscribeToMediatior() {
               /*
                * initerface
                * name:       
                */
               displayPanel._Mediator.subscribe('DisplayPanel_setDisplayName', function(msg, data) {
                    setDisplayName(data.name);
               });
               
               displayPanel._Mediator.subscribe('DisplayPanel_open', function(msg, data) {
                   openPanel();
               });
               
               displayPanel._Mediator.subscribe("DisplayPanel_clear", function(msg, data) {
                   displayTree.clear();
               });
               
               displayPanel._Mediator.subscribe("DisplayPanel_populateProperties", function(msg, data) {
                   data.e.populateProperties(displayTree);
               });
               
               // name, element, fullsize 
               displayPanel._Mediator.subscribe("DisplayPanel_redraw", function (msg, data) {
                        setDisplayName(data.name);
                        displayTree.clear();  
                        data.element.populateProperties(displayTree);
                        if (data.element.misc_elements.length > 0 )
                            displayTree.addChild("", "Separator", null);
                        data.element.populateTreeMenu(displayTree);
                        displayTree.addToHistory(data.name, data.type);
                        displayTree.draw();
                        displayTree.topViewShown = false;   
                        if (data.fullSize) {
                            displayTree.open();
                        }                        
               });
               
               // topLevelTopologies 
               // model
               displayPanel._Mediator.subscribe("DisplayPanel_drawTopLevel", function (msg, data) {
                    setDisplayName("Topologies");     
                    displayTree.clear();

                    if (data.topLevelTopologies.length === 0) {
                        for (var key in data.model.elementMap) {
                             var e = data.model.elementMap[key];
                             if (e.getType() === "Topology" && e.topLevel) {
                                 data.topLevelTopologies.push(e);                                
                                 var child = displayTree.addChild(e.getName(), "Element", e);
                                 e.populateTreeMenu(child);
                             }
                        }
                        // if still no top level topoligies  
                        if (data.topLevelTopologies.length === 0) {
                            displayTree.addChild("No top level topologies.", "Title", "No top level topologies.");
                        }
                    } else {
                        for (var i in data.topLevelTopologies) {
                            var topology = data.topLevelTopologies[i];
                            var child = displayTree.addChild(topology.getName(), "Element", e);
                            topology.populateTreeMenu(child);                        
                        }
                    }
                    
                    displayTree.draw();
                   
               });
        }
         
         var displayPanel =  {
            initMediator: function() {
                if (displayPanel.initArgs !== null) {
                    initToken = displayPanel._Mediator.subscribe(displayPanel.initArgs[0] + ".init", function(message, data) {
                        init.apply(null, displayPanel.initArgs);
                    });
                }
            },                          
            init: init         
        };
        // We make the Module the prototpe of the public interface because 
        // it consists mostly of public fields and methods 
        displayPanel.__proto__ = Module();
        return displayPanel;
       };
       return DisplayPanel;
     });
    