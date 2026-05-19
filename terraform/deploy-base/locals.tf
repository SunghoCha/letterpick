// deploy-base는 dev root를 직접 참조하지 않으므로 이름 규칙으로 dev 리소스 ARN을 계산한다.
// dev root의 name_prefix 규칙과 맞춰야 deploy-dev workflow가 같은 ECS service를 업데이트할 수 있다.
locals {
  dev_name_prefix = "${var.project}-${var.dev_environment}"

  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = "deploy-base"
    ManagedBy   = "terraform"
  })
}
