// deploy-base는 runtime root를 직접 참조하지 않으므로 이름 규칙으로 dev/prod 리소스 ARN을 계산한다.
// runtime root의 name_prefix 규칙과 맞춰야 GitHub Actions workflow가 같은 ECS service를 업데이트할 수 있다.
locals {
  dev_name_prefix  = "${var.project}-${var.dev_environment}"
  prod_name_prefix = "${var.project}-${var.prod_environment}"

  performance_dataset_bucket_name = coalesce(
    var.performance_dataset_bucket_name,
    "${local.dev_name_prefix}-performance-datasets-${data.aws_caller_identity.current.account_id}-${var.aws_region}"
  )

  prod_terraform_state_keys = [
    var.prod_terraform_state_key,
    var.prod_persistence_terraform_state_key,
    var.prod_runtime_terraform_state_key,
  ]

  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = "deploy-base"
    ManagedBy   = "terraform"
  })
}
