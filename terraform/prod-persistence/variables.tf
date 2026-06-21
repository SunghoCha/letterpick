variable "project" {
  type    = string
  default = "letterpick"
}

variable "environment" {
  type    = string
  default = "prod"

  validation {
    condition     = var.environment == "prod"
    error_message = "This Terraform root is only for the prod environment."
  }
}

variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "tags" {
  type    = map(string)
  default = {}
}

variable "vpc_cidr" {
  type    = string
  default = "10.1.0.0/16"
}

variable "availability_zone_count" {
  type    = number
  default = 2

  validation {
    condition     = var.availability_zone_count >= 2
    error_message = "availability_zone_count must be at least 2."
  }
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.1.0.0/24", "10.1.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= var.availability_zone_count
    error_message = "public_subnet_cidrs must have at least availability_zone_count entries."
  }
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.1.10.0/24", "10.1.11.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) >= var.availability_zone_count
    error_message = "private_subnet_cidrs must have at least availability_zone_count entries."
  }
}

variable "route53_zone_name" {
  type    = string
  default = "letterpicknews.com"
}

variable "ses_receive_domain_name" {
  type    = string
  default = "inbound.letterpicknews.com"
}

variable "ses_receipt_rule_set_name" {
  type    = string
  default = null
}

variable "ses_receipt_rule_name" {
  type    = string
  default = null
}

variable "ses_dns_ttl" {
  type    = number
  default = 300
}

variable "rds_port" {
  type    = number
  default = 3306
}

variable "rds_database_name" {
  type    = string
  default = "letterpick"
}

variable "rds_master_username" {
  type    = string
  default = "letterpick"
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "rds_allocated_storage" {
  type    = number
  default = 20
}

variable "rds_max_allocated_storage" {
  type    = number
  default = 100
}

variable "rds_backup_retention_period" {
  type    = number
  default = 7
}

variable "rds_deletion_protection" {
  type    = bool
  default = false
}

variable "rds_skip_final_snapshot" {
  type    = bool
  default = true
}

variable "raw_mail_bucket_name" {
  type    = string
  default = null
}

variable "raw_mail_object_prefix" {
  type    = string
  default = "raw"
}

variable "mail_received_topic_name" {
  type    = string
  default = null
}

variable "mail_receive_queue_name" {
  type    = string
  default = null
}

variable "mail_receive_dlq_name" {
  type    = string
  default = null
}

variable "mail_receive_max_receive_count" {
  type    = number
  default = 5
}

variable "mail_receive_visibility_timeout_seconds" {
  type    = number
  default = 300
}

variable "mail_receive_message_retention_seconds" {
  type    = number
  default = 1209600
}

variable "trending_lifecycle_events_queue_name" {
  type    = string
  default = null
}

variable "trending_lifecycle_events_dlq_name" {
  type    = string
  default = null
}

variable "trending_score_events_queue_name" {
  type    = string
  default = null
}

variable "trending_score_events_dlq_name" {
  type    = string
  default = null
}

variable "trending_events_max_receive_count" {
  type    = number
  default = 5
}

variable "trending_events_visibility_timeout_seconds" {
  type    = number
  default = 300
}

variable "trending_events_message_retention_seconds" {
  type    = number
  default = 1209600
}

variable "frontend_bucket_name" {
  type    = string
  default = null
}
