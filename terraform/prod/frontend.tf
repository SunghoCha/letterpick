// 이 파일은 prod 프론트엔드를 정적 파일로 배포할 공간을 만든다.
// Vue/Vite 앱은 서버 프로세스가 아니라 빌드된 HTML/CSS/JS 파일이므로,
// S3는 파일 보관을 맡고 CloudFront는 HTTPS 공개 진입점과 캐싱을 맡는다.

locals {
  frontend_bucket_name = coalesce(
    var.frontend_bucket_name,
    "${local.name_prefix}-frontend-${data.aws_caller_identity.current.account_id}-${var.aws_region}"
  )

  frontend_origin_id = "${local.name_prefix}-frontend-s3"
  api_origin_id      = "${local.name_prefix}-api-alb"
}

data "aws_cloudfront_cache_policy" "disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "Managed-AllViewer"
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

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${local.name_prefix}-frontend-oac"
  description                       = "Allow CloudFront to read the private prod frontend bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_acm_certificate" "frontend" {
  provider = aws.us_east_1

  domain_name       = var.frontend_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-frontend-certificate"
  })
}

resource "aws_route53_record" "frontend_certificate_validation" {
  for_each = {
    for option in aws_acm_certificate.frontend.domain_validation_options : option.domain_name => {
      name   = option.resource_record_name
      record = option.resource_record_value
      type   = option.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = data.aws_route53_zone.api.zone_id
}

resource "aws_acm_certificate_validation" "frontend" {
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.frontend.arn
  validation_record_fqdns = [for record in aws_route53_record.frontend_certificate_validation : record.fqdn]
}

resource "aws_cloudfront_function" "frontend_spa_rewrite" {
  name    = "${local.name_prefix}-frontend-spa-rewrite"
  runtime = "cloudfront-js-2.0"
  comment = "Rewrite browser routes to index.html while leaving backend paths untouched"
  publish = true
  code    = <<-EOT
    function handler(event) {
      var request = event.request;
      var uri = request.uri;

      if (
        uri.startsWith('/api/') ||
        uri.startsWith('/oauth2/') ||
        uri.startsWith('/login/oauth2/')
      ) {
        return request;
      }

      if (uri === '/' || !uri.includes('.')) {
        request.uri = '/index.html';
      }

      return request;
    }
  EOT
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  comment             = "${local.name_prefix} frontend"
  default_root_object = "index.html"
  aliases             = [var.frontend_domain_name]
  price_class         = var.frontend_cloudfront_price_class

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
    origin_id                = local.frontend_origin_id
  }

  # Backend requests use the same viewer domain as the frontend.
  # CloudFront forwards the original Host header to ALB so Spring/OAuth can build
  # redirects and cookies for letterpicknews.com instead of api.letterpicknews.com.
  origin {
    domain_name = aws_lb.api.dns_name
    origin_id   = local.api_origin_id

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = var.frontend_cloudfront_cache_policy_id
    compress               = true
    target_origin_id       = local.frontend_origin_id
    viewer_protocol_policy = "redirect-to-https"

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.frontend_spa_rewrite.arn
    }
  }

  ordered_cache_behavior {
    path_pattern             = "/api/*"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.disabled.id
    compress                 = true
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
    target_origin_id         = local.api_origin_id
    viewer_protocol_policy   = "redirect-to-https"
  }

  ordered_cache_behavior {
    path_pattern             = "/oauth2/*"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.disabled.id
    compress                 = true
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
    target_origin_id         = local.api_origin_id
    viewer_protocol_policy   = "redirect-to-https"
  }

  ordered_cache_behavior {
    path_pattern             = "/login/oauth2/*"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.disabled.id
    compress                 = true
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
    target_origin_id         = local.api_origin_id
    viewer_protocol_policy   = "redirect-to-https"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.frontend.certificate_arn
    minimum_protocol_version = "TLSv1.2_2021"
    ssl_support_method       = "sni-only"
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-frontend-cloudfront"
  })
}

data "aws_iam_policy_document" "frontend_bucket" {
  statement {
    sid = "AllowCloudFrontRead"

    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${aws_s3_bucket.frontend.arn}/*",
    ]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket.json

  depends_on = [
    aws_s3_bucket_public_access_block.frontend,
  ]
}

resource "aws_route53_record" "frontend" {
  name    = var.frontend_domain_name
  type    = "A"
  zone_id = data.aws_route53_zone.api.zone_id

  alias {
    evaluate_target_health = false
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
  }
}
