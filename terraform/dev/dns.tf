// 이 파일은 dev API에 사람이 읽을 수 있는 HTTPS 도메인을 붙인다.
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
