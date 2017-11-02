-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Oct 26, 2017 at 07:24 PM
-- Server version: 5.5.42
-- PHP Version: 5.6.7

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `frontend`
--
DROP DATABASE IF EXISTS `frontend`;
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
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `acl`
--

TRUNCATE TABLE `acl`;
-- --------------------------------------------------------

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
-- Truncate table before insert `driver_wizard`
--

TRUNCATE TABLE `driver_wizard`;
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

--
-- Truncate table before insert `label`
--

TRUNCATE TABLE `label`;
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
) ENGINE=InnoDB AUTO_INCREMENT=158543 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `log`
--

TRUNCATE TABLE `log`;
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
) ENGINE=InnoDB AUTO_INCREMENT=136 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `service_delta`
--

TRUNCATE TABLE `service_delta`;
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
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `service_instance`
--

TRUNCATE TABLE `service_instance`;
-- --------------------------------------------------------

--
-- Table structure for table `service_renders`
--

DROP TABLE IF EXISTS `service_renders`;
CREATE TABLE `service_renders` (
  `id` varchar(8) COLLATE utf8_unicode_ci NOT NULL,
  `manifest` mediumtext COLLATE utf8_unicode_ci,
  `package` mediumtext COLLATE utf8_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `service_renders`
--

TRUNCATE TABLE `service_renders`;
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
  `creation_time` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `verified_reduction` longtext COLLATE utf8_unicode_ci,
  `verified_addition` longtext COLLATE utf8_unicode_ci,
  `unverified_reduction` longtext COLLATE utf8_unicode_ci,
  `unverified_addition` longtext COLLATE utf8_unicode_ci,
  `reduction` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `addition` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `timestamp` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `service_verification`
--

TRUNCATE TABLE `service_verification`;
-- --------------------------------------------------------

--
-- Table structure for table `service_wizard`
--

DROP TABLE IF EXISTS `service_wizard`;
CREATE TABLE `service_wizard` (
  `service_wizard_id` int(11) NOT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `wizard_json` longtext COLLATE utf8_unicode_ci NOT NULL,
  `editable` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Truncate table before insert `service_wizard`
--

TRUNCATE TABLE `service_wizard`;
--
-- Dumping data for table `service_wizard`
--

INSERT INTO `service_wizard` (`service_wizard_id`, `name`, `description`, `username`, `wizard_json`, `editable`) VALUES
(35, 'DNCTest', '', 'xyang', '{\n    "data": {\n        "type": "Multi-Path P2P VLAN",\n        "connections": [\n            {\n                "name": "connection_1",\n                "terminals": [\n                    {\n                        "vlan_tag": "1925",\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"\n                    },\n                    {\n                        "vlan_tag": "any",\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*"\n                    }\n                ]\n            },\n            {\n                "name": "connection_2",\n                "terminals": [\n                    {\n                        "vlan_tag": "1926",\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"\n                    },\n                    {\n                        "vlan_tag": "any",\n                        "uri": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*"\n                    }\n                ]\n            }\n        ]\n    },\n    "service": "dnc"\n}', 0),
(39, 'VCNTest-10-17', '', 'xyang', '{"data":{"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"4","routes":[{"next_hop":"10.10.0.0\\/16","to":"10.0.0.1"},{"next_hop":"206.196.179.145","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:79:51","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.179.151\\/28"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key"},{"flavor":"5","image":"6111384c-7f8a-4113-a6e7-a456fdf1c3b0","routes":[{"next_hop":"10.10.0.0\\/16","to":"10.0.0.1"},{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:80:55","name":"SRIOV 1_2","hosting_gateway":"Gateway 1","ip_address":"206.196.180.155\\/26"}],"name":"VM 1_2","host":"any","security_group":"rains","keypair_name":"demo-key"}],"internet_routable":true}],"uuid":"d6741f88-0e3a-41e5-8f3e-ca1a0be44600"},"service":"vcn"}', 0),
(40, 'VCNTest-10-24', '', 'xyang', '{"data":{"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"},{"connects":[{"from":"Ceph-Storage"}],"name":"Gateway 1_2","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"4","image":"d89fdf88-b8e8-4250-846f-733e5e928f92","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"},{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:80:31","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.180.131\\/26"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key"},{"flavor":"ebeb9636-8675-42bb-b099-517505dd67a2","image":"d89fdf88-b8e8-4250-846f-733e5e928f92","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"},{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:12:80:32","name":"SRIOV 1_2","hosting_gateway":"Gateway 1","ip_address":"206.196.180.132\\/26"},{"mac_address":"aa:bb:cc:12:20:32","name":"SRIOV 1_3","hosting_gateway":"Gateway 1_2","ip_address":"10.10.200.132\\/24"}],"name":"VM 1_2","host":"any","security_group":"rains","keypair_name":"demo-key","ceph_rbds":[{"size":"600","mount_point":"\\/mnt\\/ceph1"}]}],"internet_routable":true}],"uuid":"d6741f88-0e3a-41e5-8f3e-ca1a0be44600"},"service":"vcn"}', 0),
(41, 'TestReorg', '', 'xyang', '{"data":{"routes":{"routes":[{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}]},"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"},{"connects":[{"from":"Ceph-Storage"}],"name":"Gateway 1_2","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"5","image":"03555952-e619-4b26-bffd-6b9a62ae15da","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:18:01:33","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.180.133\\/26"},{"mac_address":"aa:bb:cc:20:01:33","name":"SRIOV 1_2","hosting_gateway":"Gateway 1_2","ip_address":"10.10.200.133\\/24"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key","globus_connect":{"password":"max1$fun","data_interface_ip":"206.196.180.133","short_name":"SENSE-UMD-DTN1","default_directory":"\\/mnt\\/ceph1","username":"xiyang"},"ceph_rbds":[{"size":"4000","mount_point":"\\/mnt\\/ceph1"}]}],"internet_routable":true}],"uuid":"f7b66cb4-8f87-450a-96b4-2c84bb155e99"},"service":"vcn"}', 0),
(42, 'TestFailVerify', '', 'xyang', '{"data":{"routes":{"routes":[{"next_hop":"206.196.180.129","to":"0.0.0.0\\/0"}]},"parent":"urn:ogf:network:openstack.com:openstack-cloud","gateways":[{"connects":[{"from":"External-Access"}],"name":"Gateway 1","type":"UCS Port Profile"},{"connects":[{"from":"Ceph-Storage"}],"name":"Gateway 1_2","type":"UCS Port Profile"}],"options":["openstack-form"],"cidr":"10.0.0.0\\/16","subnets":[{"name":"Subnet 1","cidr":"10.0.0.0\\/24","vms":[{"flavor":"5","image":"03555952-e619-4b26-bffd-6b9a62ae15da","routes":[{"next_hop":"10.0.0.1","to":"10.10.0.0\\/16"}],"floating_ip":"any","sriovs":[{"mac_address":"aa:bb:cc:18:01:33","name":"SRIOV 1","hosting_gateway":"Gateway 1","ip_address":"206.196.180.133\\/26"},{"mac_address":"aa:bb:cc:20:01:33","name":"SRIOV 1_2","hosting_gateway":"Gateway 1_2","ip_address":"10.10.200.133\\/24"}],"name":"VM 1","host":"any","security_group":"rains","keypair_name":"demo-key","globus_connect":{"password":"definitelynotfun","data_interface_ip":"206.196.180.133","short_name":"SENSE-UMD-DTN1","default_directory":"\\/mnt\\/ceph1","username":"xiyang"},"ceph_rbds":[{"size":"1000","mount_point":"\\/mnt\\/ceph1"}]}],"internet_routable":true}],"uuid":"f7b66cb4-8f87-450a-96b4-2c84bb155e99"},"service":"vcn"}', 0);

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
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `acl`
--
ALTER TABLE `acl`
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=140;
--
-- AUTO_INCREMENT for table `log`
--
ALTER TABLE `log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=158543;
--
-- AUTO_INCREMENT for table `service_delta`
--
ALTER TABLE `service_delta`
  MODIFY `service_delta_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=136;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=132;
--
-- AUTO_INCREMENT for table `service_wizard`
--
ALTER TABLE `service_wizard`
  MODIFY `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=43;
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
