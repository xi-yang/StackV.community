-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Mar 13, 2018 at 12:30 AM
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
) ENGINE=InnoDB AUTO_INCREMENT=188 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Table structure for table `driver_wizard`
--

DROP TABLE IF EXISTS `driver_wizard`;
CREATE TABLE `driver_wizard` (
  `username` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivername` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivertype` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `TopUri` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `data` longtext COLLATE utf8_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `driver_wizard`
--

INSERT INTO `driver_wizard` (`username`, `drivername`, `drivertype`, `TopUri`, `description`, `data`) VALUES
('admin', 'Generic Rest', 'Generic REST Driver', 'urn:ogf:network:sdn.maxgigapop.net:network', '', '{"jsonData":[{"TOPURI":"urn:ogf:network:sdn.maxgigapop.net:network","subsystemBaseUrl":"http://206.196.179.139:8080/VersaNS-0.0.1-SNAPSHOT"}]}'),
('admin', 'TEST', '', '{"jsonData":[{"TOPURI":"TEST","stubModelTtl":"TEST"}]}', 'TEST', 'Stub System Driver'),
('admin', 'TEST', '', '{"jsonData":[{"stubModelTtl":"sbadgadfas"}]}', '', 'Stub System Driver');

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
) ENGINE=InnoDB AUTO_INCREMENT=325233 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_delta`
--

DROP TABLE IF EXISTS `service_delta`;
CREATE TABLE `service_delta` (
  `service_delta_id` int(11) NOT NULL,
  `service_instance_id` int(11) NOT NULL,
  `super_state` varchar(11) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(60) COLLATE utf8_unicode_ci NOT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `delta` longtext COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_instance`
--

DROP TABLE IF EXISTS `service_instance`;
CREATE TABLE `service_instance` (
  `service_instance_id` int(11) NOT NULL,
  `type` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `creation_time` datetime DEFAULT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `alias_name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `super_state` varchar(11) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_state` varchar(11) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Table structure for table `service_renders`
--

DROP TABLE IF EXISTS `service_renders`;
CREATE TABLE `service_renders` (
  `id` varchar(8) COLLATE utf8_unicode_ci NOT NULL,
  `manifest` mediumtext COLLATE utf8_unicode_ci,
  `package` mediumtext COLLATE utf8_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_verification`
--

DROP TABLE IF EXISTS `service_verification`;
CREATE TABLE `service_verification` (
  `service_instance_id` int(11) NOT NULL,
  `instanceUUID` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `state` varchar(10) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'INIT',
  `pending_action` varchar(45) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `verification_state` int(11) DEFAULT NULL,
  `verification_run` int(11) NOT NULL DEFAULT '0',
  `delta_uuid` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `creation_time` varchar(60) COLLATE utf8_unicode_ci DEFAULT NULL,
  `verified_reduction` longtext COLLATE utf8_unicode_ci,
  `verified_addition` longtext COLLATE utf8_unicode_ci,
  `unverified_reduction` longtext COLLATE utf8_unicode_ci,
  `unverified_addition` longtext COLLATE utf8_unicode_ci,
  `reduction` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `addition` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `timestamp` datetime DEFAULT NULL,
  `elapsed_time` varchar(60) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_wizard`
--

DROP TABLE IF EXISTS `service_wizard`;
CREATE TABLE `service_wizard` (
  `service_wizard_id` int(11) NOT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `owner` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `wizard_json` longtext COLLATE utf8_unicode_ci NOT NULL,
  `editable` tinyint(1) NOT NULL DEFAULT '0',
  `authorized` tinyint(4) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_edited` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service_wizard`
--

INSERT INTO `service_wizard` (`service_wizard_id`, `name`, `description`, `owner`, `wizard_json`, `editable`, `authorized`, `created`, `last_edited`) VALUES
(32, 'TestVCN8-24', '', 'admin', '{\n    "data": {\n        "parent": "urn:ogf:network:sdn.maxgigapop.net:network",\n        "gateways": [\n            {\n                "connects": [\n                    {\n                        "to": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"\n                    }\n                ],\n                "name": "l2path-aws-dc1",\n                "type": "AWS Direct Connect"\n            }\n        ],\n        "cidr": "10.0.0.0/16",\n        "subnets": [\n            {\n"test": "test",\n                "routes": [\n                    {\n                        "next_hop": "vpn",\n                        "from": "vpn",\n                        "to": "0.0.0.0/0"\n                    },\n                    {\n                        "next_hop": "internet",\n                        "to": "0.0.0.0/0"\n                    }\n                ],\n                "vpn_route_propagation": false,\n                "name": "Subnet_1",\n                "cidr": "10.0.0.0/24",\n                "vms": [\n                    {\n                        "image": "ami-0d1bf860",\n                        "interfaces": [\n                            {\n                                "public": false,\n                                "type": "Ethernet"\n                            }\n                        ],\n                        "name": "VM_1",\n                        "security_group": "geni",\n                        "keypair_name": "driver_key",\n                        "instance_type": "m4.large"\n                    }\n                ]\n            }\n        ]\n    },\n    "service": "vcn",\n    "options": [\n        "aws-form"\n    ]\n}', 1, 1, '2018-02-28 05:00:00', '2018-03-07 18:36:27'),
(41, 'TestReorg', 'Test', 'admin', '{"data":{"routes":{"routes":[{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}]},"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"},{"connects":[{"from":"Ceph-Storage"}],"name":"Gateway 1_2","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"5","image":"03555952-e619-4b26-bffd-6b9a62ae15da","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:18:01:33","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.180.133\\/26"},{"mac_address":"aa:bb:cc:20:01:33","name":"SRIOV 1_2","hosting_gateway":"Gateway 1_2","ip_address":"10.10.200.133\\/24"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key","globus_connect":{"password":"max1$fun","data_interface_ip":"206.196.180.133","short_name":"SENSE-UMD-DTN1","default_directory":"\\/mnt\\/ceph1","username":"xiyang"},"ceph_rbds":[{"size":"4000","mount_point":"\\/mnt\\/ceph1"}]}],"internet_routable":true}],"uuid":"f7b66cb4-8f87-450a-96b4-2c84bb155e99"},"service":"vcn"}', 0, 1, '2018-02-27 05:00:00', NULL),
(44, 'New Test', 'Test Description', 'admin', '{"data":{"routes":{"routes":[{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}]},"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"},{"connects":[{"from":"Ceph-Storage"}],"name":"Gateway 1_2","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"5","image":"03555952-e619-4b26-bffd-6b9a62ae15da","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:18:01:33","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.180.133\\/26"},{"mac_address":"aa:bb:cc:20:01:33","name":"SRIOV 1_2","hosting_gateway":"Gateway 1_2","ip_address":"10.10.200.133\\/24"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key","globus_connect":{"password":"max1$fun","data_interface_ip":"206.196.180.133","short_name":"SENSE-UMD-DTN1","default_directory":"\\/mnt\\/ceph1","username":"xiyang"},"ceph_rbds":[{"size":"4000","mount_point":"\\/mnt\\/ceph1"}]}],"internet_routable":true}],"uuid":"f7b66cb4-8f87-450a-96b4-2c84bb155e99"},"service":"vcn"}', 0, 1, '2018-02-28 18:32:44', NULL),
(46, 'test4', '', 'xyang', '{\n    "data": {\n        "parent": "urn:ogf:network:openstack.com:openstack-cloud",\n        "gateways": [\n            {\n                "connects": [\n                    {\n                        "from": "External-Access"\n                    }\n                ],\n                "name": "Gateway 1",\n                "type": "UCS Port Profile"\n            }\n        ],\n        "options": [\n            "openstack-form"\n        ],\n        "cidr": "10.0.0.0/16",\n        "subnets": [\n            {\n                "name": "Subnet 1",\n                "cidr": "10.0.0.0/24",\n                "vms": [\n                    {\n                        "flavor": "4",\n                        "routes": [\n                            {\n                                "next_hop": "10.10.0.0/16",\n                                "to": "10.0.0.1"\n                            },\n                            {\n                                "next_hop": "206.196.179.145",\n                                "to": "0.0.0.0/0"\n                            }\n                        ],\n                        "floating_ip": "any",\n                        "sriovs": [\n                            {\n                                "mac_address": "aa:bb:cc:12:79:51",\n                                "name": "SRIOV 1",\n                                "hosting_gateway": "Gateway 1",\n                                "ip_address": "206.196.179.151/28"\n                            }\n                        ],\n                        "name": "VM 1",\n                        "host": "any",\n                        "security_group": "rains",\n                        "keypair_name": "demo-key"\n                    },\n                    {\n                        "flavor": "5",\n                        "image": "6111384c-7f8a-4113-a6e7-a456fdf1c3b0",\n                        "routes": [\n                            {\n                                "next_hop": "10.10.0.0/16",\n                                "to": "10.0.0.1"\n                            },\n                            {\n                                "next_hop": "206.196.180.129",\n                                "to": "0.0.0.0/0"\n                            }\n                        ],\n                        "floating_ip": "any",\n                        "sriovs": [\n                            {\n                                "mac_address": "aa:bb:cc:12:80:55",\n                                "name": "SRIOV 1_2",\n                                "hosting_gateway": "Gateway 1",\n                                "ip_address": "206.196.180.155/26"\n                            }\n                        ],\n                        "name": "VM 1_2",\n                        "host": "any",\n                        "security_group": "rains",\n                        "keypair_name": "demo-key"\n                    }\n                ],\n                "internet_routable": true\n            }\n        ],\n        "uuid": "d6741f88-0e3a-41e5-8f3e-ca1a0be44600"\n    },\n    "service": "vcn"\n}', 0, 1, '2018-02-28 18:34:26', '2018-03-05 19:17:03'),
(48, 'Share Test', '', 'xyang', '{"data":{"parent":"urn:ogf:network:sdn.maxgigapop.net:network","gateways":[{"connects":[{"to":"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"}],"name":"l2path-aws-dc1","type":"AWS Direct Connect"}],"cidr":"10.0.0.0\\/16","subnets":[{"routes":[{"next_hop":"vpn","from":"vpn","to":"0.0.0.0\\/0"},{"next_hop":"internet","to":"0.0.0.0\\/0"}],"vpn_route_propagation":false,"name":"Subnet_1","cidr":"10.0.0.0\\/24","vms":[{"image":"ami-0d1bf860","interfaces":[{"public":false,"type":"Ethernet"}],"name":"VM_1","security_group":"geni","keypair_name":"driver_key","instance_type":"m4.large"}]}]},"service":"vcn","options":["aws-form"]}', 0, 1, '2018-02-28 20:01:17', NULL),
(49, 'Share Test 2', '', 'xyang', '{"data":{"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"4","routes":[{"next_hop":"10.10.0.0\\/16","to":"10.0.0.1"},{"next_hop":"206.196.179.145","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:79:51","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.179.151\\/28"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key"},{"flavor":"5","image":"6111384c-7f8a-4113-a6e7-a456fdf1c3b0","routes":[{"next_hop":"10.10.0.0\\/16","to":"10.0.0.1"},{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:80:55","name":"SRIOV 1_2","hosting_gateway":"Gateway 1","ip_address":"206.196.180.155\\/26"}],"name":"VM 1_2","host":"any","security_group":"rains","keypair_name":"demo-key"}],"internet_routable":true}],"uuid":"d6741f88-0e3a-41e5-8f3e-ca1a0be44600"},"service":"vcn"}', 0, 0, '2018-03-05 16:35:57', NULL),
(50, 'TestDemo', 'Hello', 'xyang', '{"data":{"parent":"urn:ogf:network:sdn.maxgigapop.net:network","gateways":[{"connects":[{"to":"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"}],"name":"l2path-aws-dc1","type":"AWS Direct Connect"}],"cidr":"10.0.0.0\\/16","subnets":[{"routes":[{"next_hop":"vpn","from":"vpn","to":"0.0.0.0\\/0"},{"next_hop":"internet","to":"0.0.0.0\\/0"}],"vpn_route_propagation":false,"name":"Subnet_1","cidr":"10.0.0.0\\/24","vms":[{"image":"ami-0d1bf860","interfaces":[{"public":false,"type":"Ethernet"}],"name":"VM_1","security_group":"geni","keypair_name":"driver_key","instance_type":"m4.large"}]}]},"service":"vcn","options":["aws-form"]}', 1, 1, '2018-03-07 18:36:56', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `service_wizard_licenses`
--

DROP TABLE IF EXISTS `service_wizard_licenses`;
CREATE TABLE `service_wizard_licenses` (
  `service_wizard_id` int(11) NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `remaining` int(5) NOT NULL DEFAULT '10'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


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
-- Indexes for table `service_delta`
--
ALTER TABLE `service_delta`
  ADD PRIMARY KEY (`service_delta_id`,`service_instance_id`,`super_state`),
  ADD KEY `service_delta-service_instance_idx` (`service_instance_id`),
  ADD KEY `service_delta-service_history_idx` (`super_state`);

--
-- Indexes for table `service_instance`
--
ALTER TABLE `service_instance`
  ADD PRIMARY KEY (`service_instance_id`),
  ADD KEY `service_instance-service_idx` (`type`),
  ADD KEY `service_instance-user_info_idx` (`username`),
  ADD KEY `service_instance-service_state_idx` (`super_state`);

--
-- Indexes for table `service_renders`
--
ALTER TABLE `service_renders`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD PRIMARY KEY (`service_instance_id`);

--
-- Indexes for table `service_wizard`
--
ALTER TABLE `service_wizard`
  ADD PRIMARY KEY (`service_wizard_id`);

--
-- Indexes for table `service_wizard_licenses`
--
ALTER TABLE `service_wizard_licenses`
  ADD PRIMARY KEY (`service_wizard_id`,`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `acl`
--
ALTER TABLE `acl`
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=188;
--
-- AUTO_INCREMENT for table `log`
--
ALTER TABLE `log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=325233;
--
-- AUTO_INCREMENT for table `service_delta`
--
ALTER TABLE `service_delta`
  MODIFY `service_delta_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=38;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=36;
--
-- AUTO_INCREMENT for table `service_wizard`
--
ALTER TABLE `service_wizard`
  MODIFY `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=52;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `service_delta`
--
ALTER TABLE `service_delta`
  ADD CONSTRAINT `service_delta-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD CONSTRAINT `service_verification-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE;

--
-- Constraints for table `service_wizard_licenses`
--
ALTER TABLE `service_wizard_licenses`
  ADD CONSTRAINT `service_wizard_licenses-service_wizard` FOREIGN KEY (`service_wizard_id`) REFERENCES `service_wizard` (`service_wizard_id`) ON DELETE CASCADE;
