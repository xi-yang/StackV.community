-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: May 17, 2017 at 04:30 PM
-- Server version: 5.5.42
-- PHP Version: 5.6.7

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `frontend`
--
DROP DATABASE `frontend`;
CREATE DATABASE IF NOT EXISTS `frontend` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `frontend`;

-- --------------------------------------------------------

--
-- Table structure for table `acl`
--

DROP TABLE IF EXISTS `acl`;
CREATE TABLE `acl` (
  `acl_id` int(11) NOT NULL,
  `subject` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `is_group` tinyint(1) NOT NULL DEFAULT '0',
  `object` varchar(45) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


-- --------------------------------------------------------

--
-- Table structure for table `driver_wizard`
--

DROP TABLE IF EXISTS `driver_wizard`;
CREATE TABLE `driver_wizard` (
  `username` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivername` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `data` longtext COLLATE utf8_unicode_ci,
  `TopUri` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivertype` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `driver_wizard`
--

INSERT INTO `driver_wizard` (`username`, `drivername`, `description`, `data`, `TopUri`, `drivertype`) VALUES
('admin', 'test', 'test', '{"jsonData":[{}]}', '', 'AwsDriver'),
('admin', 'test2', 'test', '{"jsonData":[{}]}', '', 'OpenStackDriver');

-- --------------------------------------------------------

--
-- Table structure for table `label`
--

DROP TABLE IF EXISTS `label`;
CREATE TABLE `label` (
  `identifier` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `label` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `color` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `label`
--

INSERT INTO `label` (`identifier`, `username`, `label`, `color`) VALUES
('real_test', 'admin', 'urn:ogf:network:rains.maxgigapop.net:mira:dtn03.pub.alcf.anl.gov', 'orange'),
('test', 'admin', 'urn:ogf:network:rains.maxgigapop.net:mira:parallelfilesystem-/gpfs/mira-fs1', 'purple'),
('test 2', 'admin', 'urn:ogf:network:domain=sdnx.maxgigapop.net:node=MCLN', 'red'),
('test1', 'admin', 'Test 1', 'red'),
('test2', 'admin', 'Test 2', 'blue'),
('test3 ', 'admin', 'urn:ogf:network:rains.maxgigapop.net:mira:dtn07.pub.alcf.anl.gov:nic-xeth0.2200', 'purple');

-- --------------------------------------------------------

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
CREATE TABLE `log` (
  `log_id` int(11) NOT NULL,
  `marker` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `timestamp` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
  `level` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `logger` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `module` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `method` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `referenceUUID` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `targetID` varchar(40) COLLATE utf8_unicode_ci DEFAULT NULL,
  `event` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `message` longtext COLLATE utf8_unicode_ci NOT NULL,
  `severity` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `exception` longtext COLLATE utf8_unicode_ci
) ENGINE=InnoDB AUTO_INCREMENT=27081 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
  `service_id` int(11) NOT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `filename` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(140) COLLATE utf8_unicode_ci NOT NULL,
  `atomic` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service`
--

INSERT INTO `service` (`service_id`, `name`, `filename`, `description`, `atomic`) VALUES
(1, 'User Management', 'usermgt', 'Administrative Management Functions.', 1),
(2, 'Provisioning', 'provision', 'System and Topology Overviews.', 1),
(3, 'Orchestration', 'orchest', 'Manipulation of the System Model.', 1),
(4, 'Monitoring', 'monitor', 'System Monitoring and Logging.', 1),
(7, 'Driver Management', 'driver', 'Installation and Uninstallation of Driver Instances.', 1),
(8, 'Virtual Machine Management', 'vmadd', 'Management, Instantiation, and Setup of Virtual Machine Topologies.', 1),
(9, 'View Filter Management', 'viewcreate', 'Management and Creation of graphical view filters.', 1),
(10, 'Virtual Cloud Network', 'netcreate', 'Network Creation Pilot Testbed', 0),
(11, 'Dynamic Network Connection', 'dnc', 'Creation of new network connections.', 0),
(12, 'Flow based Layer2 Protection', 'fl2p', 'Switching of protection and recovery path.', 1),
(13, 'Advanced Hybrid Cloud', 'hybridcloud', 'Advanced Hybrid Cloud Service.', 0);

-- --------------------------------------------------------

--
-- Table structure for table `service_delta`
--

DROP TABLE IF EXISTS `service_delta`;
CREATE TABLE `service_delta` (
  `service_delta_id` int(11) NOT NULL,
  `service_instance_id` int(11) NOT NULL,
  `service_history_id` int(11) NOT NULL,
  `type` varchar(60) COLLATE utf8_unicode_ci NOT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `delta` longtext COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_history`
--

DROP TABLE IF EXISTS `service_history`;
CREATE TABLE `service_history` (
  `service_history_id` int(11) NOT NULL,
  `service_instance_id` int(11) NOT NULL,
  `service_state_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_instance`
--

DROP TABLE IF EXISTS `service_instance`;
CREATE TABLE `service_instance` (
  `service_instance_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `creation_time` datetime DEFAULT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `alias_name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `service_state_id` int(11) DEFAULT NULL
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_state`
--

DROP TABLE IF EXISTS `service_state`;
CREATE TABLE `service_state` (
  `service_state_id` int(11) NOT NULL COMMENT '	',
  `super_state` varchar(45) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service_state`
--

INSERT INTO `service_state` (`service_state_id`, `super_state`) VALUES
(2, 'Cancel'),
(1, 'Create'),
(5, 'Delete'),
(3, 'Modify'),
(4, 'Reinstate');

-- --------------------------------------------------------

--
-- Table structure for table `service_verification`
--

DROP TABLE IF EXISTS `service_verification`;
CREATE TABLE `service_verification` (
  `service_instance_id` int(11) NOT NULL,
  `verification_state` int(11) DEFAULT NULL,
  `verification_run` int(11) NOT NULL DEFAULT '0',
  `delta_uuid` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `creation_time` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `verified_reduction` longtext COLLATE utf8_unicode_ci,
  `verified_addition` longtext COLLATE utf8_unicode_ci,
  `unverified_reduction` longtext COLLATE utf8_unicode_ci,
  `unverified_addition` longtext COLLATE utf8_unicode_ci,
  `reduction` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `addition` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_wizard`
--

DROP TABLE IF EXISTS `service_wizard`;
CREATE TABLE `service_wizard` (
  `service_wizard_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `wizard_json` longtext COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `editable` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service_wizard`
--

INSERT INTO `service_wizard` (`service_wizard_id`, `service_id`, `username`, `name`, `wizard_json`, `description`, `editable`) VALUES
(1, 11, NULL, 'DNC.1xConn.VLAN_any.v1', '{\n    "userID": "admin",\n    "type": "dnc",\n    "alias": "DNC.1xConn.VLAN_any",\n    "data": {\n        "connections": [\n            {\n                "name": "conn1",\n                "terminals": [\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*",\n                        "vlan_tag": "any"\n                    },\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*",\n                        "vlan_tag": "any"\n                    }\n                ]\n            }\n        ]\n    }\n}', 'DNC w/ 1 x L2 Path + Any VLAN', 0),
(2, 11, NULL, 'DNC.2xConn.VLAN_range.v1', '{\n    "userID": "admin",\n    "type": "dnc",\n    "alias": "DNC.2xConn.VLAN_range",\n    "data": {\n        "connections": [\n            {\n                "name": "conn1",\n                "terminals": [\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*",\n                        "vlan_tag": "1925-1929"\n                    },\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*",\n                        "vlan_tag": "1925-1929"\n                    }\n                ]\n            },\n            {\n                "name": "conn1",\n                "terminals": [\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*",\n                        "vlan_tag": "1925-1929"\n                    },\n                    {\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*",\n                        "vlan_tag": "1925-1929"\n                    }\n                ]\n            }\n        ]\n    }\n}', 'DNC w/ 2 x L2 Paths + Same VLAN Range', 0),
(3, 10, NULL, 'VCN.AWS.1VM_Public.v1', '{\n	"user": "admin",\n	"type": "netcreate",\n	"alias": "VCN.AWS.1VM_Public",\n	"data": {\n		"virtual_clouds": [\n			{\n				"type": "internal",\n				"parent": "urn:ogf:network:aws.amazon.com:aws-cloud",\n				"name": "vpc1",\n				"cidr": "10.0.0.0/16",\n				"subnets": [\n					{\n						"name": "subnet1",\n						"cidr": "10.0.0.0/24",\n						"virtual_machines": [\n							{\n								"name": "vm1",\n								"type": "instance+m4.large,secgroup+geni,keypair+driver_key,image+ami-0d1bf860"\n							}\n						],\n						"routes": [\n							{\n								"to": {\n									"value": "0.0.0.0/0"\n								},\n								"from": {\n									"value": "vpn"\n								},\n								"next_hop": {\n									"value": "vpn"\n								}\n							},\n							{\n								"to": {\n									"value": "0.0.0.0/0",\n									"type": "ipv4-prefix"\n								},\n								"next_hop": {\n									"value": "internet"\n								}\n							}\n						]\n					}\n				],\n				"routes": [\n					{\n						"to": {\n							"value": "0.0.0.0/0",\n							"type": "ipv4-prefix"\n						},\n						"next_hop": {\n							"value": "internet"\n						}\n					}\n				]\n			}\n		]\n	}\n}', 'VCN w/ AWS VTN + 1 VM', 0),
(4, 10, NULL, 'VCN.AWS.1VM.DC_Stitch.v1', '{\n    "user": "admin",\n    "type": "netcreate",\n    "alias": "VCN.AWS.1VM.DC_Stitch",\n    "data": {\n        "virtual_clouds": [\n            {\n                "type": "internal",\n                "parent": "urn:ogf:network:aws.amazon.com:aws-cloud",\n                "name": "vpc1",\n                "cidr": "10.0.0.0/16",\n                "gateways": [\n                    {\n                        "to": [\n                            {\n                                "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*",\n                                "type": "stitch_port"\n                            }\n                        ],\n                        "name": "l2path-aws-dc1",\n                        "type": "aws_direct_connect"\n                    }\n                ],\n                "subnets": [\n                    {\n                        "name": "subnet1",\n                        "cidr": "10.0.0.0/24",\n                        "virtual_machines": [\n                            {\n                                "name": "vm1",\n                                "type": "instance+m4.large,secgroup+geni,keypair+driver_key,image+ami-0d1bf860",\n                                "interfaces": [\n                                    {\n                                        "name": "vm1:eth0",\n                                        "type": "Ethernet",\n                                        "address": "ipv4+52.206.248.139/24"\n                                    }\n                                ]\n                            },\n                            {\n                                "name": "vm2",\n                                "type": "instance+m4.large,secgroup+geni,keypair+driver_key,image+ami-0d1bf860"\n                            }\n                        ],\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0"\n                                },\n                                "from": {\n                                    "value": "vpn"\n                                },\n                                "next_hop": {\n                                    "value": "vpn"\n                                }\n                            },\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0",\n                                    "type": "ipv4-prefix"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ]\n                    },\n                    {\n                        "name": "subnet2",\n                        "cidr": "10.0.1.0/24"\n                    }\n                ],\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ]\n            }\n        ]\n    }\n}', 'VCN w/ AWS VPC + 2 VM + DC + External L2 Stitching', 0),
(5, 10, NULL, 'VCN.OPS.1VM_Public.v1', '{\n    "user": "admin",\n    "type": "netcreate",\n    "alias": "VCN.OPS.1VM_Public",\n    "data": {\n        "virtual_clouds": [\n            {\n                "name": "vtn1",\n                "type": "internal",\n                "parent": "urn:ogf:network:openstack.com:openstack-cloud",\n                "cidr": "10.0.0.0/16",\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ],\n                "subnets": [\n                    {\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0",\n                                    "type": "ipv4-prefix"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ],\n                        "virtual_machines": [\n                            {\n                                "name": "ops-vtn1-vm1",\n                                "type": "instance+5,secgroup+rains,keypair+demo-key",\n                                "host": "any",\n                                "interfaces": [\n                                    {\n                                        "address": "ipv4+any",\n                                        "name": "ops-vtn1:vm1:eth0",\n                                        "type": "Ethernet"\n                                    }\n                                ]\n                            }\n                        ],\n                        "name": "subnet1",\n                        "cidr": "10.0.0.0/24"\n                    }\n                ]\n            }\n        ]\n    }\n}', 'VCN w/ OpenStack VTN + 1 VM ', 0),
(6, 10, NULL, 'VCN.OPS.1VM_SRIOV_Ext.v1', '{\n    "user": "admin",\n    "type": "netcreate",\n    "alias": "VCN.OPS.1VM_SRIOV_Ext",\n    "data": {\n        "virtual_clouds": [\n            {\n                "name": "vtn1",\n                "type": "internal",\n                "parent": "urn:ogf:network:openstack.com:openstack-cloud",\n                "cidr": "10.0.0.0/16",\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ],\n                "gateways": [\n                    {\n                        "from": [\n                            {\n                                "value": "External-Access",\n                                "type": "port_profile"\n                            }\n                        ],\n                        "name": "ext-gw1",\n                        "type": "ucs_port_profile"\n                    }\n                ],\n                "subnets": [\n                    {\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0",\n                                    "type": "ipv4-prefix"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ],\n                        "virtual_machines": [\n                            {\n                                "name": "ops-vtn1-vm1",\n                                "type": "instance+5,secgroup+rains,keypair+demo-key",\n                                "host": "any",\n                                "interfaces": [\n                                    {\n                                        "address": "ipv4+any",\n                                        "name": "ops-vtn1:vm1:eth0",\n                                        "type": "Ethernet"\n                                    },\n                                    {\n                                        "address": "ipv4+206.196.179.157/24,mac+aa:bb:cc:dd:01:57",\n                                        "name": "ops-vtn1:vm1:eth1",\n                                        "type": "SRIOV",\n                                        "gateway": "ext-gw1",\n                                        "routes": [\n                                            {\n                                                "to": {\n                                                    "value": "206.196.0.0/16",\n                                                    "type": "ipv4-prefix"\n                                                },\n                                                "next_hop": {\n                                                    "value": "206.196.179.145"\n                                                }\n                                            }\n                                        ]\n                                    }\n                                ]\n                            }\n                        ],\n                        "name": "subnet1",\n                        "cidr": "10.0.0.0/24"\n                    }\n                ]\n            }\n        ]\n    }\n}', 'VCN w/ OpenStack VTN + 1 VM + 1 SRIOV ', 0),
(7, 13, NULL, 'AHC.AWS_DC.OPS_SRIOV.Ceph_Globus_NFS.v1', '{\n    "username": "admin",\n    "type": "hybridcloud",\n    "alias": "AHC.AWS_DC.OPS_SRIOV.Ceph_Globus_NFS",\n    "data": {\n        "virtual_clouds": [\n            {\n                "type": "internal",\n                "parent": "urn:ogf:network:aws.amazon.com:aws-cloud",\n                "name": "vpc1",\n                "cidr": "10.0.0.0/16",\n                "subnets": [\n                    {\n                        "name": "subnet1",\n                        "cidr": "10.0.0.0/24",\n                        "virtual_machines": [\n                            {\n                                "name": "ec2-vpc1-vm1",\n                                "type": "instance+m4.large,secgroup+geni,keypair+driver_key,image+ami-0d1bf860"\n                            }\n                        ],\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0"\n                                },\n                                "from": {\n                                    "value": "vpn"\n                                },\n                                "next_hop": {\n                                    "value": "vpn"\n                                }\n                            },\n                            {\n                                "to": {\n                                    "value": "206.196.0.0/16"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ]\n                    }\n                ],\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ]\n            },\n            {\n                "name": "vtn2",\n                "type": "internal",\n                "parent": "urn:ogf:network:openstack.com:openstack-cloud",\n                "cidr": "10.1.0.0/16",\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ],\n                "gateways": [\n                    {\n                        "name": "ceph-net",\n                        "from": [\n                            {\n                                "type": "port_profile",\n                                "value": "Ceph-Storage"\n                            }\n                        ],\n                        "type": "ucs_port_profile"\n                    },\n                    {\n                        "name": "intercloud-1",\n                        "to": [\n                            {\n                                "type": "peer_cloud",\n                                "value": "urn:ogf:network:aws.amazon.com:aws-cloud?vlan=any"\n                            }\n                        ],\n                        "type": "inter_cloud_network"\n                    },\n                    {\n                        "name": "ext-net",\n                        "from": [\n                            {\n                                "type": "port_profile",\n                                "value": "External-Access"\n                            }\n                        ],\n                        "type": "ucs_port_profile"\n                    }\n                ],\n                "subnets": [\n                    {\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0",\n                                    "type": "ipv4-prefix"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ],\n                        "virtual_machines": [\n                            {\n                                "name": "ops-vtn1-vm1",\n                                "type": "instance+5,secgroup+rains,keypair+demo-key,image+03555952-e619-4b26-bffd-6b9a62ae15da",\n                                "host": "any",\n                                "interfaces": [\n                                    {\n                                        "address": "ipv4+any",\n                                        "name": "ops-vtn1:vm2:eth0",\n                                        "type": "Ethernet"\n                                    },\n                                    {\n                                        "address": "ipv4+10.10.0.1/24,mac+aa:bb:cc:ff:01:11",\n                                        "name": "ops-vtn1:vm2:eth1",\n                                        "type": "SRIOV",\n                                        "gateway": "intercloud-1"\n                                    },\n                                    {\n                                        "address": "ipv4+10.10.200.164/24,mac+aa:bb:cc:ff:01:12",\n                                        "name": "ops-vtn1:vm2:eth2",\n                                        "type": "SRIOV",\n                                        "gateway": "ceph-net"\n                                    },\n                                    {\n                                        "address": "ipv4+206.196.179.149/28,mac+aa:bb:cc:dd:01:49",\n                                        "name": "ops-vtn1-vm1:eth1",\n                                        "type": "SRIOV",\n                                        "gateway": "ext-net"\n                                    }\n                                ],\n                                "ceph_rbd": [\n                                    {\n                                        "disk_gb": "1024",\n                                        "mount_point": "/mnt/ceph0_1tb"\n                                    },\n                                    {\n                                        "disk_gb": "1024",\n                                        "mount_point": "/mnt/ceph1_1tb"\n                                    }\n                                ],\n                                "globus_connect": {\n                                    "username": "globus_user",\n                                    "password": "globus_pass",\n                                    "short_name": "MAX-SDMZ-EP-X1",\n                                    "data_interface": "206.196.179.157",\n                                    "default_directory": "/mnt/ceph0_1tb"\n                                },\n                                "nfs": {\n                                    "exports": "[\'/mnt/ceph1_1tb 206.196.0.0/16(rw,sync,no_subtree_check)\']"\n                                },\n                                "routes": [\n                                    {\n                                        "to": {\n                                            "value": "10.10.0.0/16",\n                                            "type": "ipv4-prefix"\n                                        },\n                                        "next_hop": {\n                                            "value": "10.1.0.1"\n                                        }\n                                    },\n                                    {\n                                        "to": {\n                                            "value": "0.0.0.0/0",\n                                            "type": "ipv4-prefix"\n                                        },\n                                        "next_hop": {\n                                            "value": "206.196.179.145"\n                                        }\n                                    }\n                                ],\n                                "quagga_bgp": {\n                                    "neighbors": [\n                                        {\n                                            "remote_asn": "7224",\n                                            "bgp_authkey": "versastack"\n                                        }\n                                    ],\n                                    "networks": [\n                                        "10.10.0.0/16"\n                                    ]\n                                }\n                            }\n                        ],\n                        "name": "subnet1",\n                        "cidr": "10.1.0.0/24"\n                    }\n                ]\n            }\n        ]\n    }\n}', 'AHC w/ AWS DC VLAN + OPS SRIOV + BGP + 2 x 1TB Ceph + Globus + NSF', 0);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `acl`
--
ALTER TABLE `acl`
  ADD PRIMARY KEY (`acl_id`);

--
-- Indexes for table `label`
--
ALTER TABLE `label`
  ADD PRIMARY KEY (`identifier`);

--
-- Indexes for table `log`
--
ALTER TABLE `log`
  ADD PRIMARY KEY (`log_id`);

--
-- Indexes for table `service`
--
ALTER TABLE `service`
  ADD PRIMARY KEY (`service_id`);

--
-- Indexes for table `service_delta`
--
ALTER TABLE `service_delta`
  ADD PRIMARY KEY (`service_delta_id`,`service_instance_id`,`service_history_id`),
  ADD KEY `service_delta-service_instance_idx` (`service_instance_id`),
  ADD KEY `service_delta-service_history_idx` (`service_history_id`);

--
-- Indexes for table `service_history`
--
ALTER TABLE `service_history`
  ADD PRIMARY KEY (`service_history_id`,`service_instance_id`),
  ADD KEY `service_history-service_state_idx` (`service_state_id`),
  ADD KEY `service_history-service_instance_idx` (`service_instance_id`);

--
-- Indexes for table `service_instance`
--
ALTER TABLE `service_instance`
  ADD PRIMARY KEY (`service_instance_id`),
  ADD KEY `service_instance-service_idx` (`service_id`),
  ADD KEY `service_instance-user_info_idx` (`username`),
  ADD KEY `service_instance-service_state_idx` (`service_state_id`);

--
-- Indexes for table `service_state`
--
ALTER TABLE `service_state`
  ADD PRIMARY KEY (`service_state_id`),
  ADD UNIQUE KEY `super_state_UNIQUE` (`super_state`);

--
-- Indexes for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD PRIMARY KEY (`service_instance_id`);

--
-- Indexes for table `service_wizard`
--
ALTER TABLE `service_wizard`
  ADD PRIMARY KEY (`service_wizard_id`),
  ADD KEY `service_id` (`service_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `acl`
--
ALTER TABLE `acl`
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=18;
--
-- AUTO_INCREMENT for table `log`
--
ALTER TABLE `log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=27081;
--
-- AUTO_INCREMENT for table `service`
--
ALTER TABLE `service`
  MODIFY `service_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=14;
--
-- AUTO_INCREMENT for table `service_delta`
--
ALTER TABLE `service_delta`
  MODIFY `service_delta_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `service_history`
--
ALTER TABLE `service_history`
  MODIFY `service_history_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=50;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=9;
--
-- AUTO_INCREMENT for table `service_state`
--
ALTER TABLE `service_state`
  MODIFY `service_state_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '	',AUTO_INCREMENT=6;
--
-- AUTO_INCREMENT for table `service_wizard`
--
ALTER TABLE `service_wizard`
  MODIFY `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=11;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `service_delta`
--
ALTER TABLE `service_delta`
  ADD CONSTRAINT `service_delta-service_history` FOREIGN KEY (`service_history_id`) REFERENCES `service_history` (`service_history_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `service_delta-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `service_history`
--
ALTER TABLE `service_history`
  ADD CONSTRAINT `service_history-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE;

--
-- Constraints for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD CONSTRAINT `service_verification-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE;
