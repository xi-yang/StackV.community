import NodeDetail from './node-detail';


class NodeSwitch extends NodeDetail {
  /** @override */
  popoverCoordinate() {
    const pathRawData = this.hullData['__path_raw__'];
    const xPoints = pathRawData.map(d => d[0]);
    const yPoints = pathRawData.map(d => d[1]);

    const minX = Math.min.apply(null, xPoints);
    const maxX = Math.max.apply(null, xPoints);

    const zoomLevel = d3.zoomTransform(this.controller.svg.root.node());

    const xMiddle = (minX + maxX) / 2;
    const bottomY = Math.max.apply(null, yPoints) + ( 5 / zoomLevel.k );

    const relativePoint = { x: xMiddle, y: bottomY };

    return {
      x: relativePoint.x * zoomLevel.k + zoomLevel.x,
      y: relativePoint.y * zoomLevel.k + zoomLevel.y,
    };
  };

  /** @override */
  containerId() {
    return `switch-${this.nodeData.id}`;
  }

  /** @override */
  containerClass() {
    return 'switch-container';
  }

  /** @override */
  contentClass() {
    return 'switch-ports';
  }

  /** @override */
  containerWidth() {
    return `${this.nodeData.metadata['hasBidirectionalPort'].length * 35}px`;
  }

  /** @override */
  dataValid() {
    return this.hullData.hasOwnProperty('__path_raw__') &&
        this.nodeData &&
        this.nodeData.metadata &&
        this.nodeData.metadata.hasOwnProperty('hasBidirectionalPort') &&
        this.nodeData.metadata['hasBidirectionalPort'].length > 0;
  }

  /** @override */
  prepareContent(content) {
    this.nodeData.metadata['hasBidirectionalPort'].forEach(portId => {
      const { id: aliasPortId, metadata: portData } = this.controller.nodeFetcher(portId, true);

      let portAvailable = portData.hasOwnProperty('isAlias');

      let url = portAvailable ? '/StackV-web/img/rj45_on.svg' : '/StackV-web/img/rj45_off.svg';

      let portElement = content.append('img')
          .attr('id', `switch-port-${aliasPortId}`)
          .attr('src', url)
          .style('opacity', () => portAvailable ? '1' : '0.35')
          .on('contextmenu', () => {
            console.log(portData);
            d3.event.preventDefault();
            d3.event.stopPropagation();
          });

      if (portAvailable) {
        portElement
            .on('mouseenter', () => this.controller.visualModel.highlighter.show(portData['isAlias']))
            .on('mouseleave', () => this.controller.visualModel.highlighter.hide(portData['isAlias']));
      }
    });
  }

  /** @override */
  hide() {
    this.controller.markSwitchHidden(this.nodeId);
    super.hide();
  }
}

export default NodeSwitch;