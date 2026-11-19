-- =====================================================================
-- EVENT BOOKING PLATFORM — SCHÉMA OFFICIEL MYSQL 8
-- Base de données : event_booking
-- Développeur 2 : Responsable Backend et Base de données
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `event_booking`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `event_booking`;

-- 1. Table users (Contrat partagé avec Développeur 1)
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` ENUM('CLIENT', 'ADMIN', 'AGENT') NOT NULL DEFAULT 'CLIENT'
) ENGINE=InnoDB;

-- 2. Table categories
CREATE TABLE IF NOT EXISTS `categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(100) NOT NULL,
    `description` TEXT NULL
) ENGINE=InnoDB;

-- 3. Table events
CREATE TABLE IF NOT EXISTS `events` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `titre` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `date_debut` DATETIME NOT NULL,
    `lieu` VARCHAR(255) NOT NULL,
    `capacite` INT NOT NULL,
    `prix` DECIMAL(10, 2) NOT NULL,
    `statut` ENUM('BROUILLON', 'PUBLIE', 'TERMINE', 'ANNULE') NOT NULL DEFAULT 'BROUILLON',
    `category_id` BIGINT NOT NULL,
    CONSTRAINT `chk_event_capacite` CHECK (`capacite` > 0),
    CONSTRAINT `chk_event_prix` CHECK (`prix` >= 0),
    CONSTRAINT `fk_events_category` FOREIGN KEY (`category_id`)
        REFERENCES `categories` (`id`)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. Table reservations
CREATE TABLE IF NOT EXISTS `reservations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `date_reservation` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `quantite` INT NOT NULL,
    `statut` ENUM('CONFIRMEE', 'ANNULEE') NOT NULL DEFAULT 'CONFIRMEE',
    `user_id` BIGINT NOT NULL,
    `event_id` BIGINT NOT NULL,
    CONSTRAINT `chk_reservation_quantite` CHECK (`quantite` > 0),
    CONSTRAINT `fk_reservations_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT `fk_reservations_event` FOREIGN KEY (`event_id`)
        REFERENCES `events` (`id`)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Index sur les réservations par événement et statut (concurrence & calcul des places)
CREATE INDEX `idx_reservation_event_statut` ON `reservations` (`event_id`, `statut`);

-- 5. Table tickets
CREATE TABLE IF NOT EXISTS `tickets` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code_unique` VARCHAR(100) NOT NULL UNIQUE,
    `statut` ENUM('VALIDE', 'UTILISE', 'ANNULE') NOT NULL DEFAULT 'VALIDE',
    `date_utilisation` DATETIME NULL,
    `reservation_id` BIGINT NOT NULL,
    CONSTRAINT `fk_tickets_reservation` FOREIGN KEY (`reservation_id`)
        REFERENCES `reservations` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;
