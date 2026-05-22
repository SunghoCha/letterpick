// 이 파일은 prod API에 사람이 읽을 수 있는 HTTPS 도메인을 붙인다.
// Route 53은 도메인 이름을 ALB로 보내고, ACM은 ALB가 HTTPS 인증서를 제시하게 한다.

data "aws_route53_zone" "api" {
  name         = "${trimsuffix(var.route53_zone_name, ".")}."
  private_zone = false
}

resource "aws_acm_certificate" "api" {
  domain_name       = var.api_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-certificate"
  })
}

resource "aws_route53_record" "api_certificate_validation" {
  for_each = {
    for option in aws_acm_certificate.api.domain_validation_options : option.domain_name => {
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

resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.api_certificate_validation : record.fqdn]
}

// CloudFront의 백엔드 경로는 브라우저가 본 Host(letterpicknews.com)를
// 그대로 ALB에 전달한다. CloudFront는 HTTPS origin 인증서를 Host 기준으로
// 검증하므로, ALB에도 같은 도메인의 regional ACM 인증서가 필요하다.
resource "aws_acm_certificate" "frontend_origin" {
  domain_name       = var.frontend_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-frontend-origin-certificate"
  })
}

resource "aws_route53_record" "frontend_origin_certificate_validation" {
  for_each = {
    for option in aws_acm_certificate.frontend_origin.domain_validation_options : option.domain_name => {
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

resource "aws_acm_certificate_validation" "frontend_origin" {
  certificate_arn         = aws_acm_certificate.frontend_origin.arn
  validation_record_fqdns = [for record in aws_route53_record.frontend_origin_certificate_validation : record.fqdn]
}

resource "aws_route53_record" "api" {
  name    = var.api_domain_name
  type    = "A"
  zone_id = data.aws_route53_zone.api.zone_id

  alias {
    evaluate_target_health = true
    name                   = aws_lb.api.dns_name
    zone_id                = aws_lb.api.zone_id
  }
}
