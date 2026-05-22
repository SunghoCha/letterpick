// 이 파일은 prod ALB가 CloudFront origin 요청을 HTTPS로 받을 수 있게 한다.
// prod의 사용자 공개 진입점은 CloudFront의 letterpicknews.com 하나로 둔다.
// ALB에는 별도 공개 API DNS를 붙이지 않고, CloudFront가 전달하는 Host에 맞는
// regional ACM 인증서만 붙인다.

data "aws_route53_zone" "api" {
  name         = "${trimsuffix(var.route53_zone_name, ".")}."
  private_zone = false
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
