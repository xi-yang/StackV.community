import NodeDetail from './node-detail';


class NodeServices extends NodeDetail {
  /** @override */
  popoverCoordinate() {
    const pathRawData = this.hullData['__path_raw__'];
    const xPoints = pathRawData.map(d => d[0]);
    const yPoints = pathRawData.map(d => d[1]);

    const minX = Math.min.apply(null, xPoints);
    const maxX = Math.max.apply(null, xPoints);

    const zoomLevel = d3.zoomTransform(this.controller.svg.root.node());

    const xMiddle = (minX + maxX) / 2;
    const topY = Math.min.apply(null, yPoints) - ( 45 / zoomLevel.k );

    const relativePoint = { x: xMiddle, y: topY };

    return {
      x: relativePoint.x * zoomLevel.k + zoomLevel.x,
      y: relativePoint.y * zoomLevel.k + zoomLevel.y,
    };
  };

  /** @override */
  containerId() {
    return `services-${this.nodeData.id}`;
  }

  /** @override */
  containerClass() {
    return 'services-container';
  }

  /** @override */
  contentClass() {
    return 'services-ports';
  }

  /** @override */
  containerWidth() {
    return `${this.nodeData.metadata['hasService'].length * 39 + 5}px`;
  }

  /** @override */
  dataValid() {
    return this.hullData.hasOwnProperty('__path_raw__') &&
        this.nodeData &&
        this.nodeData.metadata &&
        this.nodeData.metadata.hasOwnProperty('hasService') &&
        this.nodeData.metadata['hasService'].length > 0;
  }

  /** @override */
  prepareContent(content) {
    this.nodeData.metadata['hasService'].forEach(portId => {
      const { id: aliasPortId, metadata: portData } = this.controller.nodeFetcher(portId, true);
      let url = '/src/static/img/circle-o.svg';

      content.append('img')
          .attr('id', `service-node-${aliasPortId}`)
          .attr('src', url)
          .on('contextmenu', () => {
            console.log(portData);
            d3.event.preventDefault();
            d3.event.stopPropagation();
          });
          // .on('mouseenter', () => console.log(portData))
          // .on('mouseleave', () => console.log(portData));
    });
  }

  /** @override */
  hide() {
    this.controller.markServicesDialogHidden(this.nodeId);
    super.hide();
  }
}

export default NodeServices;