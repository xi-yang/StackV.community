-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: May 17, 2016 at 07:07 PM
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
(12, 12);

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
(8, 1),
(9, 1),
(10, 1),
(11, 1),
(12, 1),
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
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl_entry_user`
--

INSERT INTO `acl_entry_user` (`acl_id`, `user_id`) VALUES
(1, 1),
(2, 1),
(4, 1),
(9, 3),
(1, 14);

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
('test0', 'admin', 'urn:ogf:network:rains.maxgigapop.net:mira:dtn03.pub.alcf.anl.gov', 'orange'),
('test1', 'admin', 'urn:ogf:network:rains.maxgigapop.net:mira:dtn07.pub.alcf.anl.gov:nic-xeth0.2200', 'purple');

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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service`
--

INSERT INTO `service` (`service_id`, `name`, `filename`, `description`, `atomic`) VALUES
(1, 'User Management', 'usermgt', 'Administrative Management Functions.', 1),
(2, 'Provisioning', 'provision', 'System and Topology Overviews.', 1),
(3, 'Orchestration', 'orchest', 'Manipulation of the System Model.', 1),
(4, 'Monitoring', 'monitor', 'System Monitoring and Logging.', 1),
(7, 'Driver Management', 'driver', 'Installation and Uninstallation of Driver Instances.', 0),
(8, 'Virtual Machine Management', 'vmadd', 'Management, Instantiation, and Setup of Virtual Machine Topologies.', 0),
(9, 'View Filter Management', 'viewcreate', 'Management and Creation of graphical view filters.', 0),
(10, 'Network Creation', 'netcreate', 'Network Creation Pilot Testbed', 0),
(11, 'Dynamic Network Connection', 'dnc', 'Creation of new network connections.', 0),
(12, 'Flow based Layer2 Protection', 'fl2p', 'Switching of protection and recovery path.', 0);

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
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_history`
--

DROP TABLE IF EXISTS `service_history`;
CREATE TABLE `service_history` (
  `service_history_id` int(11) NOT NULL,
  `service_instance_id` int(11) NOT NULL,
  `service_state_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_instance`
--

DROP TABLE IF EXISTS `service_instance`;
CREATE TABLE `service_instance` (
  `service_instance_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `creation_time` datetime DEFAULT NULL,
  `referenceUUID` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `service_state_id` int(11) DEFAULT NULL
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
  ADD PRIMARY KEY (`acl_id`,`user_id`),
  ADD KEY `acl_entry_user-user_info_idx` (`user_id`);

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
  ADD KEY `service_instance-user_info_idx` (`user_id`),
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
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=13;
--
-- AUTO_INCREMENT for table `service`
--
ALTER TABLE `service`
  MODIFY `service_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=13;
--
-- AUTO_INCREMENT for table `service_delta`
--
ALTER TABLE `service_delta`
  MODIFY `service_delta_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=28;
--
-- AUTO_INCREMENT for table `service_history`
--
ALTER TABLE `service_history`
  MODIFY `service_history_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=32;
--
-- AUTO_INCREMENT for table `service_state`
--
ALTER TABLE `service_state`
  MODIFY `service_state_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '	',AUTO_INCREMENT=6;
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
  ADD CONSTRAINT `acl_entry_user-acl` FOREIGN KEY (`acl_id`) REFERENCES `acl` (`acl_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `acl_entry_user-user_info` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`user_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

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
  ADD CONSTRAINT `service_instance-service_state` FOREIGN KEY (`service_state_id`) REFERENCES `service_state` (`service_state_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `service_instance-user_info` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`user_id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Constraints for table `service_verification`
--
ALTER TABLE `service_verification`
  ADD CONSTRAINT `service_verification-service_instance` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`service_instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

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
