resource "aws_ecs_service" "api" {
  name            = "${local.name_prefix}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.api_desired_count
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = var.api_health_check_grace_period_seconds

  deployment_configuration {
    strategy             = "BLUE_GREEN"
    bake_time_in_minutes = tostring(var.api_blue_green_bake_time_minutes)
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api_blue.arn
    container_name   = "api"
    container_port   = var.container_port

    advanced_configuration {
      alternate_target_group_arn = aws_lb_target_group.api_green.arn
      production_listener_rule   = aws_lb_listener_rule.https_api.arn
      role_arn                   = aws_iam_role.ecs_load_balancer_infrastructure.arn
    }
  }

  network_configuration {
    subnets          = local.persistence.private_subnet_ids
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
    aws_lb_listener_rule.https_api,
    aws_iam_role_policy_attachment.ecs_load_balancer_infrastructure,
    aws_route.private_nat,
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
    subnets          = local.persistence.private_subnet_ids
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

  depends_on = [
    aws_route.private_nat,
  ]
}
