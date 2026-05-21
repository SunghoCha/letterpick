// 이 파일은 GitHub Actions deploy-dev workflow가 dev ECS service를 rolling update할 수 있게 한다.
// dev runtime보다 오래 유지되는 배포 기반 role이므로 terraform/dev가 아니라 deploy-base에서 관리한다.

data "aws_caller_identity" "current" {}

data "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"
}

data "aws_iam_policy_document" "github_actions_dev_deploy_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:environment:${var.dev_environment}"]
    }
  }
}

resource "aws_iam_role" "github_actions_dev_deploy" {
  name               = "${local.dev_name_prefix}-github-actions-deploy-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_dev_deploy_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.dev_name_prefix}-github-actions-deploy-role"
  })
}

locals {
  dev_ecs_cluster_arn      = "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:cluster/${local.dev_name_prefix}-cluster"
  dev_frontend_bucket_name = "${local.dev_name_prefix}-frontend-${data.aws_caller_identity.current.account_id}-${var.aws_region}"

  dev_ecs_service_arns = [
    "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${local.dev_name_prefix}-cluster/${local.dev_name_prefix}-api",
    "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${local.dev_name_prefix}-cluster/${local.dev_name_prefix}-worker",
  ]

  dev_ecs_task_role_arns = [
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.dev_name_prefix}-ecs-execution-role",
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.dev_name_prefix}-worker-task-role",
  ]
}

data "aws_iam_policy_document" "github_actions_dev_deploy" {
  statement {
    sid = "ReadDevTerraformState"

    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}",
    ]
  }

  statement {
    sid = "ReadDevTerraformStateObject"

    actions = [
      "s3:GetObject",
    ]

    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}/${var.dev_terraform_state_key}",
    ]
  }

  statement {
    actions = [
      "ecs:DescribeTaskDefinition",
      "ecs:RegisterTaskDefinition",
    ]

    resources = ["*"]
  }

  statement {
    actions = [
      "ecs:DescribeClusters",
    ]

    resources = [
      local.dev_ecs_cluster_arn,
    ]
  }

  statement {
    actions = [
      "ecs:DescribeServices",
      "ecs:UpdateService",
    ]

    resources = local.dev_ecs_service_arns
  }

  statement {
    actions = [
      "iam:PassRole",
    ]

    resources = local.dev_ecs_task_role_arns

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid = "DeployDevFrontendBucketObjects"

    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.dev_frontend_bucket_name}",
    ]
  }

  statement {
    sid = "DeployDevFrontendObjects"

    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${local.dev_frontend_bucket_name}/*",
    ]
  }

  statement {
    sid = "InvalidateDevFrontendCloudFront"

    actions = [
      "cloudfront:CreateInvalidation",
      "cloudfront:GetDistribution",
      "cloudfront:GetInvalidation",
    ]

    resources = [
      "arn:aws:cloudfront::${data.aws_caller_identity.current.account_id}:distribution/*",
    ]
  }
}

resource "aws_iam_role_policy" "github_actions_dev_deploy" {
  name   = "${local.dev_name_prefix}-github-actions-deploy"
  role   = aws_iam_role.github_actions_dev_deploy.id
  policy = data.aws_iam_policy_document.github_actions_dev_deploy.json
}
