ALTER TABLE `service_wizard_licenses` ADD `type` VARCHAR(25) NOT NULL DEFAULT 'ticket' AFTER `username`;

ALTER TABLE `service_instance` ADD `service_wizard_id` INT NULL AFTER `intent`;

DELIMITER $$
--
-- Events
--
DROP EVENT `Log cleanup job`$$
CREATE DEFINER=`root`@`localhost` EVENT `Log cleanup job` ON SCHEDULE EVERY 1 DAY STARTS '2019-01-02 00:00:00' ON COMPLETION NOT PRESERVE ENABLE DO DELETE FROM log where referenceUUID NOT IN (SELECT referenceUUID FROM service_instance) AND TIMESTAMPDIFF(DAY, log.timestamp, now()) > 30$$

DELIMITER ;