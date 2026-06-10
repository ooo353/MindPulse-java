-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: mindpulse-database
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `notes`
--

DROP TABLE IF EXISTS `notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记唯一标识符',
  `title` varchar(255) NOT NULL COMMENT '笔记标题',
  `content` text COMMENT '笔记正文内容',
  `type` varchar(50) DEFAULT 'text' COMMENT '笔记类型：pdf/image/text等',
  `file_url` varchar(1000) DEFAULT NULL COMMENT '附件文件URL地址',
  `tags` text COMMENT '标签，以逗号分隔',
  `summary` text COMMENT 'AI生成摘要',
  `category` varchar(255) DEFAULT NULL COMMENT 'AI推荐分类',
  `status` varchar(20) NOT NULL DEFAULT 'processing' COMMENT '处理状态',
  `author` varchar(255) NOT NULL COMMENT '笔记创建者用户名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '笔记创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '笔记最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_author` (`author`) COMMENT '按作者查询笔记的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='笔记信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notes`
--

LOCK TABLES `notes` WRITE;
/*!40000 ALTER TABLE `notes` DISABLE KEYS */;
/*!40000 ALTER TABLE `notes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reminders`
--

DROP TABLE IF EXISTS `reminders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属用户',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒内容',
  `remind_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ONCE' COMMENT '提醒类型: ONCE/DAILY/WEEKLY/CUSTOM',
  `remind_time` time DEFAULT NULL COMMENT '提醒时间点 (HH:mm)',
  `remind_date` date DEFAULT NULL COMMENT '一次性提醒日期',
  `day_of_week` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '每周提醒: MON/TUE/WED/THU/FRI/SAT/SUN',
  `cron_expression` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '自定义cron表达式',
  `target_id` bigint DEFAULT NULL COMMENT '关联目标ID',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联目标类型: TASK/NOTE',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_remind_time` (`remind_time`),
  KEY `idx_user_enabled` (`user_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提醒配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reminders`
--

LOCK TABLES `reminders` WRITE;
/*!40000 ALTER TABLE `reminders` DISABLE KEYS */;
/*!40000 ALTER TABLE `reminders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务唯一标识符',
  `title` varchar(255) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务详细描述',
  `due_date` datetime DEFAULT NULL COMMENT '任务截止日期',
  `priority` varchar(20) DEFAULT 'medium' COMMENT '任务优先级：high/medium/low',
  `status` varchar(20) DEFAULT 'pending' COMMENT '任务状态：pending/completed/archived',
  `category` varchar(255) DEFAULT NULL COMMENT '分类标签，逗号分隔',
  `author` varchar(255) NOT NULL COMMENT '任务创建者用户名',
  `related_notes` text COMMENT '关联的笔记ID，以逗号分隔',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_author` (`author`) COMMENT '按作者查询任务的索引',
  KEY `idx_task_due_date` (`due_date`) COMMENT '按截止日期查询任务的索引',
  KEY `idx_task_status` (`status`) COMMENT '按状态查询任务的索引',
  KEY `idx_task_priority` (`priority`) COMMENT '按优先级查询任务的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识符',
  `username` varchar(255) NOT NULL COMMENT '用户名，必须唯一',
  `password` varchar(255) NOT NULL COMMENT '加密后的用户密码',
  `email` varchar(255) DEFAULT NULL COMMENT '用户邮箱地址',
  `role` varchar(50) DEFAULT 'ROLE_USER' COMMENT '用户角色，默认为普通用户',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_user_username` (`username`) COMMENT '按用户名查询用户的索引'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`username`, `password`, `email`, `role`) VALUES
('demo', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'demo@example.com', 'USER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-30 15:18:10

--
-- Table structure for table `pomodoro_sessions`
--

DROP TABLE IF EXISTS `pomodoro_sessions`;
CREATE TABLE `pomodoro_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `duration_minutes` int NOT NULL DEFAULT 25,
  `actual_minutes` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'running',
  `session_type` varchar(20) NOT NULL DEFAULT 'focus',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pomodoro_user` (`user_id`),
  KEY `idx_pomodoro_status` (`status`),
  KEY `idx_pomodoro_start` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit Log Module
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL,
  `action` varchar(50) NOT NULL,
  `resource_type` varchar(50) NOT NULL,
  `resource_id` bigint DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `details` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_resource` (`resource_type`, `resource_id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
