data "aws_route53_zone" "api" {
  name         = "${trimsuffix(var.route53_zone_name, ".")}."
  private_zone = false
}
