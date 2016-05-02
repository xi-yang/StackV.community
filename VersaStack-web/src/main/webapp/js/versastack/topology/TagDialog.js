/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
"use strict";
define([], function () {
    function TagDialog (userName) {
        this.currentColor = "Red";
        this.label = "";
        this.sentData = "";
        this.currentCMObj = null;
        this.username = userName;
        this.selectedColorBox = null;
        // need variable for dialog activiated 
        var that = this;
        that.dialog = document.querySelector("#tagDialog");
                
        var colorBoxes = document.getElementsByClassName("colorBox");
        
        this.init = function() {
            for (var i = 0; i < colorBoxes.length;  i++) {
                colorBoxes[i].onclick = function() {
                    if (that.selectedColorBox) {
                         that.selectedColorBox.classList.remove("colorBox-highlighted");
                     }
                    that.selectedColorBox = this;
                    that.selectedColorBox.classList.add( "colorBox-highlighted");
                    that.currentColor = that.selectedColorBox.id.split("box")[1];
                 };
            }
            
            document.getElementById("tagDialogCloser").onclick = function() {
                that.closeDialog();
            };
            document.getElementById("tagDialogCancel").onclick = function() { 
                that.closeDialog();
            };
            

            document.getElementById("tagDialogOK").onclick = function() {
                that.label = document.getElementById("tagDialogLabelInput").value;
                
                if (that.username === ""){
                    alert("Error: Please log in to submit tags. ");
                    that.closeDialog();
                }
                
                
                var serializedData = JSON.stringify({
                        user:  that.username,
                        identifier: that.label,
                        label: that.sentData,
                        color: that.currentColor.toLowerCase()
                });
                $.ajax({
                    crossDomain: true,
                    type: "PUT",
                    url: "/VersaStack-web/restapi/app/label",
                    data: serializedData,
                    contentType: "application/json", 
                    
                    success: function(data,  textStatus,  jqXHR ) {
                        //alert("Success\n" + data + "\n" + textStatus);
                        var tagList = document.querySelector("#labelList1");
                        var tag = document.createElement("li");
                        tag.classList.add("tagPanel-labelItem");
                        tag.classList.add("label-color-" + that.currentColor.toLowerCase());
                        tag.innerHTML = that.label;
                        tag.setAttribute('data-sentData', that.sentData);
                        tag.onclick = function() {
                            var textField = document.createElement('textarea');
                            textField.innerText = that.getSentData();
                            document.body.appendChild(textField);
                            textField.select();
                            document.execCommand('copy');
                            $(textField).remove();                    
                        };
                        tagList.appendChild(tag);
                        that.closeDialog();
                    },
                    error: function(jqXHR, textStatus, errorThrown ) {
                       //alert(errorThrown + "\n"+textStatus);
                       alert("Error adding tag.");
                       that.closeDialog();
                    }
                    
                });
                    var labelInput = document.getElementById("tagDialogLabelInput");
                    $('#tagDialogLabelInput').val("");

                    labelInput.vallue = ""; 
           };            
        };

        this.closeDialog = function() {
            $('#tagDialogLabelInput').removeAttr('value');
            that.dialog.classList.remove( "tagDialog-active");
        };
        
        this.openDialog = function(o) {
            that.dialog.classList.add( "tagDialog-active");
            that.currentCMObj = o;
            if (typeof that.currentCMObj.getName === 'function') {
                that.sentData = that.currentCMObj.getName();
            } else {
                that.sentData = that.currentCMObj;
            }
        };
        this.getSentData = function() {
            //alert(that.sentData);
            return that.sentData;
        };
    }
    return TagDialog;
});

