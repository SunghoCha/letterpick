// GitHub Environment variable에 넣을 값을 출력한다.
output "github_actions_dev_deploy_role_arn" {
  value = aws_iam_role.github_actions_dev_deploy.arn
}
