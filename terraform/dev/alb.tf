// 이 파일은 dev API를 외부에서 확인할 수 있는 ALB 진입점을 만든다.
// ALB는 public subnet에 두고, 실제 API task는 private subnet에 둔다.
// dev에서는 빠른 smoke test가 목적이므로 HTTP listener만 만든다.

resource "aws_lb" "api" {
  name               = "${local.name_prefix}-api-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-alb"
  })
}

resource "aws_lb_target_group" "api" {
  name        = "${local.name_prefix}-api-tg"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  deregistration_delay = var.api_target_group_deregistration_delay_seconds

  health_check {
    enabled             = true
    path                = var.api_health_check_path
    matcher             = "200"
    interval            = var.api_health_check_interval_seconds
    timeout             = var.api_health_check_timeout_seconds
    healthy_threshold   = var.api_health_check_healthy_threshold
    unhealthy_threshold = var.api_health_check_unhealthy_threshold
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-tg"
  })
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-http-listener"
  })
}
