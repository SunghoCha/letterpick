resource "aws_lb" "api" {
  name               = "${local.name_prefix}-api-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = local.persistence.public_subnet_ids

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-alb"
  })
}

resource "aws_lb_target_group" "api_blue" {
  name        = "${local.name_prefix}-api-blue-tg"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = local.persistence.vpc_id

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
    Name = "${local.name_prefix}-api-blue-tg"
  })
}

resource "aws_lb_target_group" "api_green" {
  name        = "${local.name_prefix}-api-green-tg"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = local.persistence.vpc_id

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
    Name = "${local.name_prefix}-api-green-tg"
  })
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-http-listener"
  })

  depends_on = [
    aws_lb_listener.https,
  ]
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.api.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = aws_acm_certificate_validation.frontend_origin.certificate_arn

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "not found"
      status_code  = "404"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-https-listener"
  })
}

resource "aws_lb_listener_rule" "https_api" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type = "forward"

    forward {
      target_group {
        arn    = aws_lb_target_group.api_blue.arn
        weight = 100
      }

      target_group {
        arn    = aws_lb_target_group.api_green.arn
        weight = 0
      }
    }
  }

  condition {
    path_pattern {
      values = ["/*"]
    }
  }

  lifecycle {
    ignore_changes = [
      action,
    ]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-https-api-rule"
  })
}
