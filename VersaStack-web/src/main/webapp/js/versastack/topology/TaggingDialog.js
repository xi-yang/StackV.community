/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
"use strict";
define([], function () {
    function TaggingDialog (userName) {
        this.currentColor = "Red";
        this.label = "";
        this.sentData = "fakeURN";
        this.currentCMObj = null;
        this.username = userName;
        this.selectedColorBox = null;
        // need variable for dialog activiated 
        var that = this;
        var dialog = document.querySelector("#taggingDialog");
                
        var colorBoxes = document.getElementsByClassName("colorBox");
        
        this.init = function() {
            for (var i = 0; i < colorBoxes.length;  i++) {
                //var cb = colorBoxes[i].id;
                colorBoxes[i].onclick = function() {
                    if (that.selectedColorBox) {
                         that.selectedColorBox.classList.remove("colorBox-highlighted");
                     }
                    that.selectedColorBox = this;
                    that.selectedColorBox.classList.add( "colorBox-highlighted");
                    that.currentColor = that.selectedColorBox.id.split("box")[1];
                 };
            }
            
            document.getElementById("taggingDialogCloser").onclick = function() {
                that.closeDialog();
            };
            document.getElementById("taggingDialogCancel").onclick = function() { 
                that.closeDialog();
            };
            

            document.getElementById("taggingDialogOK").onclick = function() {
                that.label = document.getElementById("taggingDialogColorInputLabel").value;
                var serializedData = JSON.stringify({
                        user:  that.username,
                        identifier: that.sendData,
                        label: that.label,
                        color: that.currentColor
                    });
                // do only if data is valid 
                $.ajax({
                    headers: { 
                        'Accept': 'application/json',
                        'Content-Type': 'application/json' 
                    },

                    type: "PUT",
                    url: "/VersaStack-web/restapi/app/label",
                    data: serializedData,
                    
                    success: function (data) {
                       alert("Success");

                    },
                    error: function () {
//                    var labelInput = document.getElementById("taggingDialogColorInputLabel");
//                    labelInput.vallue = ""; 
//                    that.closeDialog();
//                    that.label = ""; 
                    }
                    
                });
                     var labelInput = document.getElementById("taggingDialogColorInputLabel");
                    labelInput.vallue = ""; 

           };            
        };

        this.closeDialog = function() {
            dialog.classList.remove( "taggingDialog-active");
        };
        
        this.openDialog = function(o) {
            dialog.classList.add( "taggingDialog-active");
            that.currentCMObj = o;
        };
        
    }
    return TaggingDialog;
});

