/**
 * This class will try to prevent calling dragStart or dragEnd if the mouse DID NOT MOVE
 */
class SVDragEvent {
  constructor(eventHandler) {
    this.eventHandler = eventHandler;
    this.dragStarted = false;
    this.dragMoved = false;
  }

  dragStartEvent() {
    const self = this;
    return function (...args) {
      self.dragStarted = true;
    };
  }

  dragMoveEvent() {
    const self = this;
    return function (...args) {
      if (!self.dragMoved) {
        // FIRST MOVE, TRIGGER DRAG_START
        self.eventHandler.dragStart && self.eventHandler.dragStart.apply(this, args);
        self.dragMoved = true;
      }
      self.eventHandler.dragMove && self.eventHandler.dragMove.apply(this, args);
    };
  }

  dragEndEvent() {
    const self = this;
    return function (...args) {
      if (self.dragStarted && self.dragMoved) {
        self.eventHandler.dragEnd && self.eventHandler.dragEnd.apply(this, args);
      }
      self.dragStarted = false;
      self.dragMoved = false;
    };
  }
}

export default SVDragEvent;