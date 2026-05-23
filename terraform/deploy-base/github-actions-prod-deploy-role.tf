// 이 파일은 GitHub Actions deploy-prod workflow가 prod ECS service를 배포할 수 있게 한다.
// prod runtime을 다시 만들어도 배포 role은 유지되어야 하므로 deploy-base에서 관리한다.

data "aws_iam_policy_document" "github_actions_prod_deploy_assume_role" {
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
      values   = ["repo:${var.github_repository}:environment:${var.prod_environment}"]
    }
  }
}

resource "aws_iam_role" "github_actions_prod_deploy" {
  name               = "${local.prod_name_prefix}-github-actions-deploy-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_prod_deploy_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.prod_name_prefix}-github-actions-deploy-role"
  })
}

locals {
  prod_ecs_cluster_arn      = "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:cluster/${local.prod_name_prefix}-cluster"
  prod_frontend_bucket_name = "${local.prod_name_prefix}-frontend-${data.aws_caller_identity.current.account_id}-${var.aws_region}"

  prod_ecs_service_arns = [
    "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${local.prod_name_prefix}-cluster/${local.prod_name_prefix}-api",
    "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${local.prod_name_prefix}-cluster/${local.prod_name_prefix}-worker",
  ]

  prod_ecs_task_role_arns = [
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.prod_name_prefix}-ecs-execution-role",
    "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.prod_name_prefix}-worker-task-role",
  ]
}

data "aws_iam_policy_document" "github_actions_prod_deploy" {
  statement {
    sid = "ReadProdTerraformState"

    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}",
    ]
  }

  statement {
    sid = "ReadProdTerraformStateObject"

    actions = [
      "s3:GetObject",
    ]

    resources = [
      for key in local.prod_terraform_state_keys : "arn:aws:s3:::${var.terraform_state_bucket_name}/${key}"
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
      local.prod_ecs_cluster_arn,
    ]
  }

  statement {
    actions = [
      "ecs:DescribeServices",
      "ecs:UpdateService",
    ]

    resources = local.prod_ecs_service_arns
  }

  statement {
    actions = [
      "iam:PassRole",
    ]

    resources = local.prod_ecs_task_role_arns

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid = "DeployProdFrontendBucketObjects"

    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.prod_frontend_bucket_name}",
    ]
  }

  statement {
    sid = "DeployProdFrontendObjects"

    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${local.prod_frontend_bucket_name}/*",
    ]
  }

  statement {
    sid = "InvalidateProdFrontendCloudFront"

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

resource "aws_iam_role_policy" "github_actions_prod_deploy" {
  name   = "${local.prod_name_prefix}-github-actions-deploy"
  role   = aws_iam_role.github_actions_prod_deploy.id
  policy = data.aws_iam_policy_document.github_actions_prod_deploy.json
}
