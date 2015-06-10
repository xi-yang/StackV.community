define([
   
], function() {
   //For debuging
   breakout={};
   
   var nodeList,edgeList;
   function init(){
       var request = new XMLHttpRequest();
        request.open("GET","/VersaStack-web/restapi/model/");
        request.setRequestHeader("Accept","application/json",false);
        request.onload=function(){
            data=request.responseText;
            data=JSON.parse(data);
            map=JSON.parse(data.ttlModel);
            breakout.map=map;
            
            //map is an associate array containing all of the elements
            //the key is the element id as refered to by other elements
            //The elements themselves are associative arrays
            //Here, we alias the keys used in the elements' associative arrays
            keys={
                type : "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                bidirectionalPort : "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort",
                hasBidirectionalPort : "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort",
                isAlias : "http://schemas.ogf.org/nml/2013/03/base#isAlias"
            }
            //Here we alias some common values that will be used
            values={
                namedIndividual:"http://www.w3.org/2002/07/owl#NamedIndividual",
                topology:"http://schemas.ogf.org/nml/2013/03/base#Topology",
                node:"http://schemas.ogf.org/nml/2013/03/base#Node"
            }
            
            //To assist in debuging, we mark all values as "processed" once we have done so
            //This allows us to easily check if their is a type that we have not handled
            //We begin by extracting all nodes/topologies
            nodeList=[];
            for(var key in map){
                val=map[key];
                types=val[keys.type];
                breakout.type=types;
                for(var typeKey in types){
                    type=types[typeKey];
                    type=type.value;
                    if(type===values.topology||type===values.node){
                        //Val is a node/topology
                        //We will later want to determine which nodes/topologies are root
                        //  that is to say, which nodes/topologies do not have a parent node/topology
                        val.isRoot=false;
                        val.processed=true;
                        nodeList.push(val);
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
            console.log("Iterating nodeList");
            for(var key in nodeList){
                node=nodeList[key];
                for(var key in node){
                    if(key===keys.hasBidirectionalPort){
                        portKey=node[key][0].value;
                        port=map[portKey];
                        port.node=node;
                        if(port.processed){
                            //We have already seen this edge from the other port
                            break;
                        }
                        aliasPortKey=port[keys.isAlias][0].value;
                        aliasPort=map[aliasPortKey];
                        newEdge={portA:port, portB:aliasPort};
                        edgeList.push(newEdge);
                        aliasPort.processed=true;
                        port.processed=true;
                    }
                }
            }
            
            //clean up the edgelist so it associates nodes instead of ports
            for(var key in edgeList){
                edge=edgeList[key];
                edge.nodeA=edge.portA.node;
                edge.nodeB=edge.portB.node;
                delete edge.portA;
                delete edge.portB;
            }
        };
        breakout.request=request; 
        request.send();
        
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