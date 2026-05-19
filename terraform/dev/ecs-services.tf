// 이 파일은 ECS task definition을 실제로 실행하는 dev ECS service를 만든다.
// API는 ALB target group에 붙이고, Worker는 외부 ingress 없이 SQS를 polling한다.
// task_definition은 최초 생성에만 사용하고 이후 image revision은 배포 파이프라인이 바꿀 수 있게 둔다.

resource "aws_ecs_service" "api" {
  name            = "${local.name_prefix}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.api_desired_count
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = var.api_health_check_grace_period_seconds

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = var.container_port
  }

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.api_task.id]
    assign_public_ip = false
  }

  enable_ecs_managed_tags = true
  propagate_tags          = "SERVICE"

  lifecycle {
    ignore_changes = [
      task_definition,
    ]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-service"
  })

  depends_on = [
    aws_lb_listener.http,
  ]
}

resource "aws_ecs_service" "worker" {
  name            = "${local.name_prefix}-worker"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.worker.arn
  desired_count   = var.worker_desired_count
  launch_type     = "FARGATE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.worker_task.id]
    assign_public_ip = false
  }

  enable_ecs_managed_tags = true
  propagate_tags          = "SERVICE"

  lifecycle {
    ignore_changes = [
      task_definition,
    ]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-worker-service"
  })
}
