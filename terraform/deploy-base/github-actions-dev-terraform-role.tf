// 이 파일은 GitHub Actions start-dev workflow가 terraform/dev apply를 실행할 수 있게 한다.
// dev runtime 생성/수정 권한이므로 deploy-dev role보다 권한 범위가 넓다.

data "aws_iam_policy_document" "github_actions_dev_terraform_assume_role" {
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

resource "aws_iam_role" "github_actions_dev_terraform" {
  name               = "${local.dev_name_prefix}-github-actions-terraform-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_dev_terraform_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.dev_name_prefix}-github-actions-terraform-role"
  })
}

data "aws_iam_policy_document" "github_actions_dev_terraform" {
  statement {
    sid = "TerraformStateAccess"

    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}",
    ]
  }

  statement {
    sid = "TerraformStateObjectAccess"

    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]

    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}/${var.dev_terraform_state_key}",
      "arn:aws:s3:::${var.terraform_state_bucket_name}/${var.dev_terraform_state_key}.tflock",
    ]
  }

  statement {
    sid = "ManageDevRuntime"

    actions = [
      "ec2:*",
      "ecs:*",
      "elasticache:*",
      "elasticloadbalancing:*",
      "cloudwatch:*",
      "cloudfront:*",
      "iam:*",
      "logs:*",
      "acm:*",
      "route53:*",
      "servicediscovery:*",
      "rds:*",
      "s3:*",
      "secretsmanager:*",
      "ses:*",
      "sns:*",
      "sqs:*",
      "kms:Describe*",
      "kms:List*",
    ]

    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_actions_dev_terraform" {
  name   = "${local.dev_name_prefix}-github-actions-terraform"
  role   = aws_iam_role.github_actions_dev_terraform.id
  policy = data.aws_iam_policy_document.github_actions_dev_terraform.json
}
