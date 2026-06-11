// 이 파일은 dev root 바깥에서 바꿀 수 있는 입력값을 정의한다.
// 지금은 root 골격만 있으므로 project/environment/region/tag 같은 공통 입력만 둔다.
// VPC, RDS, ECS를 추가할 때 필요한 입력값은 그 단계에서만 늘린다.
variable "project" {
  type    = string
  default = "letterpick"
}

variable "environment" {
  type    = string
  default = "dev"

  validation {
    condition     = var.environment == "dev"
    error_message = "This Terraform root is only for the dev environment."
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
  default = "10.0.0.0/16"
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
  default = ["10.0.0.0/24", "10.0.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= var.availability_zone_count
    error_message = "public_subnet_cidrs must have at least availability_zone_count entries."
  }
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.10.0/24", "10.0.11.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) >= var.availability_zone_count
    error_message = "private_subnet_cidrs must have at least availability_zone_count entries."
  }
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "api_desired_count" {
  type    = number
  default = 1
}

variable "worker_desired_count" {
  type    = number
  default = 1
}

variable "trending_service_desired_count" {
  type    = number
  default = 1
}

variable "api_health_check_path" {
  type    = string
  default = "/actuator/health/readiness"
}

variable "api_health_check_grace_period_seconds" {
  type    = number
  default = 120
}

variable "api_health_check_interval_seconds" {
  type    = number
  default = 30
}

variable "api_health_check_timeout_seconds" {
  type    = number
  default = 5
}

variable "api_health_check_healthy_threshold" {
  type    = number
  default = 2
}

variable "api_health_check_unhealthy_threshold" {
  type    = number
  default = 3
}

variable "api_target_group_deregistration_delay_seconds" {
  type    = number
  default = 30
}

variable "api_domain_name" {
  type    = string
  default = "dev-api.letterpicknews.com"
}

variable "frontend_domain_name" {
  type    = string
  default = "dev.letterpicknews.com"
}

variable "route53_zone_name" {
  type    = string
  default = "letterpicknews.com"
}

variable "ses_receive_domain_name" {
  type    = string
  default = "inbound-dev.letterpicknews.com"
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
  default = 0
}

variable "rds_deletion_protection" {
  type    = bool
  default = false
}

variable "rds_skip_final_snapshot" {
  type    = bool
  default = true
}

variable "redis_port" {
  type    = number
  default = 6379
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "redis_engine_version" {
  type    = string
  default = "7.1"
}

variable "redis_parameter_group_name" {
  type    = string
  default = "default.redis7"
}

variable "enable_db_access_host" {
  type    = bool
  default = true
}

variable "db_access_instance_type" {
  type    = string
  default = "t4g.nano"
}

variable "db_access_subnet_index" {
  type    = number
  default = 0

  validation {
    condition     = var.db_access_subnet_index >= 0
    error_message = "db_access_subnet_index must be zero or greater."
  }
}

variable "enable_k6_runner" {
  type    = bool
  default = false
}

variable "k6_runner_instance_type" {
  type    = string
  default = "t3.small"
}

variable "k6_runner_subnet_index" {
  type    = number
  default = 0

  validation {
    condition     = var.k6_runner_subnet_index >= 0
    error_message = "k6_runner_subnet_index must be zero or greater."
  }
}

variable "enable_perf_observability" {
  type    = bool
  default = false
}

variable "raw_mail_bucket_name" {
  type    = string
  default = null
}

variable "frontend_bucket_name" {
  type    = string
  default = null
}

variable "frontend_cloudfront_price_class" {
  type    = string
  default = "PriceClass_200"
}

variable "frontend_cloudfront_cache_policy_id" {
  type    = string
  default = "658327ea-f89d-4fab-a63d-7e88639e58f6"
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

variable "initial_backend_image_uri" {
  type = string
}

variable "initial_trending_service_image_uri" {
  type = string
}

variable "public_feed_search_strategy" {
  description = "Public feed search strategy for the backend. fulltext is the default; like remains for fallback and baseline comparison."
  type        = string
  default     = "fulltext"

  validation {
    condition     = contains(["like", "fulltext"], var.public_feed_search_strategy)
    error_message = "public_feed_search_strategy must be like or fulltext."
  }
}

variable "secret_name_prefix" {
  type    = string
  default = null
}

variable "frontend_base_url" {
  type    = string
  default = "https://dev.letterpicknews.com"
}

variable "newsletter_inbox_address_domain" {
  type    = string
  default = null
}

variable "google_client_id" {
  type    = string
  default = "dummy-google-id"
}

variable "naver_client_id" {
  type    = string
  default = "dummy-naver-id"
}

variable "backend_secret_names" {
  description = "Map of ECS environment variable names to Secrets Manager secret names. Each secret must contain a JSON key with the same name as the map key."
  type        = map(string)
  default     = {}
}

variable "api_cpu" {
  type    = number
  default = 512
}

variable "api_memory" {
  type    = number
  default = 1024
}

variable "worker_cpu" {
  type    = number
  default = 512
}

variable "worker_memory" {
  type    = number
  default = 1024
}

variable "trending_service_cpu" {
  type    = number
  default = 512
}

variable "trending_service_memory" {
  type    = number
  default = 1024
}

variable "log_retention_days" {
  type    = number
  default = 14
}
