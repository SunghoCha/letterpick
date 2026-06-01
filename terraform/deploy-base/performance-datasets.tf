// 성능 실험 dataset은 dev runtime보다 오래 유지한다.
// dev를 destroy해도 10k/100k fixture를 다시 업로드하지 않기 위해 deploy-base에서 관리한다.

resource "aws_s3_bucket" "performance_datasets" {
  bucket        = local.performance_dataset_bucket_name
  force_destroy = var.performance_dataset_bucket_force_destroy

  tags = merge(local.common_tags, {
    Name    = local.performance_dataset_bucket_name
    Purpose = "performance-datasets"
  })
}

resource "aws_s3_bucket_public_access_block" "performance_datasets" {
  bucket = aws_s3_bucket.performance_datasets.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "performance_datasets" {
  bucket = aws_s3_bucket.performance_datasets.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "performance_datasets" {
  bucket = aws_s3_bucket.performance_datasets.id

  rule {
    id     = "abort-incomplete-multipart-upload"
    status = "Enabled"

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
