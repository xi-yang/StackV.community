"use strict";
define(["local/stackv/utils"], function (utils) {
    var arrayIncludes = utils.arrayIncludes;

    function Module() {
        console.log("Module: I exist.");
        var module = {}; 

        module.name = null;
        module._Mediator = null;
        module._Render = null;
        module.initArgs = null;
        module.initReqs = null;
        module.initToken = null; 
        module._Model = null; 

        module.initNotify = function(Name) {
            module._Mediator.publish("initalized", {name: Name});
            module._Mediator.unsubscribe(module.initToken);
            console.log("module initalized");
        };

        module.setMediator = function(Mediator) {
            module._Mediator = Mediator;
            module.initToken = module._Mediator.subscribe("initUpdate", function(msg, data) {
                var reqsSatisfied = arrayIncludes(data.init, module.initReqs);
                if (reqsSatisfied) {
                    console.log(module.initArgs);
                    module._Mediator.publish(module.initArgs[0] + ".init", null);
                    module._Mediator.unsubscribe(module.initToken);
                } else {
                    console.log("reqs not satisfied");
                }

            });
            
        };

        module.setVizProperties = function(Model, Render, OutputApi) {
            module._Model = Model;
            module._Render = Render;
            module._OutputApi = OutputApi; 
        };
        
        module.setInitProperties = function(args, reqs) {
            module.initArgs = args, 
            module.initReqs = reqs;
        };

        module.helloWorld = function () {console.log("Hello world");};

        module.makeSubscriptions = function(){};

        return module;       
    }
    return Module;
});
