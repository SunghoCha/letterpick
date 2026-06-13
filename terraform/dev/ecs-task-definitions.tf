// 이 파일은 ECS cluster와 초기 API/Worker task definition을 만든다.
// Terraform은 실행 계약과 최초 revision만 만들고, 이후 image 교체는 배포 파이프라인이 맡는다.

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-cluster"
  })
}

locals {
  newsletter_inbox_address_domain = coalesce(var.newsletter_inbox_address_domain, var.ses_receive_domain_name)

  backend_common_environment = {
    SPRING_PROFILES_ACTIVE                               = var.environment
    LETTERPICK_ENV                                       = var.environment
    LETTERPICK_AWS_REGION                                = var.aws_region
    FRONTEND_BASE_URL                                    = var.frontend_base_url
    SPRING_DATASOURCE_URL                                = "jdbc:mysql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.rds_database_name}"
    SPRING_DATA_REDIS_HOST                               = aws_elasticache_replication_group.redis.primary_endpoint_address
    SPRING_DATA_REDIS_PORT                               = tostring(var.redis_port)
    NEWSLETTER_INBOX_ADDRESS_DOMAIN                      = local.newsletter_inbox_address_domain
    LETTERPICK_PUBLIC_FEED_SEARCH_STRATEGY               = var.public_feed_search_strategy
    LETTERPICK_PUBLIC_ISSUE_VIEW_COUNT_SNAPSHOT_INTERVAL = tostring(var.public_issue_view_count_snapshot_interval)
    LETTERPICK_TRENDING_LIFECYCLE_EVENTS_QUEUE           = aws_sqs_queue.trending_lifecycle_events.name
    LETTERPICK_TRENDING_SCORE_EVENTS_QUEUE               = aws_sqs_queue.trending_score_events.name
    GOOGLE_CLIENT_ID                                     = var.google_client_id
    NAVER_CLIENT_ID                                      = var.naver_client_id
  }

  database_secrets = [
    {
      name      = "SPRING_DATASOURCE_USERNAME"
      valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:username::"
    },
    {
      name      = "SPRING_DATASOURCE_PASSWORD"
      valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::"
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
      LETTERPICK_OUTBOX_PUBLISH_ENABLED    = "true"
      }) : {
      name  = name
      value = value
    }
  ]

  worker_environment = [
    for name, value in merge(local.backend_common_environment, {
      LETTERPICK_SQS_ENABLED               = "true"
      LETTERPICK_MAIL_SQS_LISTENER_ENABLED = "true"
      LETTERPICK_MAIL_RECEIVE_QUEUE        = aws_sqs_queue.mail_receive.name
      LETTERPICK_OUTBOX_RETRY_ENABLED      = "true"
      }) : {
      name  = name
      value = value
    }
  ]

  trending_service_environment = [
    for name, value in {
      SPRING_PROFILES_ACTIVE                     = var.environment
      LETTERPICK_ENV                             = var.environment
      LETTERPICK_AWS_REGION                      = var.aws_region
      SPRING_DATASOURCE_URL                      = "jdbc:mysql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.rds_database_name}"
      LETTERPICK_TRENDING_SQS_ENABLED            = "true"
      LETTERPICK_TRENDING_SQS_LISTENER_ENABLED   = "true"
      LETTERPICK_TRENDING_LIFECYCLE_EVENTS_QUEUE = aws_sqs_queue.trending_lifecycle_events.name
      LETTERPICK_TRENDING_SCORE_EVENTS_QUEUE     = aws_sqs_queue.trending_score_events.name
      } : {
      name  = name
      value = value
    }
  ]

  trending_service_observability_environment = var.enable_observability_stack ? [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:/otel/opentelemetry-javaagent.jar"
    },
    {
      name  = "OTEL_SERVICE_NAME"
      value = "trending-service"
    },
    {
      name  = "OTEL_EXPORTER_OTLP_ENDPOINT"
      value = "http://127.0.0.1:4317"
    },
    {
      name  = "OTEL_EXPORTER_OTLP_PROTOCOL"
      value = "grpc"
    },
    {
      name  = "OTEL_TRACES_EXPORTER"
      value = "otlp"
    },
    {
      name  = "OTEL_METRICS_EXPORTER"
      value = "none"
    },
    {
      name  = "OTEL_LOGS_EXPORTER"
      value = "none"
    },
  ] : []

  observability_private_ip = var.enable_observability_stack ? aws_instance.observability[0].private_ip : ""

  trending_service_alloy_config = var.enable_observability_stack ? templatefile("${path.module}/files/trending-service-alloy-config.alloy.tftpl", {
    environment                 = var.environment
    service_name                = "trending-service"
    prometheus_remote_write_url = "http://${local.observability_private_ip}:${var.observability_prometheus_port}/api/v1/write"
    tempo_otlp_grpc_endpoint    = "${local.observability_private_ip}:${var.observability_tempo_otlp_grpc_port}"
  }) : ""

  trending_service_app_container_definition = {
    name        = "trending-service"
    image       = var.initial_trending_service_image_uri
    essential   = true
    stopTimeout = 30

    portMappings = [
      {
        containerPort = var.container_port
        protocol      = "tcp"
      },
    ]

    healthCheck = {
      command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/livez || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 120
    }

    environment = concat(local.trending_service_environment, local.trending_service_observability_environment)
    secrets     = local.database_secrets

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.trending_service.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "trending-service"
      }
    }
  }

  trending_service_alloy_config_container_definitions = [
    for _ in [1] : {
      name      = "alloy-config"
      image     = var.observability_config_writer_image
      essential = false

      entryPoint = ["sh", "-c"]
      command = [
        "printf '%s' \"$ALLOY_CONFIG_BASE64\" | base64 -d > /config/config.alloy && chmod 0444 /config/config.alloy"
      ]

      environment = [
        {
          name  = "ALLOY_CONFIG_BASE64"
          value = base64encode(local.trending_service_alloy_config)
        },
      ]

      mountPoints = [
        {
          sourceVolume  = "trending-service-alloy-config"
          containerPath = "/config"
          readOnly      = false
        },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.trending_service.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "trending-service-alloy-config"
        }
      }
    } if var.enable_observability_stack
  ]

  trending_service_alloy_sidecar_container_definitions = [
    for _ in [1] : {
      name      = "alloy"
      image     = var.observability_alloy_image
      essential = false

      command = [
        "run",
        "--server.http.listen-addr=127.0.0.1:12345",
        "--storage.path=/tmp/alloy",
        "/etc/alloy/config.alloy",
      ]

      dependsOn = [
        {
          containerName = "alloy-config"
          condition     = "SUCCESS"
        },
      ]

      mountPoints = [
        {
          sourceVolume  = "trending-service-alloy-config"
          containerPath = "/etc/alloy"
          readOnly      = true
        },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.trending_service.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "trending-service-alloy"
        }
      }
    } if var.enable_observability_stack
  ]

  trending_service_alloy_container_definitions = concat(
    local.trending_service_alloy_config_container_definitions,
    local.trending_service_alloy_sidecar_container_definitions
  )

  trending_service_container_definitions = concat(
    [local.trending_service_app_container_definition],
    local.trending_service_alloy_container_definitions
  )
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
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/livez || exit 1"]
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
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/livez || exit 1"]
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

  container_definitions = jsonencode(local.trending_service_container_definitions)

  dynamic "volume" {
    for_each = var.enable_observability_stack ? ["trending-service-alloy-config"] : []

    content {
      name = volume.value
    }
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-trending-service-task-definition"
  })
}
