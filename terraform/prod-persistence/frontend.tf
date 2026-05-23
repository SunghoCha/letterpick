locals {
  frontend_bucket_name = coalesce(
    var.frontend_bucket_name,
    "${local.name_prefix}-frontend-${data.aws_caller_identity.current.account_id}-${var.aws_region}"
  )
}

resource "aws_s3_bucket" "frontend" {
  bucket        = local.frontend_bucket_name
  force_destroy = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-frontend"
  })
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
