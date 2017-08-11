//parse addresses
//match with gateway
//  add to routes
//  port_profile
//    add to svcDelta
//    add to dependOn
//  stitch_port
//    add to dependOn
//    add to svcDelta
//    add to connCriteriaValue
//    add to createPathExportTo

(function(refUuid, topoUri, routes, gateways, vm_name, vm_interfaces, service) {
    var dependOn = '';
    var svcDelta = '';
    var createPathExportTo = '';

    var sriovList = [];
    for (var i in vm_interfaces) {
        if (i.type === 'SRIOV') {
            sriovList.push(i);
        }
    }
    var sriov_num = 0;
    for (var sriov in sriovList) {
        var ip = '';
        var mac = '';
        for (var ad in sriov.address.split(',')) {
            ip = ad.includes('ipv4') ? ad.slice(ad.indexOf('ipv4') + 5) : ip;
            mac = ad.includes('mac') ? ad.slice(ad.indexOf('mac') + 4) : ip;
            // is this rewrite intended behavior? 
        }
        for (var gateway in gateways) {
            if (gateway.name === sriov.name) {
                //TODO add to routes
                
                // intended behavior? (routes[0])
                if (gateway.routes[0].from) {
                    if (gateway.type === 'port_profile') {

                        dependOn += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vm_name + "-sriov" + sriov_num + "&gt;, ";
                        svcDelta += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vm_name + "-sriov" + sriov_num + "&gt;\n" +
                            "    a            spa:PolicyAction ;\n" +
                            "    spa:type     \"MCE_UcsSriovStitching\" ;\n" +
                            "    spa:dependOn &lt;x-policy-annotation:action:create-" + vm_name + "&gt;, &lt;x-policy-annotation:action:create-" + vm_name + "-eth0&gt;;\n" +
                            "    spa:importFrom &lt;x-policy-annotation:data:sriov-criteria-external-" + vm_name + "-sriov" + sriov_num + "&gt;.\n" +
                            "\n" +
                            "&lt;x-policy-annotation:data:sriov-criteria-external-" + vm_name + "-sriov" + i + "&gt;\n" +
                            "    a            spa:PolicyData;\n" +
                            "    spa:type     \"JSON\";\n" +
                            "    spa:value    \"\"\"{\n" +
                            "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vm_name + "\",\n" +
                            "       \"to_port_profile\": \"" + gateway.routes[0].from + "\",\n" +
                            "       \"mac_address\": \"" + mac + "\"" +
                            (ip ? "" : ",\n       \"ip_address\": \"" + ip + "\"") +
                            //(routes ? "" : ",\n       \"routes\": " + routes.replace("\\", "")) +
                            "\n    }\"\"\" .\n\n";
                    }
                }
                if (gateway.routes[0].to) {
                    if (gateway.type === 'stitch_port') {
                        dependOn += "&lt;x-policy-annotation:action:ucs-" + vm_name + "-sriov" + sriov + "-stitch&gt;, ";
                            svcDelta += "&lt;x-policy-annotation:action:ucs-" + vm_name + "-sriov" + sriov + "-stitch&gt;\n" +
                            "    a            spa:PolicyAction ;\n" +
                            "    spa:type     \"MCE_UcsSriovStitching\" ;\n" +
                            "    spa:dependOn &lt;x-policy-annotation:action:create-" + vm_name + "&gt;, &lt;x-policy-annotation:action:create-path&gt;;\n" +
                            "    spa:importFrom &lt;x-policy-annotation:data:" + vm_name + "-sriov" + sriov + "-criteria&gt; .\n" +
                            "\n" +
                            "&lt;x-policy-annotation:data:" + vm_name + "-sriov" + sriov + "-criteria&gt;\n" +
                            "    a            spa:PolicyData;\n" +
                            "    spa:type     \"JSON\";\n" +
                            "    spa:format    \"\"\"{\n" +
                            "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vm_name + "\",\n" +
                            "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn" + gateway.name + "%,\n" +
                            "       \"mac_address\": \"" + mac + "\"" +
                            (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"") +
                            //(routeArr.isEmpty() ? "" : ",\n       \"routes\": " + routeArr.toString().replace("\\", "")) +
                            "\n    }\"\"\" .\n\n";
                            createPathExportTo += "&lt;x-policy-annotation:data:" + vm_name + "-sriov" + sriov + "-criteria&gt;, ";
                    }
                }


            }
        }



    }
    dependOn = "&lt;" + topoUri + ":vt&gt;\n" +
        "   a  nml:Topology;\n" +
        "   spa:type spa:Abstraction;\n" +
        "   spa:dependOn  " + dependOn.slice(0, (dependOn.length - 2)) + ".\n\n";

    switch (service) {
        case 'dependOn':
            return dependOn;
        case 'sriov':
            return svcDelta;
        case 'createPathExportTo':
            return createPathExportTo;
        
    }
    
});
