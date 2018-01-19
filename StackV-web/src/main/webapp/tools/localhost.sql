-- MySQL dump 10.13  Distrib 5.5.27, for osx10.6 (i386)
--
-- Host: localhost    Database: frontend
-- ------------------------------------------------------
-- Server version	5.5.27

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `frontend`
--
DROP DATABASE IF EXISTS `frontend`;
CREATE DATABASE IF NOT EXISTS `frontend` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `frontend`;

--
-- Table structure for table `acl`
--

DROP TABLE IF EXISTS `acl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acl` (
  `acl_id` int(11) NOT NULL AUTO_INCREMENT,
  `subject` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `is_group` tinyint(1) NOT NULL DEFAULT '0',
  `object` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`acl_id`)
) ENGINE=InnoDB AUTO_INCREMENT=146 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acl`
--

LOCK TABLES `acl` WRITE;
/*!40000 ALTER TABLE `acl` DISABLE KEYS */;
/*!40000 ALTER TABLE `acl` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `driver_wizard`
--

DROP TABLE IF EXISTS `driver_wizard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `driver_wizard` (
  `username` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivername` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `drivertype` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `TopUri` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `data` longtext COLLATE utf8_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `driver_wizard`
--

LOCK TABLES `driver_wizard` WRITE;
/*!40000 ALTER TABLE `driver_wizard` DISABLE KEYS */;
INSERT INTO `driver_wizard` VALUES ('admin','Generic Rest','Generic REST Driver','urn:ogf:network:sdn.maxgigapop.net:network','','{\"jsonData\":[{\"TOPURI\":\"urn:ogf:network:sdn.maxgigapop.net:network\",\"subsystemBaseUrl\":\"http://206.196.179.139:8080/VersaNS-0.0.1-SNAPSHOT\"}]}'),('admin','TEST','','{\"jsonData\":[{\"TOPURI\":\"TEST\",\"stubModelTtl\":\"TEST\"}]}','TEST','Stub System Driver'),('admin','TEST','','{\"jsonData\":[{\"stubModelTtl\":\"sbadgadfas\"}]}','','Stub System Driver');
/*!40000 ALTER TABLE `driver_wizard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `label`
--

DROP TABLE IF EXISTS `label`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `label` (
  `identifier` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `label` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `color` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `label`
--

LOCK TABLES `label` WRITE;
/*!40000 ALTER TABLE `label` DISABLE KEYS */;
/*!40000 ALTER TABLE `label` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log` (
  `log_id` int(11) NOT NULL AUTO_INCREMENT,
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
  `exception` longtext COLLATE utf8_unicode_ci,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=159291 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_delta`
--

DROP TABLE IF EXISTS `service_delta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_delta` (
  `service_delta_id` int(11) NOT NULL AUTO_INCREMENT,
  `service_instance_id` int(11) NOT NULL,
  `super_state` varchar(11) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(60) COLLATE utf8_unicode_ci NOT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `delta` longtext COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`service_delta_id`,`service_instance_id`,`super_state`),
  KEY `service_delta-service_instance_idx` (`service_instance_id`),
  KEY `service_delta-service_history_idx` (`super_state`),
  CONSTRAINT `service_delta-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=144 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_delta`
--

LOCK TABLES `service_delta` WRITE;
/*!40000 ALTER TABLE `service_delta` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_delta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_instance`
--

DROP TABLE IF EXISTS `service_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_instance` (
  `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `creation_time` datetime DEFAULT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `alias_name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `super_state` varchar(11) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_state` varchar(11) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`service_instance_id`),
  KEY `service_instance-service_idx` (`type`),
  KEY `service_instance-user_info_idx` (`username`),
  KEY `service_instance-service_state_idx` (`super_state`)
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_instance`
--

LOCK TABLES `service_instance` WRITE;
/*!40000 ALTER TABLE `service_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_renders`
--

DROP TABLE IF EXISTS `service_renders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_renders` (
  `id` varchar(8) COLLATE utf8_unicode_ci NOT NULL,
  `manifest` mediumtext COLLATE utf8_unicode_ci,
  `package` mediumtext COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_renders`
--

LOCK TABLES `service_renders` WRITE;
/*!40000 ALTER TABLE `service_renders` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_renders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_verification`
--

DROP TABLE IF EXISTS `service_verification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
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
  `timestamp` datetime DEFAULT NULL,
  `elapsed_time` varchar(60) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`service_instance_id`),
  CONSTRAINT `service_verification-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_verification`
--

LOCK TABLES `service_verification` WRITE;
/*!40000 ALTER TABLE `service_verification` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_verification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_wizard`
--

DROP TABLE IF EXISTS `service_wizard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_wizard` (
  `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `wizard_json` longtext COLLATE utf8_unicode_ci NOT NULL,
  `editable` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`service_wizard_id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_wizard`
--

LOCK TABLES `service_wizard` WRITE;
/*!40000 ALTER TABLE `service_wizard` DISABLE KEYS */;
 INSERT INTO `service_wizard` VALUES (43,'AHC (Demo)','6xVM+SRIOV+Ceph+Globus+DC','xyang','{\"data\":{\"openstack\":{\"parent\":\"urn:ogf:network:openstack.com:openstack-cloud\",\"gateways\":[{\"name\":\"Gateway 1\",\"type\":\"Intercloud Network\"},{\"connects\":[{\"from\":\"External-Access\"}],\"name\":\"Gateway 1_2\",\"type\":\"UCS Port Profile\"},{\"connects\":[{\"from\":\"Ceph-Storage\"}],\"name\":\"Gateway 1_3\",\"type\":\"UCS Port Profile\"},{\"connects\":[{\"from\":\"LustreRtr\"}],\"name\":\"Gateway 1_4\",\"type\":\"UCS Port Profile\"}],\"cidr\":\"10.1.0.0\\/16\",\"subnets\":[{\"name\":\"OpenStack Subnet 1\",\"cidr\":\"10.1.0.0\\/24\",\"vms\":[{\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.1.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:d1:10:10:01\",\"name\":\"SRIOV 1\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"10.10.0.1\\/24\"},{\"mac_address\":\"aa:bd:d1:18:01:36\",\"name\":\"SRIOV 1_2\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"206.196.180.184\\/26\"},{\"mac_address\":\"aa:bd:d1:20:01:36\",\"name\":\"SRIOV 1_3\",\"hosting_gateway\":\"Gateway 1_3\",\"ip_address\":\"10.10.200.184\\/24\"},{\"mac_address\":\"aa:bd:d1:02:50:06\",\"name\":\"SRIOV 1_4\",\"hosting_gateway\":\"Gateway 1_4\",\"ip_address\":\"10.10.104.10\\/24\"}],\"name\":\"OpenStack VM 1\",\"host\":\"any\",\"keypair_name\":\"demo-key\",\"security_group\":\"rains\",\"globus_connect\":{\"password\":\"secret\",\"public\":true,\"data_interface_ip\":\"206.196.180.184\",\"short_name\":\"UMD-MAX-SDMZ-DTN-1\",\"default_directory\":\"\\/usrs\",\"username\":\"xiyang\"},\"instance_type\":\"ebeb9636-8675-42bb-b099-517505dd67a2\"},{\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.1.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"10.10.0.1\",\"to\":\"10.0.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:d1:10:10:12\",\"name\":\"SRIOV 1_5\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"10.10.0.185\\/24\"},{\"mac_address\":\"aa:bd:d1:18:01:37\",\"name\":\"SRIOV 1_6\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"206.196.180.185\\/26\"},{\"mac_address\":\"aa:bd:d1:20:01:37\",\"name\":\"SRIOV 1_7\",\"hosting_gateway\":\"Gateway 1_3\",\"ip_address\":\"10.10.200.185\\/24\"},{\"mac_address\":\"aa:bd:d1:02:50:22\",\"name\":\"SRIOV 1_8\",\"hosting_gateway\":\"Gateway 1_4\",\"ip_address\":\"10.10.104.11\\/24\"}],\"name\":\"OpenStack VM 1_2\",\"host\":\"any\",\"keypair_name\":\"demo-key\",\"security_group\":\"rains\",\"globus_connect\":{\"password\":\"secret\",\"public\":true,\"data_interface_ip\":\"206.196.180.185\",\"short_name\":\"UMD-MAX-SDMZ-DTN-2\",\"default_directory\":\"\\/usrs\",\"username\":\"xiyang\"},\"instance_type\":\"ebeb9636-8675-42bb-b099-517505dd67a2\"},{\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.1.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"10.10.0.1\",\"to\":\"10.0.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:d1:10:10:13\",\"name\":\"SRIOV 1_9\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"10.10.0.186\\/24\"},{\"mac_address\":\"aa:bd:d1:18:01:38\",\"name\":\"SRIOV 1_10\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"206.196.180.186\\/26\"},{\"mac_address\":\"aa:bd:d1:20:01:38\",\"name\":\"SRIOV 1_11\",\"hosting_gateway\":\"Gateway 1_3\",\"ip_address\":\"10.10.200.186\\/24\"},{\"mac_address\":\"aa:bd:d1:02:50:38\",\"name\":\"SRIOV 1_12\",\"hosting_gateway\":\"Gateway 1_4\",\"ip_address\":\"10.10.104.12\\/24\"}],\"name\":\"OpenStack VM 1_3\",\"host\":\"any\",\"keypair_name\":\"demo-key\",\"security_group\":\"rains\",\"globus_connect\":{\"password\":\"secret\",\"public\":true,\"data_interface_ip\":\"206.196.180.186\",\"short_name\":\"UMD-MAX-SDMZ-DTN-3\",\"default_directory\":\"\\/usrs\",\"username\":\"xiyang\"},\"instance_type\":\"ebeb9636-8675-42bb-b099-517505dd67a2\"},{\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.1.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"10.10.0.1\",\"to\":\"10.0.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:d1:10:10:14\",\"name\":\"SRIOV 1_13\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"10.10.0.187\\/24\"},{\"mac_address\":\"aa:bd:d1:18:01:39\",\"name\":\"SRIOV 1_14\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"206.196.180.187\\/26\"},{\"mac_address\":\"aa:bd:d1:20:01:39\",\"name\":\"SRIOV 1_15\",\"hosting_gateway\":\"Gateway 1_3\",\"ip_address\":\"10.10.200.187\\/24\"},{\"mac_address\":\"aa:bd:d1:02:50:86\",\"name\":\"SRIOV 1_16\",\"hosting_gateway\":\"Gateway 1_4\",\"ip_address\":\"10.10.104.13\\/24\"}],\"name\":\"OpenStack VM 1_4\",\"host\":\"any\",\"keypair_name\":\"demo-key\",\"security_group\":\"rains\",\"globus_connect\":{\"password\":\"secret\",\"public\":true,\"data_interface_ip\":\"206.196.180.187\",\"short_name\":\"UMD-MAX-SDMZ-DTN-4\",\"default_directory\":\"\\/usrs\",\"username\":\"xiyang\"},\"instance_type\":\"ebeb9636-8675-42bb-b099-517505dd67a2\"},{\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.1.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"10.10.0.1\",\"to\":\"10.0.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:d1:10:10:15\",\"name\":\"SRIOV 1_17\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"10.10.0.188\\/24\"},{\"mac_address\":\"aa:bd:d1:30:01:40\",\"name\":\"SRIOV 1_18\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"206.196.180.188\\/26\"},{\"mac_address\":\"aa:bd:d1:20:01:40\",\"name\":\"SRIOV 1_19\",\"hosting_gateway\":\"Gateway 1_3\",\"ip_address\":\"10.10.200.188\\/24\"},{\"mac_address\":\"aa:bd:d1:02:51:02\",\"name\":\"SRIOV 1_20\",\"hosting_gateway\":\"Gateway 1_4\",\"ip_address\":\"10.10.104.14\\/24\"}],\"name\":\"OpenStack VM 1_5\",\"host\":\"any\",\"keypair_name\":\"demo-key\",\"security_group\":\"rains\",\"globus_connect\":{\"password\":\"secret\",\"public\":true,\"data_interface_ip\":\"206.196.180.188\",\"short_name\":\"UMD-MAX-SDMZ-DTN-5\",\"default_directory\":\"\\/usrs\",\"username\":\"xiyang\"},\"instance_type\":\"ebeb9636-8675-42bb-b099-517505dd67a2\"}],\"internet_routable\":true}]},\"intercloud\":{\"bgp\":{\"authentication_key\":\"secret\",\"amazon_asn\":\"7224\",\"vm_host\":\"OpenStack VM 1\",\"networks\":\"10.10.0.0\\/16\"}},\"aws\":{\"parent\":\"urn:ogf:network:aws.amazon.com:aws-cloud\",\"cidr\":\"10.0.0.0\\/16\",\"direct_connect_vlan\":\"any\",\"subnets\":[{\"vpn_route_propagation\":true,\"name\":\"AWS Subnet 1\",\"cidr\":\"10.0.0.0\\/24\",\"vms\":[{\"image\":\"ami-6d64487a\",\"interfaces\":[{\"public\":true,\"type\":\"Ethernet\",\"elastic_ip\":\"52.206.248.139\"}],\"name\":\"AWS VM 1\",\"instance_type\":\"m4.10xlarge\"}],\"internet_routable\":true}]},\"uuid\":\"974dcc70-a321-45f3-b3ab-2d8aced4dfdd\"},\"service\":\"ahc\"}',0),(44,'VCN-AWS (Demo)','1 VPC 1 VM w/ ElasticIP','xyang','{\"data\":{\"parent\":\"urn:ogf:network:aws.amazon.com:aws-cloud\",\"options\":[\"aws-form\"],\"cidr\":\"10.0.0.0\\/16\",\"subnets\":[{\"vpn_route_propagation\":true,\"name\":\"Subnet 1\",\"cidr\":\"10.0.0.0\\/24\",\"vms\":[{\"interfaces\":[{\"public\":true,\"type\":\"Ethernet\",\"elastic_ip\":\"52.71.182.172\"}],\"name\":\"VM 1\"}],\"internet_routable\":true}],\"uuid\":\"92d4b9f2-a8a5-4586-b8c9-993f64b74d4a\"},\"service\":\"vcn\"}',0),(45,'VCN-OpenStack (Demo)','2xVM SRIOV+Ceph_RDB','xyang','{\"data\":{\"parent\":\"urn:ogf:network:openstack.com:openstack-cloud\",\"gateways\":[{\"connects\":[{\"from\":\"External-Access\"}],\"name\":\"Gateway 1\",\"type\":\"UCS Port Profile\"},{\"connects\":[{\"from\":\"Ceph-Storage\"}],\"name\":\"Gateway 1_2\",\"type\":\"UCS Port Profile\"}],\"options\":[\"openstack-form\"],\"cidr\":\"10.0.0.0\\/16\",\"subnets\":[{\"name\":\"Subnet 1\",\"cidr\":\"10.0.0.0\\/24\",\"vms\":[{\"flavor\":\"4\",\"routes\":[{\"next_hop\":\"10.0.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:cc:12:80:31\",\"name\":\"SRIOV 1\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"206.196.180.131\\/26\"}],\"name\":\"VM 1\",\"host\":\"any\",\"security_group\":\"rains\",\"keypair_name\":\"demo-key\"},{\"flavor\":\"ebeb9636-8675-42bb-b099-517505dd67a2\",\"image\":\"18306614-6f3b-438e-a5ad-f9f9a31d6254\",\"routes\":[{\"next_hop\":\"10.0.0.1\",\"to\":\"10.10.0.0\\/16\"},{\"next_hop\":\"206.196.180.129\",\"to\":\"0.0.0.0\\/0\"}],\"floating_ip\":\"any\",\"sriovs\":[{\"mac_address\":\"aa:bb:cc:12:80:32\",\"name\":\"SRIOV 1_2\",\"hosting_gateway\":\"Gateway 1\",\"ip_address\":\"206.196.180.132\\/26\"},{\"mac_address\":\"aa:bb:cc:12:20:32\",\"name\":\"SRIOV 1_3\",\"hosting_gateway\":\"Gateway 1_2\",\"ip_address\":\"10.10.200.132\\/24\"}],\"name\":\"VM 1_2\",\"host\":\"any\",\"security_group\":\"rains\",\"keypair_name\":\"demo-key\",\"ceph_rbds\":[{\"size\":\"600\",\"mount_point\":\"\\/mnt\\/ceph1\"}]}],\"internet_routable\":true}],\"uuid\":\"d6741f88-0e3a-41e5-8f3e-ca1a0be44600\"},\"service\":\"vcn\"}',0),(46,'DNC Single-Conn (Demo)','Best Effort - and VLAN','xyang','{\n    \"data\": {\n        \"type\": \"Multi-Path P2P VLAN\",\n        \"uuid\": \"d2f96199-5538-4e36-82e7-d9f479b55325\",\n        \"connections\": [\n            {\n                \"bandwidth\": {\n                    \"qos_class\": \"bestEffort\"\n                },\n                \"name\": \"connection 1\",\n                \"terminals\": [\n                    {\n                        \"vlan_tag\": \"any\",\n                        \"assign_ip\": false,\n                        \"uri\": \"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*\"\n                    },\n                    {\n                        \"vlan_tag\": \"any\",\n                        \"assign_ip\": false,\n                        \"uri\": \"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*\"\n                    }\n                ]\n            }\n        ]\n    },\n    \"service\": \"dnc\"\n}',0),(47,'DNC Multi-Conns (Demo)','1 BestEffort + 1G Guaranteed - any VLANs  ','xyang','{\"data\":{\"type\":\"Multi-Path P2P VLAN\",\"uuid\":\"d2422c98-7423-4bd8-bdac-5576b14dde16\",\"connections\":[{\"bandwidth\":{\"qos_class\":\"guaranteedCapped\",\"capacity\":\"1000\"},\"name\":\"connection 1\",\"terminals\":[{\"vlan_tag\":\"any\",\"assign_ip\":false,\"uri\":\"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*\"},{\"vlan_tag\":\"any\",\"assign_ip\":false,\"uri\":\"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*\"}]},{\"bandwidth\":{\"qos_class\":\"bestEffort\"},\"name\":\"connection 1_2\",\"terminals\":[{\"vlan_tag\":\"any\",\"assign_ip\":false,\"uri\":\"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*\"},{\"vlan_tag\":\"any\",\"assign_ip\":false,\"uri\":\"urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*\"}]}]},\"service\":\"dnc\"}',0);
/*!40000 ALTER TABLE `service_wizard` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-01-14 11:24:50
