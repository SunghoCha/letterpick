// Redis is a runtime projection store. It is removed when prod-runtime is stopped.

resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = local.persistence.private_subnet_ids

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-redis-subnet-group"
  })
}

resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "Allow Redis access from application tasks"
  vpc_id      = local.persistence.vpc_id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-redis-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_api" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.api_task.id
  description                  = "Allow API task to reach Redis"

  ip_protocol = "tcp"
  from_port   = var.redis_port
  to_port     = var.redis_port
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_worker" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.worker_task.id
  description                  = "Allow worker task to reach Redis"

  ip_protocol = "tcp"
  from_port   = var.redis_port
  to_port     = var.redis_port
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_trending_service" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.trending_service_task.id
  description                  = "Allow trending service task to reach Redis"

  ip_protocol = "tcp"
  from_port   = var.redis_port
  to_port     = var.redis_port
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.db_access_host[0].id
  description                  = "Allow DB access host to inspect Redis"

  ip_protocol = "tcp"
  from_port   = var.redis_port
  to_port     = var.redis_port
}

resource "aws_vpc_security_group_egress_rule" "redis_all_ipv4" {
  security_group_id = aws_security_group.redis.id
  description       = "Allow Redis outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "Redis for LetterPick prod runtime"

  engine               = "redis"
  engine_version       = var.redis_engine_version
  node_type            = var.redis_node_type
  num_cache_clusters   = 1
  port                 = var.redis_port
  parameter_group_name = var.redis_parameter_group_name

  automatic_failover_enabled = false
  multi_az_enabled           = false
  apply_immediately          = true

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false
  snapshot_retention_limit   = 0

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-redis"
  })
}
