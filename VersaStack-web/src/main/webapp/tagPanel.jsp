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
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.1/css/font-awesome.min.css">

        <div class="closed" id="tagPanel" data-toggle="popover">
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
            var userName;
            
            this.init = function() {
                userName = "${user.getUsername()}";
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
                
                var x = document.createElement("i");
                x.classList.add("fa");
                x.classList.add("fa-times");
                x.classList.add("tagDeletionIcon");      
                x.onclick = that.deleteTag.bind(undefined, label, tag, tagList);
                
                tag.innerHTML = label;
                tag.appendChild(x);

                tag.onclick = function(e) {
                    // Don't fire for events triggered by children. 
                    if (e.target !== this)
                        return;
  

                    var textField = document.createElement('textarea');
                    textField.innerText = data;
                    document.body.appendChild(textField);
                    textField.select();
                    document.execCommand('copy');
                    $(textField).remove();   
                    
                    $("#tagPanel").popover({content: "Data copied to clipboard", placement: "top", trigger: "manual"});
                    $("#tagPanel").popover("show");
                    setTimeout(
                      function(){$("#tagPanel").popover('hide');$("#tagPanel").popover('destroy');}, 
                    1000);          
                };
                tagList.appendChild(tag);
            };
            
            this.deleteTag = function (identifier, htmlElement, list) {
                    $.ajax({
                        crossDomain: true,
                        type: "DELETE",
                        url: "/VersaStack-web/restapi/app/label/" + userName + "/delete/" + identifier,

                        success: function(data,  textStatus,  jqXHR ) {
                            $("#tagPanel").popover({content: "Tag Deleted", placement: "top", trigger: "manual"});
                            $("#tagPanel").popover("show");
                            setTimeout(
                              function(){$("#tagPanel").popover('hide');$("#tagPanel").popover('destroy');}, 
                            1000);          
                            list.removeChild(htmlElement);
                        },

                        error: function(jqXHR, textStatus, errorThrown ) {
                            $("#tagPanel").popover({content: "Error deleting tag.", placement: "top", trigger: "manual"});
                            $("#tagPanel").popover("show");
                            setTimeout(
                              function(){$("#tagPanel").popover('hide');$("#tagPanel").popover('destroy');}, 
                            1000);          
                            
                           //alert(errorThrown + "\n"+textStatus);
                           //alert("Error deleting tag.");
                        }                  
                    });                
            };
            
            this.init();
        })();
    </script>
