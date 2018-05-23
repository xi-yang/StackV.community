'use strict';
/* 
 * Copyright (c) 2013-2018 University of Maryland
 * Created by: Alberto Jimenez
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.
 * 
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

/* global XDomainRequest, baseUrl, keycloak, Power2, TweenLite, tweenBlackScreen, Mousetrap, swal, iziToast */

var $detailsModal = $("#details-modal");
var tweenVisPanel = new TweenLite("#vis-panel", .75, {ease: Power2.easeInOut, paused: true, autoAlpha: 1});

function loadVisualization() {
    tweenVisPanel.play();

    loadModals();
    loadListeners();
}

var detailsConfig = {
    title: 'Details',
    headerColor: '#3e4d5f',
    width: '50vh',
    transitionIn: 'fadeInDown',
    transitionOut: 'fadeOutUp',
    top: '104px',
    overlay: false
};

function loadModals() {
    // Initialize
    $detailsModal.html('<div id="details-modal-id"></div><div id="details-modal-data"></div>');

    // Enable
    $detailsModal.iziModal(detailsConfig);
}

function loadListeners() {
    // Node
    $("svg").on("click", "g", function () {
        let node = this.__data__;
        if (node === undefined) {
            return;
        }

        if ("metadata" in node) {
            let metadata = node.metadata;
            // Single node.
            $("#details-modal-id").text(metadata.id);
            
            if ("hasService" in metadata) {
                
            }
            
           console.log(node.metadata);
        } else if ("hullNodes" in node) {
            // Hull node.
            console.log(node.hullNodes);
        }
        
        if ($detailsModal.iziModal("getState") === "closed") {
            $detailsModal.iziModal("open");
        }
    });
}

