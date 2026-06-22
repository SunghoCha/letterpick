resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-cluster"
  })
}

locals {
  newsletter_inbox_address_domain = coalesce(var.newsletter_inbox_address_domain, local.persistence.ses_receive_domain_name)

  backend_common_environment = {
    SPRING_PROFILES_ACTIVE                               = var.environment
    LETTERPICK_AWS_REGION                                = var.aws_region
    FRONTEND_BASE_URL                                    = var.frontend_base_url
    SPRING_DATASOURCE_URL                                = "jdbc:mysql://${local.persistence.rds_address}:${local.persistence.rds_port}/${local.persistence.rds_database_name}"
    SPRING_DATA_REDIS_HOST                               = aws_elasticache_replication_group.redis.primary_endpoint_address
    SPRING_DATA_REDIS_PORT                               = tostring(var.redis_port)
    NEWSLETTER_INBOX_ADDRESS_DOMAIN                      = local.newsletter_inbox_address_domain
    NEWSLETTER_PUBLIC_FEED_COLLECTOR_INBOX_ADDRESS       = var.public_feed_collector_inbox_address
    LETTERPICK_PUBLIC_ISSUE_VIEW_COUNT_SNAPSHOT_INTERVAL = tostring(var.public_issue_view_count_snapshot_interval)
    LETTERPICK_TRENDING_LIFECYCLE_EVENTS_QUEUE           = local.persistence.trending_lifecycle_events_queue_name
    LETTERPICK_TRENDING_SCORE_EVENTS_QUEUE               = local.persistence.trending_score_events_queue_name
    GOOGLE_CLIENT_ID                                     = var.google_client_id
    NAVER_CLIENT_ID                                      = var.naver_client_id
  }

  database_secrets = [
    {
      name      = "SPRING_DATASOURCE_USERNAME"
      valueFrom = "${local.persistence.rds_master_user_secret_arn}:username::"
    },
    {
      name      = "SPRING_DATASOURCE_PASSWORD"
      valueFrom = "${local.persistence.rds_master_user_secret_arn}:password::"
    },
  ]

  application_secrets = [
    for name in sort(keys(var.backend_secret_names)) : {
      name      = name
      valueFrom = "${data.aws_secretsmanager_secret.backend[name].arn}:${name}::"
    }
  ]

  backend_secrets = concat(local.database_secrets, local.application_secrets)

  api_environment = [
    for name, value in merge(local.backend_common_environment, {
      LETTERPICK_SQS_ENABLED               = "true"
      LETTERPICK_MAIL_SQS_LISTENER_ENABLED = "false"
      LETTERPICK_MAIL_RECEIVE_QUEUE        = local.persistence.mail_receive_queue_name
      LETTERPICK_MAIL_RECEIVE_DLQ          = local.persistence.mail_receive_dlq_name
      LETTERPICK_OUTBOX_PUBLISH_ENABLED    = "true"
      LETTERPICK_TRENDING_SERVICE_BASE_URL = local.trending_service_connect_base_url
      }) : {
      name  = name
      value = value
    }
  ]

  worker_environment = [
    for name, value in merge(local.backend_common_environment, {
      LETTERPICK_SQS_ENABLED               = "true"
      LETTERPICK_MAIL_SQS_LISTENER_ENABLED = "true"
      LETTERPICK_MAIL_RECEIVE_QUEUE        = local.persistence.mail_receive_queue_name
      LETTERPICK_TRENDING_SERVICE_BASE_URL = local.trending_service_connect_base_url
      MANAGEMENT_METRICS_TAGS_APPLICATION  = "letterpick-worker"
      MANAGEMENT_METRICS_TAGS_SERVICE      = "letterpick-worker"
      }) : {
      name  = name
      value = value
    }
  ]

  trending_service_environment = [
    for name, value in {
      SPRING_PROFILES_ACTIVE                                     = var.environment
      LETTERPICK_AWS_REGION                                      = var.aws_region
      FRONTEND_BASE_URL                                          = var.frontend_base_url
      SPRING_DATASOURCE_URL                                      = "jdbc:mysql://${local.persistence.rds_address}:${local.persistence.rds_port}/${local.persistence.rds_database_name}"
      SPRING_DATA_REDIS_HOST                                     = aws_elasticache_replication_group.redis.primary_endpoint_address
      SPRING_DATA_REDIS_PORT                                     = tostring(var.redis_port)
      LETTERPICK_TRENDING_SQS_ENABLED                            = "true"
      LETTERPICK_TRENDING_SQS_LISTENER_ENABLED                   = "true"
      LETTERPICK_TRENDING_RANKING_SUMMARY_READER                 = "redis"
      LETTERPICK_TRENDING_RANKING_SUMMARY_WRITER                 = "redis"
      LETTERPICK_TRENDING_SCORE_LISTENER_MAX_CONCURRENT_MESSAGES = "10"
      LETTERPICK_TRENDING_LIFECYCLE_EVENTS_QUEUE                 = local.persistence.trending_lifecycle_events_queue_name
      LETTERPICK_TRENDING_SCORE_EVENTS_QUEUE                     = local.persistence.trending_score_events_queue_name
      MANAGEMENT_METRICS_TAGS_APPLICATION                        = "trending-service"
      MANAGEMENT_METRICS_TAGS_SERVICE                            = "trending-service"
      } : {
      name  = name
      value = value
    }
  ]
}

data "aws_secretsmanager_secret" "backend" {
  for_each = var.backend_secret_names

  name = each.value
}

resource "aws_ecs_task_definition" "api" {
  family                   = "${local.name_prefix}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.api_cpu
  memory                   = var.api_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.api_task.arn

  container_definitions = jsonencode([
    {
      name        = "api"
      image       = var.initial_backend_image_uri
      essential   = true
      stopTimeout = 30

      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        },
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 120
      }

      environment = local.api_environment
      secrets     = local.backend_secrets

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.api.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "api"
        }
      }
    },
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-task-definition"
  })
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "${local.name_prefix}-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.worker_cpu
  memory                   = var.worker_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.worker_task.arn

  container_definitions = jsonencode([
    {
      name        = "worker"
      image       = var.initial_backend_image_uri
      essential   = true
      stopTimeout = 30

      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 120
      }

      environment = local.worker_environment
      secrets     = local.backend_secrets

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.worker.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "worker"
        }
      }
    },
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-worker-task-definition"
  })
}

resource "aws_ecs_task_definition" "trending_service" {
  family                   = "${local.name_prefix}-trending-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.trending_service_cpu
  memory                   = var.trending_service_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.trending_service_task.arn

  container_definitions = jsonencode([
    {
      name        = "trending-service"
      image       = var.initial_trending_service_image_uri
      essential   = true
      stopTimeout = 30

      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
          name          = local.service_connect_port_name
          appProtocol   = "http"
        },
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 120
      }

      environment = local.trending_service_environment
      secrets     = local.backend_secrets

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.trending_service.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "trending-service"
        }
      }
    },
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-trending-service-task-definition"
  })
}
