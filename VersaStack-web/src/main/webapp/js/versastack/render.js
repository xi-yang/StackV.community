define([
    "local/d3", "local/versastack/utils"
], function (d3, utils) {
    var map_ = utils.map_;

    /**@param {outputApi} outputApi
     * @param {Model} model**/
    function doRender(outputApi, model) {
        var svgContainer = outputApi.getSvgContainer();
        redraw();

        function redraw() {
            svgContainer.selectAll("*").remove();
            var nodeList = model.listNodes();
            var edgeList = model.listEdges();
            map_(nodeList, drawTopology);
            map_(edgeList, drawEdge);
            map_(nodeList, drawNode);
        }

        /**@param {Node} n**/
        function drawNode(n) {
            if (n.isLeaf()) {
                var svgNode = svgContainer.append("image")
                        .attr("x", n.x - settings.NODE_SIZE / 2)
                        .attr("y", n.y - settings.NODE_SIZE / 2)
                        .attr("xlink:href", n.getIconPath())
                        .attr('height', settings.NODE_SIZE)
                        .attr('width', settings.NODE_SIZE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n));
                //register the drag listener
                var drag = d3.behavior.drag()
                        .on("drag", function () {
                            console.log("DRAG");
                            var e = d3.event;
                            n.x = e.x;
                            n.y = e.y;
                            redraw();
                        });
                svgNode.call(drag);
            }
        }
        /**@param {Node} n**/
        function drawTopology(n) {
            if (!n.isLeaf()) {
                //render the convex hull surounding the decendents of n
                var leaves = n.getLeaves();
                //workaround for small topologies
                if (leaves.length === 0) {
                    return;
                }
                ;
                while (leaves.length < 3) {
                    leaves.push(leaves[0]);
                }

                var path = d3.geom.hull()
                        .x(function (n) {
                            return n.x;
                        })
                        .y(function (n) {
                            return n.y;
                        })
                        (leaves);
                var hull = svgContainer.append("path")
                        .style("fill", "steelblue")
                        .style("stroke", "steelblue")
                        .style("stroke-width", settings.TOPOLOGY_SIZE)
                        .style("stroke-linejoin", "round")
                        .style("stroke-opacity", "20%")
                        .datum(path)
                        .attr("d", function (d) {
                            //@param d is the datum set above
                            //see https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d
                            if (d.length === 0) {
                                return;
                            }
                            var ans = "M" + d[0].x + " " + d[0].y + " ";
                            for (var i = 1; i < d.length; i++) {
                                ans += "L" + d[i].x + " " + d[i].y + " ";
                            }
                            ans += "Z"
                            return ans;
                        })
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n));
                //register the onDrag container

                var drag = d3.behavior.drag()
                        .on("drag", function () {
                            var e = d3.event;
                            move(n, e.dx, e.dy);
                            redraw();
                        });
                hull.call(drag);
            }

        }
        /**@param {Edge} e**/
        function drawEdge(e) {
            svgContainer.append("line")
                    .attr("x1", e.source.x)
                    .attr("y1", e.source.y)
                    .attr("x2", e.target.x)
                    .attr("y2", e.target.y)
                    .attr("style", "stroke:rgb(0,0,0);stroke-width:2");

        }

        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            outputApi.setActiveName(n.getName());
        }
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeDblClick(n) {
            n.toggleFold();
            redraw();
        }


        /**@param {Node} n**/
        function move(n, dx, dy) {
            n.x += dx;
            n.y += dy;
            map_(n.children, function (child) {
                move(child, dx, dy);
            });
        }
    }

    var settings = {
        NODE_SIZE: 30,
        TOPOLOGY_SIZE: 45
    }

    return{
        doRender: doRender
    };
});