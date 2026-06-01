// GitHub Environment variable에 넣을 값을 출력한다.
output "github_actions_dev_deploy_role_arn" {
  value = aws_iam_role.github_actions_dev_deploy.arn
}

output "github_actions_dev_terraform_role_arn" {
  value = aws_iam_role.github_actions_dev_terraform.arn
}

output "github_actions_prod_deploy_role_arn" {
  value = aws_iam_role.github_actions_prod_deploy.arn
}

output "github_actions_prod_terraform_role_arn" {
  value = aws_iam_role.github_actions_prod_terraform.arn
}

output "performance_dataset_bucket_name" {
  value = aws_s3_bucket.performance_datasets.bucket
}
