locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  })

  availability_zones = slice(data.aws_availability_zones.available.names, 0, var.availability_zone_count)
}
