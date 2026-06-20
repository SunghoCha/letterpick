// 이 파일은 dev 환경의 네트워크 접근 경계를 만든다.
// ALB, ECS task, RDS, DB access host를 서로 다른 security group으로 나눠서
// 외부 요청은 ALB까지만 받고, DB는 API/Worker task와 SSM 터널용 host에서만 접근하게 한다.

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "Allow public HTTP traffic to the application load balancer"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alb-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "alb_http_ipv4" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow public HTTP"

  ip_protocol = "tcp"
  from_port   = 80
  to_port     = 80
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "alb_https_ipv4" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow public HTTPS"

  ip_protocol = "tcp"
  from_port   = 443
  to_port     = 443
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "alb_all_ipv4" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow outbound traffic from ALB"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "api_task" {
  name        = "${local.name_prefix}-api-task-sg"
  description = "Allow API task traffic from the ALB"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-task-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "api_task_from_alb" {
  security_group_id            = aws_security_group.api_task.id
  referenced_security_group_id = aws_security_group.alb.id
  description                  = "Allow ALB to reach API task"

  ip_protocol = "tcp"
  from_port   = var.container_port
  to_port     = var.container_port
}

resource "aws_vpc_security_group_egress_rule" "api_task_all_ipv4" {
  security_group_id = aws_security_group.api_task.id
  description       = "Allow API task outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "worker_task" {
  name        = "${local.name_prefix}-worker-task-sg"
  description = "Allow worker task outbound traffic"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-worker-task-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "worker_task_all_ipv4" {
  security_group_id = aws_security_group.worker_task.id
  description       = "Allow worker task outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "trending_service_task" {
  name        = "${local.name_prefix}-trending-service-task-sg"
  description = "Allow trending service task outbound traffic"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-trending-service-task-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "trending_service_task_all_ipv4" {
  security_group_id = aws_security_group.trending_service_task.id
  description       = "Allow trending service task outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "trending_service_task_http_from_k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  security_group_id            = aws_security_group.trending_service_task.id
  referenced_security_group_id = aws_security_group.k6_runner[0].id
  description                  = "Allow k6 runner to reach trending service HTTP"

  ip_protocol = "tcp"
  from_port   = var.container_port
  to_port     = var.container_port
}

resource "aws_vpc_security_group_ingress_rule" "trending_service_task_http_from_api" {
  security_group_id            = aws_security_group.trending_service_task.id
  referenced_security_group_id = aws_security_group.api_task.id
  description                  = "Allow API task to reach trending service HTTP"

  ip_protocol = "tcp"
  from_port   = var.container_port
  to_port     = var.container_port
}

resource "aws_security_group" "db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  name        = "${local.name_prefix}-db-access-host-sg"
  description = "Allow SSM managed host to reach private RDS"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-db-access-host-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "db_access_host_all_ipv4" {
  count = var.enable_db_access_host ? 1 : 0

  security_group_id = aws_security_group.db_access_host[0].id
  description       = "Allow DB access host outbound traffic for SSM and RDS"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  name        = "${local.name_prefix}-k6-runner-sg"
  description = "Allow k6 runner outbound traffic"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-k6-runner-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "k6_runner_all_ipv4" {
  count = var.enable_k6_runner ? 1 : 0

  security_group_id = aws_security_group.k6_runner[0].id
  description       = "Allow k6 runner outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "observability" {
  count = var.enable_observability_stack ? 1 : 0

  name        = "${local.name_prefix}-observability-sg"
  description = "Allow application telemetry traffic to the dev observability stack"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-observability-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "observability_prometheus_from_trending_service" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.trending_service_task.id
  description                  = "Allow trending service metrics remote write"

  ip_protocol = "tcp"
  from_port   = var.observability_prometheus_port
  to_port     = var.observability_prometheus_port
}

resource "aws_vpc_security_group_ingress_rule" "observability_prometheus_from_api" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.api_task.id
  description                  = "Allow API metrics remote write"

  ip_protocol = "tcp"
  from_port   = var.observability_prometheus_port
  to_port     = var.observability_prometheus_port
}

resource "aws_vpc_security_group_ingress_rule" "observability_prometheus_from_worker" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.worker_task.id
  description                  = "Allow worker metrics remote write"

  ip_protocol = "tcp"
  from_port   = var.observability_prometheus_port
  to_port     = var.observability_prometheus_port
}

resource "aws_vpc_security_group_ingress_rule" "observability_tempo_grpc_from_trending_service" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.trending_service_task.id
  description                  = "Allow trending service OTLP traces"

  ip_protocol = "tcp"
  from_port   = var.observability_tempo_otlp_grpc_port
  to_port     = var.observability_tempo_otlp_grpc_port
}

resource "aws_vpc_security_group_ingress_rule" "observability_tempo_grpc_from_api" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.api_task.id
  description                  = "Allow API OTLP traces"

  ip_protocol = "tcp"
  from_port   = var.observability_tempo_otlp_grpc_port
  to_port     = var.observability_tempo_otlp_grpc_port
}

resource "aws_vpc_security_group_ingress_rule" "observability_tempo_grpc_from_worker" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id            = aws_security_group.observability[0].id
  referenced_security_group_id = aws_security_group.worker_task.id
  description                  = "Allow worker OTLP traces"

  ip_protocol = "tcp"
  from_port   = var.observability_tempo_otlp_grpc_port
  to_port     = var.observability_tempo_otlp_grpc_port
}

resource "aws_vpc_security_group_egress_rule" "observability_all_ipv4" {
  count = var.enable_observability_stack ? 1 : 0

  security_group_id = aws_security_group.observability[0].id
  description       = "Allow observability stack outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Allow MySQL access from application tasks"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rds-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "rds_mysql_from_api" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.api_task.id
  description                  = "Allow API task to reach MySQL"

  ip_protocol = "tcp"
  from_port   = var.rds_port
  to_port     = var.rds_port
}

resource "aws_vpc_security_group_ingress_rule" "rds_mysql_from_worker" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.worker_task.id
  description                  = "Allow worker task to reach MySQL"

  ip_protocol = "tcp"
  from_port   = var.rds_port
  to_port     = var.rds_port
}

resource "aws_vpc_security_group_ingress_rule" "rds_mysql_from_trending_service" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.trending_service_task.id
  description                  = "Allow trending service task to reach MySQL"

  ip_protocol = "tcp"
  from_port   = var.rds_port
  to_port     = var.rds_port
}

resource "aws_vpc_security_group_ingress_rule" "rds_mysql_from_k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.k6_runner[0].id
  description                  = "Allow k6 runner to seed MySQL dev data"

  ip_protocol = "tcp"
  from_port   = var.rds_port
  to_port     = var.rds_port
}

resource "aws_vpc_security_group_ingress_rule" "rds_mysql_from_db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.db_access_host[0].id
  description                  = "Allow SSM DB access host to reach MySQL"

  ip_protocol = "tcp"
  from_port   = var.rds_port
  to_port     = var.rds_port
}

resource "aws_vpc_security_group_egress_rule" "rds_all_ipv4" {
  security_group_id = aws_security_group.rds.id
  description       = "Allow RDS outbound traffic"

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}
