locals {
  ses_receipt_rule_set_name = coalesce(var.ses_receipt_rule_set_name, "${local.name_prefix}-receive-rules")
  ses_receipt_rule_name     = coalesce(var.ses_receipt_rule_name, "${local.name_prefix}-store-and-notify")
  ses_inbound_mx_record     = "10 inbound-smtp.${var.aws_region}.amazonaws.com"
}

resource "aws_ses_domain_identity" "mail_receive" {
  domain = var.ses_receive_domain_name
}

resource "aws_route53_record" "ses_domain_identity" {
  zone_id = data.aws_route53_zone.api.zone_id
  name    = "_amazonses.${var.ses_receive_domain_name}"
  type    = "TXT"
  ttl     = var.ses_dns_ttl
  records = [aws_ses_domain_identity.mail_receive.verification_token]
}

resource "aws_ses_domain_identity_verification" "mail_receive" {
  domain = aws_ses_domain_identity.mail_receive.id

  depends_on = [
    aws_route53_record.ses_domain_identity,
  ]
}

resource "aws_ses_domain_dkim" "mail_receive" {
  domain = aws_ses_domain_identity.mail_receive.domain
}

resource "aws_route53_record" "ses_dkim" {
  count = 3

  zone_id = data.aws_route53_zone.api.zone_id
  name    = "${aws_ses_domain_dkim.mail_receive.dkim_tokens[count.index]}._domainkey.${var.ses_receive_domain_name}"
  type    = "CNAME"
  ttl     = var.ses_dns_ttl
  records = ["${aws_ses_domain_dkim.mail_receive.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

data "aws_iam_policy_document" "ses_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["ses.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_role" "ses_s3_action" {
  name               = "${local.name_prefix}-ses-s3-action-role"
  assume_role_policy = data.aws_iam_policy_document.ses_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-ses-s3-action-role"
  })
}

data "aws_iam_policy_document" "ses_s3_action" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      local.raw_mail_object_arn,
    ]
  }

  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      aws_sns_topic.mail_received.arn,
    ]
  }
}

resource "aws_iam_role_policy" "ses_s3_action" {
  name   = "${local.name_prefix}-ses-s3-action"
  role   = aws_iam_role.ses_s3_action.id
  policy = data.aws_iam_policy_document.ses_s3_action.json
}

resource "aws_ses_receipt_rule_set" "mail_receive" {
  rule_set_name = local.ses_receipt_rule_set_name
}

resource "aws_ses_active_receipt_rule_set" "mail_receive" {
  rule_set_name = aws_ses_receipt_rule_set.mail_receive.rule_set_name
}

resource "aws_ses_receipt_rule" "store_and_notify" {
  name          = local.ses_receipt_rule_name
  rule_set_name = aws_ses_receipt_rule_set.mail_receive.rule_set_name
  recipients    = [var.ses_receive_domain_name]
  enabled       = true
  scan_enabled  = true
  tls_policy    = "Optional"

  s3_action {
    position          = 1
    bucket_name       = aws_s3_bucket.raw_mail.bucket
    object_key_prefix = local.raw_mail_object_prefix_slash
    topic_arn         = aws_sns_topic.mail_received.arn
    iam_role_arn      = aws_iam_role.ses_s3_action.arn
  }

  depends_on = [
    aws_iam_role_policy.ses_s3_action,
    aws_ses_domain_identity_verification.mail_receive,
  ]
}

resource "aws_route53_record" "ses_mx" {
  zone_id = data.aws_route53_zone.api.zone_id
  name    = var.ses_receive_domain_name
  type    = "MX"
  ttl     = var.ses_dns_ttl
  records = [local.ses_inbound_mx_record]

  depends_on = [
    aws_ses_receipt_rule.store_and_notify,
    aws_ses_active_receipt_rule_set.mail_receive,
  ]
}
