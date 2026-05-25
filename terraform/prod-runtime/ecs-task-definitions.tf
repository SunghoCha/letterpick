resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-cluster"
  })
}

locals {
  newsletter_inbox_address_domain = coalesce(var.newsletter_inbox_address_domain, local.persistence.ses_receive_domain_name)

  backend_common_environment = {
    SPRING_PROFILES_ACTIVE          = var.environment
    LETTERPICK_AWS_REGION           = var.aws_region
    FRONTEND_BASE_URL               = var.frontend_base_url
    SPRING_DATASOURCE_URL           = "jdbc:mysql://${local.persistence.rds_address}:${local.persistence.rds_port}/${local.persistence.rds_database_name}"
    NEWSLETTER_INBOX_ADDRESS_DOMAIN = local.newsletter_inbox_address_domain
    GOOGLE_CLIENT_ID                = var.google_client_id
    NAVER_CLIENT_ID                 = var.naver_client_id
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
      LETTERPICK_MAIL_SQS_LISTENER_ENABLED = "false"
      LETTERPICK_MAIL_RECEIVE_QUEUE        = local.persistence.mail_receive_queue_name
      LETTERPICK_MAIL_RECEIVE_DLQ          = local.persistence.mail_receive_dlq_name
      }) : {
      name  = name
      value = value
    }
  ]

  worker_environment = [
    for name, value in merge(local.backend_common_environment, {
      LETTERPICK_MAIL_SQS_LISTENER_ENABLED = "true"
      LETTERPICK_MAIL_RECEIVE_QUEUE        = local.persistence.mail_receive_queue_name
      }) : {
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
