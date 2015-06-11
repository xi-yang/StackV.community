define([
   
], function() {
   //For debuging
   breakout={};
   
   var nodeList,edgeList;
   function init(){
       var request = new XMLHttpRequest();
        request.open("GET","/VersaStack-web/restapi/model/");
//        request.open("GET","/VersaStack-web/data/graph-full.json");// A sample graph
        request.setRequestHeader("Accept","application/json",false);
        request.onload=function(){
            data=request.responseText;
            data=JSON.parse(data);
            map=JSON.parse(data.ttlModel);
//            map=data;//For use with data/graph-full.json
            breakout.map=map;
            
            //map is an associate array containing all of the elements
            //the key is the element id as refered to by other elements
            //The elements themselves are associative arrays
            //Here, we alias the keys used in the elements' associative arrays, and some common values
            values={
                type : "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                hasBidirectionalPort : "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort",
                isAlias : "http://schemas.ogf.org/nml/2013/03/base#isAlias",
                namedIndividual:"http://www.w3.org/2002/07/owl#NamedIndividual",
                topology:"http://schemas.ogf.org/nml/2013/03/base#Topology",
                node:"http://schemas.ogf.org/nml/2013/03/base#Node",
                bidirectionalPort:"http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort",
                hypervisorService:"http://schemas.ogf.org/mrs/2013/12/topology#HypervisorService",
                labelGroup:"http://schemas.ogf.org/nml/2013/03/base#LabelGroup",
                label:"http://schemas.ogf.org/nml/2013/03/base#Label",
                hasNode: "http://schemas.ogf.org/nml/2013/03/base#hasNode",
                hasService: "http://schemas.ogf.org/nml/2013/03/base#hasService",
                hasTopology: "http://schemas.ogf.org/nml/2013/03/base#hasTopology",
                networkAdress: "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress",
                bucket: "http://schemas.ogf.org/mrs/2013/12/topology#Bucket",
                routingService: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService",
                switchingSubnet: "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet", 
                hasNetworkAddress: "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress",
                provideByService: "http://schemas.ogf.org/mrs/2013/12/topology#providedByService",
                hasBucket: "http://schemas.ogf.org/mrs/2013/12/topology#hasBucket",
                belongsTo: "http://schemas.ogf.org/nml/2013/03/base#belongsTo",
                name: "http://schemas.ogf.org/nml/2013/03/base#name",
                tag: "http://schemas.ogf.org/mrs/2013/12/topology#Tag",
                route: "http://schemas.ogf.org/mrs/2013/12/topology#Route",
                volume: "http://schemas.ogf.org/mrs/2013/12/topology#Volume",
                virtualCloudService: "http://schemas.ogf.org/mrs/2013/12/topology#VirtualCloudService",
                blockStorageService: "http://schemas.ogf.org/mrs/2013/12/topology#BlockStorageService",
                routingTable: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable",
                switchingService:"http://schemas.ogf.org/nml/2013/03/base#SwitchingService",
                topopolgySwitchingService:"http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService",
                hasVolume:"http://schemas.ogf.org/mrs/2013/12/topology#hasVolume",
                objectStorageService:"http://schemas.ogf.org/mrs/2013/12/topology#ObjectStorageService"
            };
            
            
            //To assist in debuging, we mark all values as "processed" once we have done so
            //This allows us to easily check if their is a type that we have not handled
            //We begin by extracting all nodes/topologies
            nodeList=[];
            for(var key in map){
                val=map[key];
                types=val[values.type];
                breakout.type=types;
                for(var typeKey in types){
                    type=types[typeKey];
                    type=type.value;
                    switch(type){
                        case values.topology:
                        case values.node:
                            //We will later want to determine which nodes/topologies are root
                            //  that is to say, which nodes/topologies do not have a parent node/topology
                            val.isRoot=false;
                            val.processed=true;
                            nodeList.push(val);
                            break;
                        case values.bidirectionalPort:
                            if(values.hasBidirectionalPort in val){
                                subPortKeys=val[values.hasBidirectionalPort];
                                for(key in subPortKeys){
                                    subPortKey=subPortKeys[key].value;
                                    subPort=map[subPortKey];
                                    subPort.parentPort=val;
                                }
                            }
                        case values.namedIndividual://All elements have this
                        case values.switchingService:
                        case values.topopolgySwitchingService:
                        case values.hypervisorService:
                        case values.labelGroup:
                        case values.label:
                        case values.networkAdress:
                        case values.bucket:
                        case values.routingService:
                        case values.switchingSubnet:
                        case values.tag:
                        case values.route:
                        case values.volume:
                        case values.virtualCloudService:
                        case values.blockStorageService:
                        case values.routingTable:
                        case values.switchingService:
                        case values.objectStorageService:
                            break;
                        default:
                            console.log("Unknown type: "+type);
                            break;
                    }
                }
            }
            
            //We will construct a list of edges
            //To do this, we first iterate through the ports of each node.
            //We use the alias of the port to create an edge between ports
            //We also create a backlink in the port back to the node so that we can later convert the edge into an edge between nodes
            //To avoid duplicate edges, we mark the alias port as visited, and if we see a visted port, we do not add an edge
            //This requires that no port has multiple aliases (otherwise we risk missing edges)
            edgeList=[];
            for(var key in nodeList){
                node=nodeList[key];
                for(var key in node){
                    switch(key){
                        case values.hasBidirectionalPort:
                            ports=node[key];
                            for(var key in ports){
                                portKey=ports[key].value;
                                port=map[portKey];
                                port.node=node;
                                if(!port.processed && values.isAlias in port){
                                    aliasPortKey=port[values.isAlias][0].value;
                                    aliasPort=map[aliasPortKey];
                                    newEdge={portA:port, portB:aliasPort};
                                    edgeList.push(newEdge);
                                    aliasPort.processed=true;
                                }
                                port.processed=true;
                            }
                            break;
                        case "isRoot": //This is a key that we added to determine which elements are root in the node/topology tree
                        case "processed":  //This is key that we added to assist in detecting when we fail to handle a case
                        case values.type:
                        case values.hasNode:
                        case values.hasService:
                        case values.hasTopology:
                        case values.hasNetworkAddress:
                        case values.provideByService:
                        case values.hasBucket:
                        case values.belongsTo:
                        case values.name:
                        case values.hasVolume:
                            break;
                        default:
                            console.log("Unknown key: "+key); 
                    }
                }
            }
            
            
            //clean up the edgelist so it associates nodes instead of ports
            for(var key in edgeList){
                edge=edgeList[key];
                edge.nodeA=getNodeOfPort(edge.portA);
                edge.nodeB=getNodeOfPort(edge.portB);
                delete edge.portA;
                delete edge.portB;
            }
        };
        breakout.request=request; 
        request.send();
   }
   function getNodeOfPort(port){
       while("parentPort" in port){
           port=port.parentPort;
       }
       return port.node;
   }

    /** PUBLIC INTERFACE **/
    return {
        init : init,
        nodeList: function(){return nodeList},
        edgeList: function(){return edgeList},
        breakout : breakout
    };
    /** END PUBLIC INTERFACE **/

});