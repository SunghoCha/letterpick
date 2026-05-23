data "aws_route53_zone" "api" {
  name         = "${trimsuffix(local.persistence.route53_zone_name, ".")}."
  private_zone = false
}

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
