// 이 파일은 dev 성능 실험 중 볼 CloudWatch dashboard를 만든다.
// AWS 기본 메트릭과 API 애플리케이션 내부 지표를 함께 묶는다.

locals {
  performance_dashboard_widgets = concat(
    [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6

        properties = {
          title  = "ALB Target Response Time p95"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "p95"
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.api.arn_suffix],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6

        properties = {
          title  = "ALB Requests and 5xx"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.api.arn_suffix, { stat = "Sum" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", aws_lb.api.arn_suffix, { stat = "Sum" }],
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", aws_lb.api.arn_suffix, { stat = "Sum" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6

        properties = {
          title  = "ECS API CPU / Memory"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.api.name],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.api.name],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6

        properties = {
          title  = "RDS CPU"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.main.identifier],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6

        properties = {
          title  = "RDS Connections"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.main.identifier],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6

        properties = {
          title  = "RDS FreeableMemory"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", aws_db_instance.main.identifier],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 18
        width  = 12
        height = 6

        properties = {
          title  = "RDS IOPS"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", aws_db_instance.main.identifier],
            ["AWS/RDS", "WriteIOPS", "DBInstanceIdentifier", aws_db_instance.main.identifier],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 18
        width  = 12
        height = 6

        properties = {
          title  = "RDS Latency"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "ReadLatency", "DBInstanceIdentifier", aws_db_instance.main.identifier],
            ["AWS/RDS", "WriteLatency", "DBInstanceIdentifier", aws_db_instance.main.identifier],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 24
        width  = 12
        height = 6

        properties = {
          title  = "API JDBC Connections"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [local.application_metrics_namespace, "jdbc.connections.active.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "dataSource", { label = "active" }],
            [local.application_metrics_namespace, "jdbc.connections.idle.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "dataSource", { label = "idle" }],
            [local.application_metrics_namespace, "jdbc.connections.max.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "dataSource", { label = "max" }],
            [local.application_metrics_namespace, "jdbc.connections.min.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "dataSource", { label = "min" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 30
        width  = 12
        height = 6

        properties = {
          title  = "API Tomcat Threads"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [local.application_metrics_namespace, "tomcat.threads.busy.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "http-nio-8080", { label = "busy" }],
            [local.application_metrics_namespace, "tomcat.threads.current.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "http-nio-8080", { label = "current" }],
            [local.application_metrics_namespace, "tomcat.threads.config.max.value", "application", "letterPick", "environment", var.environment, "service", "api", "name", "http-nio-8080", { label = "max" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 24
        width  = 12
        height = 6

        properties = {
          title  = "API JVM Heap Memory"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [local.application_metrics_namespace, "jvm.memory.used.value", "application", "letterPick", "environment", var.environment, "service", "api", "area", "heap", "id", "Eden Space", { label = "eden used" }],
            [local.application_metrics_namespace, "jvm.memory.used.value", "application", "letterPick", "environment", var.environment, "service", "api", "area", "heap", "id", "Tenured Gen", { label = "tenured used" }],
            [local.application_metrics_namespace, "jvm.memory.used.value", "application", "letterPick", "environment", var.environment, "service", "api", "area", "heap", "id", "Survivor Space", { label = "survivor used" }],
          ]
        }
      },
    ],
    var.enable_k6_runner ? [
      {
        type   = "metric"
        x      = 0
        y      = 36
        width  = 12
        height = 6

        properties = {
          title  = "k6 Runner CPU"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.k6_runner[0].id],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 36
        width  = 12
        height = 6

        properties = {
          title  = "k6 Runner Network"
          region = var.aws_region
          view   = "timeSeries"
          period = 60
          stat   = "Sum"
          metrics = [
            ["AWS/EC2", "NetworkIn", "InstanceId", aws_instance.k6_runner[0].id],
            ["AWS/EC2", "NetworkOut", "InstanceId", aws_instance.k6_runner[0].id],
          ]
        }
      },
    ] : []
  )
}

resource "aws_cloudwatch_dashboard" "performance" {
  count = var.enable_perf_observability ? 1 : 0

  dashboard_name = "${local.name_prefix}-performance"

  dashboard_body = jsonencode({
    widgets = local.performance_dashboard_widgets
  })
}
