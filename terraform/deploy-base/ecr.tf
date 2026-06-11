// 이 파일은 backend/trending-service image publish에 필요한 장기 리소스를 관리한다.
// 기존 backend ECR repository는 이미 AWS에 있으므로 data source로 참조하고,
// 새 trending-service repository와 GitHub Actions image publish role은 deploy-base에서 관리한다.

locals {
  backend_ecr_repository_name          = coalesce(var.backend_ecr_repository_name, "${var.project}-backend")
  trending_service_ecr_repository_name = coalesce(var.trending_service_ecr_repository_name, "${var.project}-trending-service")
  github_actions_image_publish_role_name = coalesce(
    var.github_actions_image_publish_role_name,
    "${var.project}-github-actions-image-publish-role"
  )
}

data "aws_ecr_repository" "backend" {
  name = local.backend_ecr_repository_name
}

resource "aws_ecr_repository" "trending_service" {
  name                 = local.trending_service_ecr_repository_name
  image_tag_mutability = "MUTABLE"
  force_delete         = var.trending_service_ecr_force_delete

  tags = merge(local.common_tags, {
    Name = local.trending_service_ecr_repository_name
  })
}

resource "aws_ecr_lifecycle_policy" "trending_service" {
  repository = aws_ecr_repository.trending_service.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep recent trending-service images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

data "aws_iam_policy_document" "github_actions_image_publish_assume_role" {
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
      values   = ["repo:${var.github_repository}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_actions_image_publish" {
  name               = local.github_actions_image_publish_role_name
  assume_role_policy = data.aws_iam_policy_document.github_actions_image_publish_assume_role.json

  tags = merge(local.common_tags, {
    Name = local.github_actions_image_publish_role_name
  })
}

data "aws_iam_policy_document" "github_actions_image_publish" {
  statement {
    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = ["*"]
  }

  statement {
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]

    resources = [
      data.aws_ecr_repository.backend.arn,
      aws_ecr_repository.trending_service.arn,
    ]
  }
}

resource "aws_iam_role_policy" "github_actions_image_publish" {
  name   = "${var.project}-github-actions-image-publish"
  role   = aws_iam_role.github_actions_image_publish.id
  policy = data.aws_iam_policy_document.github_actions_image_publish.json
}
