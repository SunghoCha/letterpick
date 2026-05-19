// 이 파일은 ECS task가 stdout/stderr를 보낼 CloudWatch log group을 만든다.
// API와 Worker는 운영상 보는 로그가 다르므로 log group도 분리한다.

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${local.name_prefix}/api"
  retention_in_days = var.log_retention_days

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-log-group"
  })
}

resource "aws_cloudwatch_log_group" "worker" {
  name              = "/ecs/${local.name_prefix}/worker"
  retention_in_days = var.log_retention_days

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-worker-log-group"
  })
}
