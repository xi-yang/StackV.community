-- phpMyAdmin SQL Dump
-- version 4.4.1.1
-- http://www.phpmyadmin.net
--
-- Host: localhost:8889
-- Generation Time: Jun 15, 2015 at 04:16 PM
-- Server version: 5.5.42
-- PHP Version: 5.6.7

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `frontend`
--
CREATE DATABASE IF NOT EXISTS `frontend` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `frontend`;

-- --------------------------------------------------------

--
-- Table structure for table `acl`
--

CREATE TABLE `acl` (
  `acl_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `acl`
--

INSERT INTO `acl` (`acl_id`, `service_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 7);

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
(6, 1),
(7, 1),
(2, 2);

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
(1, 3),
(3, 6);

-- --------------------------------------------------------

--
-- Table structure for table `service`
--

CREATE TABLE `service` (
  `service_id` int(11) NOT NULL,
  `name` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(140) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `service`
--

INSERT INTO `service` (`service_id`, `name`, `description`) VALUES
(1, 'User Management', 'Administrative Management Functions.'),
(2, 'Provisioning', 'System and Topology Overviews.'),
(3, 'Orchestration', 'Manipulation of the System Model.'),
(4, 'Example', 'Test.'),
(5, 'Connection', ''),
(6, 'Property Addition', ''),
(7, 'Plug-in Driver', '');

-- --------------------------------------------------------

--
-- Table structure for table `user_info`
--

CREATE TABLE `user_info` (
  `user_id` int(11) NOT NULL,
  `username` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `email` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `usergroup_id` int(11) DEFAULT '2',
  `first_name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `last_name` varchar(20) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `user_info`
--

INSERT INTO `user_info` (`user_id`, `username`, `email`, `usergroup_id`, `first_name`, `last_name`) VALUES
(1, 'admin', 'neroczan@gmail.com', 1, 'Alberto', 'Jimenez'),
(3, 'test', 'test@test.com', 2, 'Daikoku', 'Ten'),
(6, 'test3', 'roger@moore.com', 2, 'Roger', 'Moore'),
(7, 'test2', 'james@frolick.com', 1, 'James', 'Frolick');

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
-- Indexes for table `user_info`
--
ALTER TABLE `user_info`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `user_info-usergroup_idx` (`usergroup_id`);

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
  MODIFY `acl_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=8;
--
-- AUTO_INCREMENT for table `service`
--
ALTER TABLE `service`
  MODIFY `service_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=8;
--
-- AUTO_INCREMENT for table `user_info`
--
ALTER TABLE `user_info`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=8;
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
-- Constraints for table `user_info`
--
ALTER TABLE `user_info`
  ADD CONSTRAINT `user_info-usergroup` FOREIGN KEY (`usergroup_id`) REFERENCES `usergroup` (`usergroup_id`) ON DELETE SET NULL ON UPDATE NO ACTION;
--
-- Database: `login`
--
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
('test2', '108-5035-483-564522-3572-60-539-120-69-1211-25-89103157717-8942-121-15-45108-1761-8', 'gfv6pd666idq41qd28ostqfqs477isdgscc7u73fictinglrrtpbfpkgueb9bvij'),
('test3', '-102-29-103-328-7472-72-9242-63-60115-6979-101-62589770-8212611531-7705417-9631726', 'fk8pu3cfp532qmjrbd20stf89qjcj6snca4jpt1qflk8k40o67f2bccl0ji0srif');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `cred`
--
ALTER TABLE `cred`
  ADD PRIMARY KEY (`username`);
