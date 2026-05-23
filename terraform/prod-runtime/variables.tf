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
  default = "PriceClass_100"
}

variable "frontend_cloudfront_cache_policy_id" {
  type    = string
  default = "658327ea-f89d-4fab-a63d-7e88639e58f6"
}

variable "initial_backend_image_uri" {
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
