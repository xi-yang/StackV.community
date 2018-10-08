/* global XDomainRequest, baseUrl, keycloak */
export function loadLabels() {
    $("#tagPanel-tab").click(function (evt) {
        $("#tagPanel").toggleClass("closed");

        evt.preventDefault();
    });

    var tags = []; // stores tag objects {color, data, label}
    var selectedColors = []; // colors selected for filtering

    var colorBoxes = document.getElementsByClassName("filteredColorBox");
    var tagHTMLs = document.getElementsByClassName("tagPanel-labelItem");
    var that = this;
    var userName;

    this.init = function () {
        userName = sessionStorage.getItem("username");
        var token = sessionStorage.getItem("token");
        var loggedIn = sessionStorage.getItem("loggedIn");
        var baseUrl = window.location.origin;

        if (loggedIn) {
            $.ajax({
                // crossDomain: true,
                type: "GET",
                url: baseUrl + "/StackV-web/restapi/app/label/" + userName,
                dataType: "json",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "bearer " + keycloak.token);
                },

                success: function (data, textStatus, jqXHR) {
                    for (var i = 0, len = data.length; i < len; i++) {
                        var dataRow = data[i];
                        that.createTag(dataRow[0], dataRow[1], dataRow[2]);
                    }
                },

                error: function (jqXHR, textStatus, errorThrown) {
                    console.log(errorThrown + "\n" + textStatus);
                    console.log("Error retrieving tags.");
                }
            });
        }

        for (var i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i].onclick = function () {
                var selectedColor = this.id.split("box")[1].toLowerCase();
                var selectedIndex = selectedColors.indexOf(selectedColor);
                if (selectedIndex === -1) {
                    selectedColors.push(selectedColor);
                    this.classList.add("colorBox-highlighted");
                } else {
                    selectedColors.splice(selectedIndex, 1);
                    this.classList.remove("colorBox-highlighted");
                }

                that.updateTagList();
            };
        }
    };

    this.updateTagList = function () {
        var tagHTMLs = document.getElementsByClassName("tagPanel-labelItem");
        for (var i = 0; i < tagHTMLs.length; i++) {
            var curTag = tagHTMLs.item(i);
            var curColor = curTag.classList.item(1).split("label-color-")[1];
            if (selectedColors.length === 0) {
                curTag.classList.remove("hide");
            } else if (selectedColors.indexOf(curColor) === -1) {
                curTag.classList.add("hide");
            } else {
                curTag.classList.remove("hide");
            }
        }
    };

    this.createTag = function (label, data, color) {
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

        tag.onclick = function (e) {
            // Don't fire for events triggered by children. 
            if (e.target !== this)
                return;


            var textField = document.createElement("textarea");
            textField.innerText = data;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand("copy");
            $(textField).remove();

            $("#tagPanel").popover({ content: "Data copied to clipboard", placement: "top", trigger: "manual" });
            $("#tagPanel").popover("show");
            setTimeout(
                function () {
                    $("#tagPanel").popover("hide");
                    $("#tagPanel").popover("destroy");
                },
                1000);
        };
        tagList.appendChild(tag);
    };

    this.deleteTag = function (identifier, htmlElement, list) {
        var token = sessionStorage.getItem("token");

        $.ajax({
            crossDomain: true,
            type: "DELETE",
            url: "/StackV-web/restapi/app/label/" + userName + "/delete/" + identifier,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + token);
            },

            success: function (data, textStatus, jqXHR) {
                $("#tagPanel").popover({ content: "Tag Deleted", placement: "top", trigger: "manual" });
                $("#tagPanel").popover("show");
                setTimeout(
                    function () {
                        $("#tagPanel").popover("hide");
                        $("#tagPanel").popover("destroy");
                    },
                    1000);
                list.removeChild(htmlElement);
            },

            error: function (jqXHR, textStatus, errorThrown) {
                $("#tagPanel").popover({ content: "Error deleting tag.", placement: "top", trigger: "manual" });
                $("#tagPanel").popover("show");
                setTimeout(
                    function () {
                        $("#tagPanel").popover("hide");
                        $("#tagPanel").popover("destroy");
                    },
                    1000);

                //alert(errorThrown + "\n"+textStatus);
                //alert("Error deleting tag.");
            }
        });
    };
    $("#ClearAllTagsButton").click(function () {
        //var tagList = document.querySelector("#labelList1");
        var token = sessionStorage.getItem("token");

        $.ajax({
            crossDomain: true,
            type: "DELETE",
            url: "/StackV-web/restapi/app/label/" + userName + "/clearall",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "bearer " + token);
            },

            success: function (data, textStatus, jqXHR) {
                $("#tagPanel").popover({ content: "Tags Cleared", placement: "top", trigger: "manual" });
                $("#tagPanel").popover("show");
                setTimeout(
                    function () {
                        $("#tagPanel").popover("hide");
                        $("#tagPanel").popover("destroy");
                    },
                    1000);
                $("#labelList1").empty();
            },

            error: function (jqXHR, textStatus, errorThrown) {
                $("#tagPanel").popover({ content: "Error clearing tags.", placement: "top", trigger: "manual" });
                $("#tagPanel").popover("show");
                setTimeout(
                    function () {
                        $("#tagPanel").popover("hide");
                        $("#tagPanel").popover("destroy");
                    },
                    1000);

                //alert(errorThrown + "\n"+textStatus);
                //alert("Error deleting tag.");
            }
        });

    });
    this.init();
}