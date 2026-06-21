// 이 파일은 dev 성능 실험에서 k6를 실행할 전용 private EC2를 만든다.
// DB access host와 역할을 섞지 않기 위해 별도 인스턴스로 분리한다.
// 외부 ingress 없이 SSM Session Manager로만 접속한다.

data "aws_ami" "al2023_x86_64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-6.1-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

data "aws_iam_policy_document" "k6_runner_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  name               = "${local.name_prefix}-k6-runner-role"
  assume_role_policy = data.aws_iam_policy_document.k6_runner_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-k6-runner-role"
  })
}

resource "aws_iam_role_policy_attachment" "k6_runner_ssm" {
  count = var.enable_k6_runner ? 1 : 0

  role       = aws_iam_role.k6_runner[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "k6_runner_db_secret" {
  count = var.enable_k6_runner ? 1 : 0

  name = "${local.name_prefix}-k6-runner-db-secret"
  role = aws_iam_role.k6_runner[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
        ]
        Resource = aws_db_instance.main.master_user_secret[0].secret_arn
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
        ]
        Resource = aws_db_instance.main.master_user_secret[0].kms_key_id
      },
    ]
  })
}

resource "aws_iam_role_policy" "k6_runner_trending_score_events" {
  count = var.enable_k6_runner ? 1 : 0

  name = "${local.name_prefix}-k6-runner-trending-score-events"
  role = aws_iam_role.k6_runner[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:SendMessageBatch",
        ]
        Resource = aws_sqs_queue.trending_score_events.arn
      },
    ]
  })
}

resource "aws_iam_instance_profile" "k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  name = "${local.name_prefix}-k6-runner-profile"
  role = aws_iam_role.k6_runner[0].name

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-k6-runner-profile"
  })
}

resource "aws_instance" "k6_runner" {
  count = var.enable_k6_runner ? 1 : 0

  ami                         = data.aws_ami.al2023_x86_64.id
  instance_type               = var.k6_runner_instance_type
  subnet_id                   = aws_subnet.private[var.k6_runner_subnet_index].id
  vpc_security_group_ids      = [aws_security_group.k6_runner[0].id]
  iam_instance_profile        = aws_iam_instance_profile.k6_runner[0].name
  associate_public_ip_address = false
  user_data_replace_on_change = true

  user_data_base64 = base64gzip(templatefile("${path.module}/files/k6-runner-user-data.sh.tftpl", {
    aws_region                      = var.aws_region
    public_feed_search_script       = file("${path.module}/files/public-feed-search.js")
    public_issue_view_count_script  = file("${path.module}/files/public-issue-view-count.js")
    trending_score_events_script    = file("${path.module}/files/send-trending-score-events.py")
    trending_score_events_queue_url = aws_sqs_queue.trending_score_events.url
    seed_public_ranking_view_source_script = templatefile("${path.module}/files/seed-public-ranking-view-source.sh.tftpl", {
      aws_region    = var.aws_region
      db_host       = aws_db_instance.main.address
      db_port       = tostring(aws_db_instance.main.port)
      db_name       = var.rds_database_name
      db_secret_arn = aws_db_instance.main.master_user_secret[0].secret_arn
      redis_host    = aws_elasticache_replication_group.redis.primary_endpoint_address
      redis_port    = tostring(var.redis_port)
    })
    seed_public_feed_script = templatefile("${path.module}/files/seed-public-feed.sh.tftpl", {
      aws_region          = var.aws_region
      db_host             = aws_db_instance.main.address
      db_port             = tostring(aws_db_instance.main.port)
      db_name             = var.rds_database_name
      db_secret_arn       = aws_db_instance.main.master_user_secret[0].secret_arn
      default_issue_count = tostring(var.public_feed_seed_issue_count)
    })
  }))

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    encrypted   = true
    volume_size = 8
    volume_type = "gp3"
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-k6-runner"
  })
}
