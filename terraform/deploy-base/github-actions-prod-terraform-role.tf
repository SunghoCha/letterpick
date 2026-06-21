// 이 파일은 GitHub Actions start-prod workflow가 terraform/prod apply를 실행할 수 있게 한다.
// prod full-start는 dev처럼 일체형 runtime을 만들되 API service만 ECS native Blue/Green으로 둔다.

data "aws_iam_policy_document" "github_actions_prod_terraform_assume_role" {
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

resource "aws_iam_role" "github_actions_prod_terraform" {
  name               = "${local.prod_name_prefix}-github-actions-terraform-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_prod_terraform_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.prod_name_prefix}-github-actions-terraform-role"
  })
}

data "aws_iam_policy_document" "github_actions_prod_terraform" {
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

    resources = flatten([
      for key in local.prod_terraform_state_keys : [
        "arn:aws:s3:::${var.terraform_state_bucket_name}/${key}",
        "arn:aws:s3:::${var.terraform_state_bucket_name}/${key}.tflock",
      ]
    ])
  }

  statement {
    sid = "ManageProdRuntime"

    actions = [
      "ec2:*",
      "ecs:*",
      "elasticache:*",
      "elasticloadbalancing:*",
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

resource "aws_iam_role_policy" "github_actions_prod_terraform" {
  name   = "${local.prod_name_prefix}-github-actions-terraform"
  role   = aws_iam_role.github_actions_prod_terraform.id
  policy = data.aws_iam_policy_document.github_actions_prod_terraform.json
}
