<%-- 
    Document   : tagPanel.jsp
    Created on : May 5, 2016, 2:13:44 PM
    Author     : aheard
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  

        <link rel="stylesheet" href="/VersaStack-web/css/tagPanel.css">       

        <div class="closed" id="tagPanel">
            <div id="tagPanel-tab">
                Tags
            </div>
            <div id ="tagPanel-contents">
                <div id="tagPanel-colorPanel">
                    <div id="tagPanel-colorPanelTitle"> Filter Colors</div>
                    <div id="tagPanelColorSelectionTab">
                        <span class="filteredColorBox" id="boxRed"></span>
                        <span class="filteredColorBox" id="boxOrange"></span>
                        <span class="filteredColorBox" id="boxYellow"></span>
                        <span class="filteredColorBox" id="boxGreen"></span>
                        <span class="filteredColorBox" id="boxBlue"></span>
                        <span class="filteredColorBox" id="boxPurple"></span>
                    </div>
                </div>
                <div id="tagPanel-labelPanel">
                    <div id="tagPanel-labelPanelTitle">Labels</div>
                    <div id="labelList-container">
                        <ul class="tagPanel-labelList" id="labelList1">
                        </ul>
                    </div>
                </div>
            </div>
        </div>

    <script>
        $("#tagPanel-tab").click(function (evt) {
            $("#tagPanel").toggleClass("closed");

             evt.preventDefault();
        });

       (function() {
            var tags = []; // stores tag objects {color, data, label}
            var selectedColors = []; // colors selected for filtering
            
            var colorBoxes = document.getElementsByClassName("filteredColorBox");
            var tagHTMLs = document.getElementsByClassName("tagPanel-labelItem");
            var that = this;
            
            this.init = function() {
                var userName = "${user.getUsername()}";
                // only do this if the user is logged in 
                if(userName !== "") {
                    $.ajax({
                        crossDomain: true,
                        type: "GET",
                        url: "/VersaStack-web/restapi/app/label/" + userName,
                        dataType: "json",

                        success: function(data,  textStatus,  jqXHR ) {
                            for (var i = 0, len = data.length; i < len; i++) {
                                var dataRow = data[i];
                                that.createTag(dataRow[0], dataRow[1], dataRow[2]);
                            }
                        },

                        error: function(jqXHR, textStatus, errorThrown ) {
                           alert(errorThrown + "\n"+textStatus);
                           alert("Error retrieving tags.");
                        }                  
                    });
                }
                
                for (var i = 0; i < colorBoxes.length;  i++) {
                    colorBoxes[i].onclick = function() {
                        var selectedColor = this.id.split("box")[1].toLowerCase();
                        var selectedIndex = selectedColors.indexOf(selectedColor);
                        if (selectedIndex === -1) {
                            selectedColors.push(selectedColor);
                            this.classList.add( "colorBox-highlighted");
                        } else {
                            selectedColors.splice(selectedIndex, 1);
                            this.classList.remove("colorBox-highlighted");
                        }      
                        
                        that.updateTagList();
                    };
                }
            };  
            
            this.updateTagList = function() {
               var tagHTMLs = document.getElementsByClassName("tagPanel-labelItem");
               for( var i = 0; i < tagHTMLs.length; i++){
                   var curTag = tagHTMLs.item(i);
                   var curColor = curTag.classList.item(1).split("label-color-")[1];
                   if (selectedColors.length === 0) {
                       curTag.classList.remove("hide");
                   } else if (selectedColors.indexOf(curColor) === -1){
                       curTag.classList.add("hide");
                   } else {
                       curTag.classList.remove("hide");
                   }
               }
            };
            
            this.createTag = function(label, data, color) {
                var tagList = document.querySelector("#labelList1");
                var tag = document.createElement("li");
                tag.classList.add("tagPanel-labelItem");
                tag.classList.add("label-color-" + color.toLowerCase());
                tag.innerHTML = label;
                tag.onclick = function() {
                    var textField = document.createElement('textarea');
                    textField.innerText = data;
                    document.body.appendChild(textField);
                    textField.select();
                    document.execCommand('copy');
                    $(textField).remove();                    
                };
                tagList.appendChild(tag);
            };
    
            this.init();
        })();
    </script>
