-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Aug 12, 2015 at 09:22 PM
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

CREATE TABLE `acl` (
  `acl_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl`
--

INSERT INTO `acl` (`acl_id`, `service_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(7, 7),
(8, 8),
(9, 9);

-- --------------------------------------------------------

--
-- Table structure for table `acl_entry_group`
--

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
(5, 1),
(7, 1),
(8, 1),
(9, 1),
(2, 2),
(3, 2);

-- --------------------------------------------------------

--
-- Table structure for table `acl_entry_user`
--

CREATE TABLE `acl_entry_user` (
  `acl_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl_entry_user`
--

INSERT INTO `acl_entry_user` (`acl_id`, `user_id`) VALUES
(2, 1),
(9, 3),
(5, 8);

-- --------------------------------------------------------

--
-- Table structure for table `service`
--

CREATE TABLE `service` (
  `service_id` int(11) NOT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `filename` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(140) COLLATE utf8_unicode_ci NOT NULL,
  `atomic` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service`
--

INSERT INTO `service` (`service_id`, `name`, `filename`, `description`, `atomic`) VALUES
(1, 'User Management', 'usermgt', 'Administrative Management Functions.', 1),
(2, 'Provisioning', 'provision', 'System and Topology Overviews.', 1),
(3, 'Orchestration', 'orchest', 'Manipulation of the System Model.', 1),
(4, 'Monitoring', 'monitor', 'System Monitoring and Logging.', 1),
(5, 'Example', 'example', 'Test.', 0),
(7, 'Driver Management', 'driver', 'Installation and Uninstallation of Driver Instances.', 0),
(8, 'Virtual Machine Addition', 'vmadd', 'Instantiation and Setup of Virtual Machine Topology.', 0),
(9, 'Orchestration View Creation', 'viewcreate', 'Creation of a new graphical view filter.', 0);

-- --------------------------------------------------------

--
-- Table structure for table `user_belongs`
--

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

CREATE TABLE `user_info` (
  `user_id` int(11) NOT NULL,
  `username` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
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
(3, 'test', 'test@test.com', 2, 'Daikoku', 'Ten'),
(7, 'test2', 'james@frolick.com', 1, 'James', 'Frolick'),
(8, 'test5', 'jim@jenson.edu', 2, 'Jim', 'Jenson'),
(14, 'test3', 'roger@moore.com', 2, 'Roger', 'Moore'),
(15, 'test4', 'marc@aur.com', 1, 'Marcus', 'Aurelius');

-- --------------------------------------------------------

--
-- Table structure for table `usergroup`
--

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
-- Indexes for table `service`
--
ALTER TABLE `service`
  ADD PRIMARY KEY (`service_id`);

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
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=10;
--
-- AUTO_INCREMENT for table `service`
--
ALTER TABLE `service`
  MODIFY `service_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=10;
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
-- Constraints for table `user_belongs`
--
ALTER TABLE `user_belongs`
  ADD CONSTRAINT `user_belongs-usergroup` FOREIGN KEY (`usergroup_id`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `user_belongs-user_info` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`user_id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `user_info`
--
ALTER TABLE `user_info`
  ADD CONSTRAINT `user_info-usergroup` FOREIGN KEY (`active_usergroup`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE SET NULL ON UPDATE NO ACTION;
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
('test', '5110866491678-43680-84-122-656466-76781181-107-63-1117943-2-99-9491611111117127', 'hujepei844nfh079f54gkp489oeafv3juk2s1khemeqqldglvm7smbcnlmku8lmj'),
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
