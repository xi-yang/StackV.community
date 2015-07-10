$(function () {
    $("#nav").load("/VersaStack-web/navbar.html");

    $(".button-service-select").click(function (evt) {
        $ref = "srvc/" + this.id.toLowerCase() + ".jsp #service-specific";
        // console.log($ref);

        $("#service-table").toggleClass("hide");
        $("#button-service-cancel").toggleClass("hide");
        $("#service-specific").load($ref);
        evt.preventDefault();
    });

    $("#button-service-cancel").click(function (evt) {
        $("#service-specific").empty();
        $("#button-service-cancel").toggleClass("hide");
        $("#service-table").toggleClass("hide");
    });

    $("#button-service-return").click(function (evt) {
        window.location.href = "/VersaStack-web/ops/catalog.jsp";

        evt.preventDefault();
    });

    $(".button-group-select").click(function (evt) {
        $ref = "user_groups.jsp?id=" + this.id;
        $ref = $ref.replace('select', '') + " #group-specific";
        // console.log($ref);
        $("#group-specific").load($ref);
        evt.preventDefault();
    });
});

function aclSelect(sel) {
    $ref = "privileges.jsp?id=" + sel.value + " #acl-tables";
    $("#acl-tables").load($ref);
}

function installSelect(sel) {
    if (sel.value !== null) {
        $ref = "/VersaStack-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-menu";
        $ref2 = "/VersaStack-web/ops/srvc/driver.jsp?form_install=" + sel.value + " #service-fields";
    }
    else {
        $ref = "/VersaStack-web/ops/srvc/driver.jsp #service-menu";
        $ref2 = "/VersaStack-web/ops/srvc/driver.jsp #service-fields";   
    }
    $("#service-top").load($ref);
    $("#service-bottom").load($ref2);
}

function driverSelect(sel) {
    if (sel.value !== null) {
        $ref = "/VersaStack-web/ops/srvc/driver.jsp?driver_id=" + sel.value + " #service-fields";
    }
    else $ref = "/VersaStack-web/ops/srvc/driver.jsp #service-fields";
    $("#service-bottom").load($ref);
}