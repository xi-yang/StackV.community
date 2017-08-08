///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package net.maxgigapop.mrs.driver.openstack;
//
///**
// *
// * @author ys3p
// */
//public class backupclass {
//    
//    
//    //else if (o.get("request").toString().equals("CreateNetworkInterfaceRequest")) {
////                Port port = new NeutronPort();
////                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
////                String portname = o.get("port name").toString();
////                Subnet subnet = null;
////                for (Subnet sn : client.getSubnets()) {
////                    if (sn.getName().equals(o.get("subnet name").toString())) {
////                        subnet = sn;
////                        break;
////                    }
////
////                }
////                if (subnet == null) {
////                    throw logger.error_throwing(method, "unknown subnet:" + o.get("subnet name"));
////                }
////                if (o.get("private address").toString().equals("any")) {
////                    port.toBuilder().name(o.get("port name").toString())
////                            .fixedIp(null, subnet.getId())
////                            .networkId(subnet.getNetworkId());
////                } else {
////                    port.toBuilder().name(o.get("port name").toString())
////                            .fixedIp(o.get("private address").toString(), subnet.getId())
////                            .networkId(subnet.getNetworkId());
////                }
////                osClient.networking().port().create(port);
////
////                PortCreationCheck(portname, url, NATServer, username, password, tenantName, topologyUri);
////            }
//                        
//            else if (o.get("request").toString().equals("RunInstanceRequest")) {
//                ServerCreateBuilder builder = Builders.server();
//                // determine image and flavor
//                if (o.get("image").toString().equals("any") && o.get("image").toString().equals("any")) {
//                    builder.name(o.get("server name").toString())
//                            .image(client.getImages().get(0).getId())
//                            .flavor(client.getFlavors().get(0).getId());
//                } else if (o.get("image").toString().equals("any")) {
//
//                    builder.name(o.get("server name").toString())
//                            .image(client.getImages().get(0).getId())
//                            .flavor(o.get("flavor").toString());
//                } else if (o.get("flavor").toString().equals("any")) {
//
//                    builder.name(o.get("server name").toString())
//                            .image(o.get("image").toString())
//                            .flavor(client.getFlavors().get(0).getId());
//                } else {
//                    builder.name(o.get("server name").toString())
//                            .image(o.get("image").toString())
//                            .flavor(o.get("flavor").toString());
//                }
//                // optional keypair 
//                if (o.containsKey("keypair") && !o.get("keypair").toString().isEmpty()) {
//                    builder.keypairName(o.get("keypair").toString());
//                } 
//                // optional host placement
//                if (o.containsKey("host name")) {
//                    builder.availabilityZone("nova:"+o.get("host name").toString());
//                }
//                int index = 0;
//                while (true) {
//                    String key = "port" + Integer.toString(index);
//                    if (o.containsKey(key)) {
//                        OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
//                        for (Port p : client.getPorts()) {  //here need to be careful
//                            if (client.getResourceName(p).equals(o.get(key).toString())) {
//                                builder.addNetworkPort(p.getId());
//                                break;
//                            }
//                        }
//                        index++;
//                    } else {
//                        break;
//                    }
//                }
//                ServerCreate server = (ServerCreate) builder.build();
//                Server s = osClient.compute().servers().boot(server);
//                String servername = o.get("server name").toString();
//                VmCreationCheck(servername, url, NATServer, username, password, tenantName, topologyUri);
//                // optional secgroups 
//                if (o.containsKey("secgroup") && !o.get("secgroup").toString().isEmpty()) {
//                    String[] sgs = o.get("secgroup").toString().split(",|;|:");
//                    for (String secgroup : sgs) {
//                        SecurityGroupAddCheck(s.getId(), secgroup);
//                    }
//                }
//                if (o.containsKey("alt name")) {
//                    client.setMetadata(o.get("server name").toString(), "alt name", o.get("alt name").toString());
//                }
//            } 
//    
//}
