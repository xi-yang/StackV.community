ALTER TABLE `service_wizard_licenses` ADD `type` VARCHAR(25) NOT NULL DEFAULT 'ticket' AFTER `username`;

ALTER TABLE `service_instance` ADD `service_wizard_id` INT NULL AFTER `intent`;

