-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:3306
-- Generation Time: Dec 04, 2018 at 10:45 PM
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `clipbook`
--

DROP TABLE IF EXISTS `clipbook`;
CREATE TABLE `clipbook` (
  `clipbook_id` int(11) NOT NULL,
  `username` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(256) COLLATE utf8_unicode_ci NOT NULL,
  `clip` longtext COLLATE utf8_unicode_ci NOT NULL,
  `color` varchar(25) COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `driver`
--

DROP TABLE IF EXISTS `driver`;
CREATE TABLE `driver` (
  `urn` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(64) COLLATE utf8_unicode_ci NOT NULL,
  `xml` longtext COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
  `last_state` varchar(11) COLLATE utf8_unicode_ci DEFAULT NULL,
  `intent` longtext COLLATE utf8_unicode_ci NOT NULL,
  `service_wizard_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `service_wizard_licenses`
--

DROP TABLE IF EXISTS `service_wizard_licenses`;
CREATE TABLE `service_wizard_licenses` (
  `service_wizard_id` int(11) NOT NULL,
  `username` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(25) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'ticket',
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
-- Indexes for table `clipbook`
--
ALTER TABLE `clipbook`
  ADD PRIMARY KEY (`clipbook_id`);

--
-- Indexes for table `driver`
--
ALTER TABLE `driver`
  ADD PRIMARY KEY (`urn`),
  ADD UNIQUE KEY `urn` (`urn`),
  ADD KEY `urn_2` (`urn`);

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
  ADD KEY `service_instance-service_state_idx` (`super_state`),
  ADD KEY `service_wizard_id` (`service_wizard_id`);

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
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `clipbook`
--
ALTER TABLE `clipbook`
  MODIFY `clipbook_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `log`
--
ALTER TABLE `log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `service_delta`
--
ALTER TABLE `service_delta`
  MODIFY `service_delta_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `service_instance`
--
ALTER TABLE `service_instance`
  MODIFY `service_instance_id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `service_wizard`
--
ALTER TABLE `service_wizard`
  MODIFY `service_wizard_id` int(11) NOT NULL AUTO_INCREMENT;
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

DELIMITER $$
--
-- Events
--
DROP EVENT `Log cleanup job`$$
CREATE DEFINER=`root`@`localhost` EVENT `Log cleanup job` ON SCHEDULE EVERY 1 DAY STARTS '2019-01-02 00:00:00' ON COMPLETION NOT PRESERVE ENABLE DO DELETE FROM log where referenceUUID NOT IN (SELECT referenceUUID FROM service_instance) AND TIMESTAMPDIFF(DAY, log.timestamp, now()) > 30$$

DELIMITER ;