-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Oct 17, 2016 at 08:20 PM
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
  `service_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl`
--

INSERT INTO `acl` (`acl_id`, `service_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(7, 7),
(8, 8),
(9, 9),
(10, 10),
(11, 11),
(12, 12),
(13, 13);

-- --------------------------------------------------------

--
-- Table structure for table `acl_entry_group`
--

DROP TABLE IF EXISTS `acl_entry_group`;
CREATE TABLE `acl_entry_group` (
  `acl_id` int(11) NOT NULL,
  `usergroup_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl_entry_group`
--

INSERT INTO `acl_entry_group` (`acl_id`, `usergroup_id`) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(7, 1),
(10, 1),
(11, 1),
(12, 1),
(13, 1),
(2, 2),
(3, 2),
(11, 2);

-- --------------------------------------------------------

--
-- Table structure for table `acl_entry_user`
--

DROP TABLE IF EXISTS `acl_entry_user`;
CREATE TABLE `acl_entry_user` (
  `acl_id` int(11) NOT NULL,
  `subject` varchar(45) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl_entry_user`
--

INSERT INTO `acl_entry_user` (`acl_id`, `subject`) VALUES
(1, '1'),
(2, '1'),
(4, '1'),
(1, '14'),
(9, '3');

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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service_wizard`
--

INSERT INTO `service_wizard` (`service_wizard_id`, `service_id`, `username`, `name`, `wizard_json`, `description`, `editable`) VALUES
(1, 13, NULL, 'Hybrid Cloud Test', '{\n    "username": "admin",\n    "type": "hybridcloud",\n    "alias": "hybrid-full-1a",\n    "data": {\n        "virtual_clouds": [\n            {\n                "type": "internal",\n                "parent": "urn:ogf:network:aws.amazon.com:aws-cloud",\n                "name": "vtn1",\n                "cidr": "10.0.0.0/16",\n                "subnets": [\n                    {\n                        "name": "subnet1",\n                        "cidr": "10.0.0.0/24",\n                        "virtual_machines": [\n                            {\n                                "name": "ec2-vpc1-vm1",\n                                "type": "instance+m4.large,secgroup+geni,keypair+driver_key,image+ami-0d1bf860"\n                            }\n                        ],\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0"\n                                },\n                                "from": {\n                                    "value": "vpn"\n                                },\n                                "next_hop": {\n                                    "value": "vpn"\n                                }\n                            },\n                            {\n                                "to": {\n                                    "value": "206.196.0.0/16"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ]\n                    }\n                ],\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ]\n            },\n            {\n                "name": "vtn2",\n                "type": "internal",\n                "parent": "urn:ogf:network:openstack.com:openstack-cloud",\n                "cidr": "10.1.0.0/16",\n                "routes": [\n                    {\n                        "to": {\n                            "value": "0.0.0.0/0",\n                            "type": "ipv4-prefix"\n                        },\n                        "next_hop": {\n                            "value": "internet"\n                        }\n                    }\n                ],\n                "gateways": [\n                    {\n                        "name": "ceph-net",\n                        "from": [\n                            {\n                                "type": "port_profile",\n                                "value": "Ceph-Storage"\n                            }\n                        ],\n                        "type": "ucs_port_profile"\n                    },\n                    {\n                        "name": "intercloud-1",\n                        "to": [\n                            {\n                                "type": "peer_cloud",\n                                "value": "urn:ogf:network:aws.amazon.com:aws-cloud?vlan=any"\n                            }\n                        ],\n                        "type": "inter_cloud_network"\n                    }\n                ],\n                "subnets": [\n                    {\n                        "routes": [\n                            {\n                                "to": {\n                                    "value": "0.0.0.0/0",\n                                    "type": "ipv4-prefix"\n                                },\n                                "next_hop": {\n                                    "value": "internet"\n                                }\n                            }\n                        ],\n                        "virtual_machines": [\n                            {\n                                "name": "ops-vtn1-vm1",\n                                "type": "instance+2,secgroup+rains,keypair+demo-key",\n                                "host": "rvtk-compute3",\n                                "interfaces": [\n                                    {\n                                        "address": "ipv4+10.10.252.164/24",\n                                        "name": "ops-vtn1:vm2:eth0",\n                                        "type": "Ethernet"\n                                    },\n                                    {\n                                        "address": "ipv4+10.10.0.1/24,mac+aa:bb:cc:ff:01:11",\n                                        "name": "ops-vtn1:vm2:eth1",\n                                        "type": "SRIOV",\n                                        "gateway": "intercloud-1"\n                                    },\n                                    {\n                                        "address": "ipv4+10.10.200.164/24,mac+aa:bb:cc:ff:01:12",\n                                        "name": "ops-vtn1:vm2:eth2",\n                                        "type": "SRIOV",\n                                        "gateway": "ceph-net"\n                                    }\n                                ],\n                                "ceph_rbd": [\n                                    {\n                                        "disk_gb": "1024",\n                                        "mount_point": "/mnt/ceph0_1tb"\n                                    },\n                                    {\n                                        "disk_gb": "1024",\n                                        "mount_point": "/mnt/ceph1_1tb"\n                                    }\n                                ],\n                                "quagga_bgp": {\n                                    "neighbors": [\n                                        {\n                                            "remote_asn": "7224",\n                                            "bgp_authkey": "versastack"\n                                        }\n                                    ],\n                                    "networks": [\n                                        "10.10.0.0/16"\n                                    ]\n                                }\n                            }\n                        ],\n                        "name": "subnet1",\n                        "cidr": "10.1.0.0/24"\n                    }\n                ]\n            }            \n        ]\n    }\n}\n', 'Test Profile for Hybrid Cloud', 0),
(3, 13, 'admin', 'Demo Test', '{\n	"username": "admin",\n	"type": "hybridcloud",\n	"alias": "TechX2016.AHC.SDX.demo2",\n	"data": {\n		"virtual_clouds": [\n			{\n				"name": "vtn1",\n				"type": "internal",\n				"parent": "urn:ogf:network:openstack.com:openstack-cloud",\n				"cidr": "10.1.0.0/16",\n				"routes": [\n					{\n						"to": {\n							"value": "0.0.0.0/0",\n							"type": "ipv4-prefix"\n						},\n						"next_hop": {\n							"value": "internet"\n						}\n					}\n				],\n				"gateways": [\n					{\n						"name": "ceph-net",\n						"from": [\n							{\n								"type": "port_profile",\n								"value": "Ceph-Storage"\n							}\n						],\n						"type": "ucs_port_profile"\n					}, \n					{\n						"name": "external-net",\n						"from": [\n							{\n								"type": "port_profile",\n								"value": "External-Access"\n							}\n						],\n						"type": "ucs_port_profile"\n					}, \n					{\n						"name": "intercloud-1",\n						"to": [\n							{\n								"type": "peer_cloud",\n								"value": "urn:ogf:network:aws.amazon.com:aws-cloud?vlan=any"\n							}\n						],\n						"type": "inter_cloud_network"\n					} \n				],\n				"subnets": [\n					{\n						"routes": [\n							{\n								"to": {\n									"value": "0.0.0.0/0",\n									"type": "ipv4-prefix"\n								},\n								"next_hop": {\n									"value": "internet"\n								}\n                                                        }\n						],\n						"virtual_machines": [\n							{\n								"name": "ops-vtn1-vm1",\n                                                                "type": "instance+5,secgroup+rains,keypair+demo-key,image+3de656bd-21d5-4c46-89c0-cfdeb7d9c590",\n								"host": "rvtk-compute2",\n								"interfaces": [\n									{\n										"address": "ipv4+10.10.252.202/24",\n										"name": "ops-vtn1:vm1:eth0",\n										"type": "Ethernet"\n									},\n									{\n										"address": "ipv4+10.10.0.1/24,mac+aa:bb:cc:dd:10:01",\n										"name": "ops-vtn1:vm1:eth1",\n										"type": "SRIOV",\n										"gateway": "intercloud-1"\n									},\n									{\n										"address": "ipv4+10.10.200.202/24,mac+aa:bb:cc:dd:02:02",\n										"name": "ops-vtn1:vm1:eth2",\n										"type": "SRIOV",\n										"gateway": "ceph-net"\n									},\n									{\n										"address": "ipv4+206.196.179.157/28,mac+aa:bb:cc:dd:01:57",\n										"name": "ops-vtn1:vm1:eth3",\n										"type": "SRIOV",\n										"gateway": "external-net",\n										"routes": [\n                                                        				{\n                                    				                            "to":  {\n												"type": "ipv4-prefix",\n												"value": "206.196.0.0/16"\n											    },\n                         				                                    "next_hop": {\n												"value": "206.196.179.145"\n											    }\n                                                        				}\n										]\n									}\n								],\n								"quagga_bgp": {\n									"neighbors": [\n										{\n											"remote_asn": "7224",\n											"bgp_authkey": "versastack"\n										}\n									],\n									"networks": [\n										"10.10.0.0/16"\n									]\n								}\n							},\n							{\n								"name": "ops-vtn1-vm2",\n                                                                "type": "instance+5,secgroup+rains,keypair+demo-key,image+3de656bd-21d5-4c46-89c0-cfdeb7d9c590",\n								"host": "rvtk-compute6",\n								"interfaces": [\n									{\n										"address": "ipv4+10.10.252.217/24",\n										"name": "ops-vtn1:vm2:eth0",\n										"type": "Ethernet"\n									},\n									{\n										"address": "ipv4+10.10.0.17/24,mac+aa:bb:cc:dd:10:17",\n										"name": "ops-vtn1:vm2:eth1",\n										"type": "SRIOV",\n										"gateway": "intercloud-1",\n										"routes": [\n                                                        				{\n                                    				                            "to":  {\n												"type": "ipv4-prefix",\n												"value": "10.0.0.0/16"\n											    },\n                         				                                    "next_hop": {\n												"value": "10.10.0.2"\n											    }\n                                                        				}\n										]\n									},\n									{\n										"address": "ipv4+10.10.200.217/24,mac+aa:bb:cc:dd:02:17",\n										"name": "ops-vtn1:vm2:eth2",\n										"type": "SRIOV",\n										"gateway": "ceph-net"\n									}\n								]\n							}, \n							{\n								"name": "ops-vtn1-vm3",\n                                                                "type": "instance+5,secgroup+rains,keypair+demo-key,image+3de656bd-21d5-4c46-89c0-cfdeb7d9c590",\n								"host": "rvtk-compute7",\n								"interfaces": [\n									{\n										"address": "ipv4+10.10.252.219/24",\n										"name": "ops-vtn1:vm3:eth0",\n										"type": "Ethernet"\n									},\n									{\n										"address": "ipv4+10.10.0.19/24,mac+aa:bb:cc:dd:10:19",\n										"name": "ops-vtn1:vm3:eth1",\n										"type": "SRIOV",\n										"gateway": "intercloud-1",\n										"routes": [\n                                                        				{\n                                    				                            "to":  {\n												"type": "ipv4-prefix",\n												"value": "10.0.0.0/16"\n											    },\n                         				                                    "next_hop": {\n												"value": "10.10.0.2"\n											    }\n                                                        				}\n										]\n									},\n									{\n										"address": "ipv4+10.10.200.219/24,mac+aa:bb:cc:dd:02:19",\n										"name": "ops-vtn1:vm3:eth2",\n										"type": "SRIOV",\n										"gateway": "ceph-net"\n									}\n								]\n							}\n						],\n						"name": "subnet1",\n						"cidr": "10.1.0.0/24"\n					}\n				]\n			},\n			{\n				"type": "internal",\n				"parent": "urn:ogf:network:aws.amazon.com:aws-cloud",\n				"name": "vpc1",\n				"cidr": "10.0.0.0/16",\n				"subnets": [\n					{\n						"name": "subnet1",\n						"cidr": "10.0.0.0/24",\n						"virtual_machines": [\n							{\n								"name": "ec2-vpc1-vm1",\n                                                                "type": "instance+m4.xlarge,secgroup+geni,keypair+xi-aws-max-dev-key,image+ami-b66ae0a1"\n							},\n							{\n								"name": "ec2-vpc1-vm2",\n                                                                "type": "instance+m4.xlarge,secgroup+geni,keypair+xi-aws-max-dev-key,image+ami-b66ae0a1"\n							},\n							{\n								"name": "ec2-vpc1-vm3",\n                                                                "type": "instance+m4.xlarge,secgroup+geni,keypair+xi-aws-max-dev-key,image+ami-b66ae0a1"\n							}\n						],\n						"routes": [\n							{\n								"to": {\n									"value": "0.0.0.0/0"\n								},\n								"from": {\n									"value": "vpn"\n								},\n								"next_hop": {\n									"value": "vpn"\n								}\n							},\n							{\n								"to": {\n									"value": "0.0.0.0/0"\n								},\n								"next_hop": {\n									"value": "internet"\n								}\n							}\n						]\n					}\n				],\n				"routes": [\n					{\n						"to": {\n							"value": "0.0.0.0/0",\n							"type": "ipv4-prefix"\n						},\n						"next_hop": {\n							"value": "internet"\n						}\n					}\n				]\n			}\n		]\n	}\n}\n', 'Test for upcoming Demo', 0);

-- --------------------------------------------------------

--
-- Table structure for table `user_belongs`
--

DROP TABLE IF EXISTS `user_belongs`;
CREATE TABLE `user_belongs` (
  `user_id` int(11) NOT NULL,
  `usergroup_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `user_belongs`
--

INSERT INTO `user_belongs` (`user_id`, `usergroup_id`) VALUES
(1, 1),
(7, 1),
(8, 1),
(15, 1),
(1, 2),
(3, 2),
(14, 2);

-- --------------------------------------------------------

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `user_id` int(11) NOT NULL,
  `username` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `email` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `active_usergroup` int(11) DEFAULT '2',
  `first_name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `last_name` varchar(20) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `user_info`
--

INSERT INTO `user_info` (`user_id`, `username`, `email`, `active_usergroup`, `first_name`, `last_name`) VALUES
(1, 'admin', 'neroczan@gmail.com', 1, 'Alberto', 'Jimenez'),
(3, 'test', 'test@test.com', 2, 'Daiko', 'Ten'),
(7, 'test2', 'james@frolick.com', 1, 'James', 'Frolick'),
(8, 'test5', 'jim@jenson.edu', 2, 'Jim', 'Jenson'),
(14, 'test3', 'roger@moore.com', 2, 'Roger', 'Moore'),
(15, 'test4', 'marc@aur.com', 1, 'Marcus', 'Aurelius');

-- --------------------------------------------------------

--
-- Table structure for table `usergroup`
--

DROP TABLE IF EXISTS `usergroup`;
CREATE TABLE `usergroup` (
  `usergroup_id` int(11) NOT NULL,
  `title` varchar(25) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `usergroup`
--

INSERT INTO `usergroup` (`usergroup_id`, `title`) VALUES
(1, 'Administrators'),
(2, 'Users');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `acl`
--
ALTER TABLE `acl`
  ADD PRIMARY KEY (`acl_id`,`service_id`),
  ADD UNIQUE KEY `acl_id` (`acl_id`),
  ADD KEY `acl-service_idx` (`service_id`);

--
-- Indexes for table `acl_entry_group`
--
ALTER TABLE `acl_entry_group`
  ADD PRIMARY KEY (`acl_id`,`usergroup_id`),
  ADD KEY `acl_entry_group-usergroup_idx` (`usergroup_id`);

--
-- Indexes for table `acl_entry_user`
--
ALTER TABLE `acl_entry_user`
  ADD PRIMARY KEY (`acl_id`,`subject`),
  ADD KEY `acl_entry_user-user_info_idx` (`subject`);

--
-- Indexes for table `label`
--
ALTER TABLE `label`
  ADD PRIMARY KEY (`identifier`);

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
-- Indexes for table `user_belongs`
--
ALTER TABLE `user_belongs`
  ADD PRIMARY KEY (`user_id`,`usergroup_id`),
  ADD KEY `user_belongs-usergroup_idx` (`usergroup_id`);

--
-- Indexes for table `user_info`
--
ALTER TABLE `user_info`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `user_info-usergroup_idx` (`active_usergroup`);

--
-- Indexes for table `usergroup`
--
ALTER TABLE `usergroup`
  ADD PRIMARY KEY (`usergroup_id`),
  ADD UNIQUE KEY `group_id` (`usergroup_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `acl`
--
ALTER TABLE `acl`
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=14;
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
  MODIFY `service_history_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `service_state`
--
ALTER TABLE `service_state`
  MODIFY `service_state_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '	',AUTO_INCREMENT=6;
--
-- AUTO_INCREMENT for table `service_wizard`
--
ALTER TABLE `service_wizard`
  MODIFY `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `user_info`
--
ALTER TABLE `user_info`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=16;
--
-- AUTO_INCREMENT for table `usergroup`
--
ALTER TABLE `usergroup`
  MODIFY `usergroup_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=3;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `acl`
--
ALTER TABLE `acl`
  ADD CONSTRAINT `acl-service` FOREIGN KEY (`service_id`) REFERENCES `service` (`service_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `acl_entry_group`
--
ALTER TABLE `acl_entry_group`
  ADD CONSTRAINT `acl_entry_group-acl` FOREIGN KEY (`acl_id`) REFERENCES `acl` (`acl_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `acl_entry_group-usergroup` FOREIGN KEY (`usergroup_id`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `acl_entry_user`
--
ALTER TABLE `acl_entry_user`
  ADD CONSTRAINT `acl_entry_user-acl` FOREIGN KEY (`acl_id`) REFERENCES `acl` (`acl_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

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
  ADD CONSTRAINT `service_history-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `service_history-service_state` FOREIGN KEY (`service_state_id`) REFERENCES `service_state` (`service_state_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `service_instance`
--
ALTER TABLE `service_instance`
  ADD CONSTRAINT `service_instance-service` FOREIGN KEY (`service_id`) REFERENCES `service` (`service_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `service_instance-service_state` FOREIGN KEY (`service_state_id`) REFERENCES `service_state` (`service_state_id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Constraints for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD CONSTRAINT `service_verification-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `service_wizard`
--
ALTER TABLE `service_wizard`
  ADD CONSTRAINT `service_wizard-service` FOREIGN KEY (`service_id`) REFERENCES `service` (`service_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `user_belongs`
--
ALTER TABLE `user_belongs`
  ADD CONSTRAINT `user_belongs-usergroup` FOREIGN KEY (`usergroup_id`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `user_belongs-user_info` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`user_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `user_info`
--
ALTER TABLE `user_info`
  ADD CONSTRAINT `user_info-usergroup` FOREIGN KEY (`active_usergroup`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE NO ACTION ON UPDATE NO ACTION;
--
-- Database: `login`
--
DROP DATABASE `login`;
CREATE DATABASE IF NOT EXISTS `login` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `login`;

-- --------------------------------------------------------

--
-- Table structure for table `cred`
--

DROP TABLE IF EXISTS `cred`;
CREATE TABLE `cred` (
  `username` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
  `password_hash` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `salt` varchar(64) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `cred`
--

INSERT INTO `cred` (`username`, `password_hash`, `salt`) VALUES
('admin', '-974-682329-118-94902114-95-10079105-87-3333121-41-72-14-58-2791-16884139-66-57-2126', 'kXaRVWM1YHHtn9fM'),
('test', '-12710239395522-5473-8885-336922-5353-35-34-104-4541462811961749523-18-27669-58', '89fi5qhjsua60lq6e42oon267jdlk4cmdn460dl2cmpnbsdt90ugp757k41af5ng'),
('test2', '49-81118-81-92-6-5786-19923576-18-1227210071-18-59101-33-504370-10847-77-9-127-105-7-62', 'qdd85hqc3ev0e4ktgjlmf55m6mebsgjhuk3lg7n7tpcqgshpg50p4vnjmn13iil6'),
('test3', '-23115-111-9783-10410828-65458325465119-1149489-11411749-15-42-125329-11994-122-94-58-25', '97hpe70nmh5dcp4nvnrc5lrk2qa3pp568f8n2vjgkc2b74265ga2eh5v9550pe4'),
('test4', '-117-12655-85539997-740-97-9937-703961-112-64-2217368-16119-319423-30-36122-93-13-21', 'aii31mao4uo88travlv6umidd58e4iarvqmadeft50ou8m1ts92973ugupqtrf2v'),
('test5', '-94-706-1254533-21-39-39-755623012-76-53-60960-699-4387-10-5089-19-27-38204127', 'nm9r2qm2rr1guqkhrkk8su3bck8as03oouig5c6gad8ep4b4frhpcq94sf4635jt');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `cred`
--
ALTER TABLE `cred`
  ADD PRIMARY KEY (`username`);
