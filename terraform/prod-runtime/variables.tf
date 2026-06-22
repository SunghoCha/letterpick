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

variable "frontend_domain_name" {
  type    = string
  default = "letterpicknews.com"
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

variable "api_blue_green_bake_time_minutes" {
  type    = number
  default = 15
}

variable "frontend_cloudfront_price_class" {
  type    = string
  default = "PriceClass_200"
}

variable "frontend_cloudfront_cache_policy_id" {
  type    = string
  default = "658327ea-f89d-4fab-a63d-7e88639e58f6"
}

variable "initial_backend_image_uri" {
  type = string
}

variable "initial_trending_service_image_uri" {
  type = string
}

variable "secret_name_prefix" {
  type    = string
  default = null
}

variable "frontend_base_url" {
  type    = string
  default = "https://letterpicknews.com"
}

variable "newsletter_inbox_address_domain" {
  type    = string
  default = null
}

variable "public_feed_collector_inbox_address" {
  type = string

  validation {
    condition     = trimspace(var.public_feed_collector_inbox_address) != ""
    error_message = "public_feed_collector_inbox_address must not be blank."
  }
}

variable "google_client_id" {
  type = string
}

variable "naver_client_id" {
  type = string
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

variable "public_issue_view_count_snapshot_interval" {
  description = "Snapshot interval for public issue view count events in the prod backend."
  type        = number
  default     = 50

  validation {
    condition     = var.public_issue_view_count_snapshot_interval > 0
    error_message = "public_issue_view_count_snapshot_interval must be greater than zero."
  }
}

variable "enable_observability_stack" {
  type    = bool
  default = false
}

variable "observability_instance_type" {
  type    = string
  default = "t3.small"
}

variable "observability_subnet_index" {
  type    = number
  default = 0

  validation {
    condition     = var.observability_subnet_index >= 0
    error_message = "observability_subnet_index must be zero or greater."
  }
}

variable "observability_grafana_port" {
  type    = number
  default = 3000
}

variable "observability_prometheus_port" {
  type    = number
  default = 9090
}

variable "observability_tempo_otlp_grpc_port" {
  type    = number
  default = 4317
}

variable "observability_tempo_otlp_http_port" {
  type    = number
  default = 4318
}

variable "observability_prometheus_image" {
  type    = string
  default = "prom/prometheus:v2.55.1"
}

variable "observability_tempo_image" {
  type    = string
  default = "grafana/tempo:2.6.1"
}

variable "observability_grafana_image" {
  type    = string
  default = "grafana/grafana:11.5.2"
}

variable "observability_docker_compose_version" {
  type    = string
  default = "2.29.7"
}

variable "observability_alloy_image" {
  type    = string
  default = "grafana/alloy:v1.16.0"
}

variable "observability_config_writer_image" {
  type    = string
  default = "busybox:1.36"
}

variable "log_retention_days" {
  type    = number
  default = 30
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
