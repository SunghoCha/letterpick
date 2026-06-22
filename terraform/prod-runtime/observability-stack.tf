// 이 파일은 prod runtime 점검에서 사용할 관측 저장소와 UI를 private EC2에 올린다.
// Grafana는 public inbound 없이 SSM port forwarding으로만 접근한다.

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
}

data "aws_iam_policy_document" "observability_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "observability" {
  count = var.enable_observability_stack ? 1 : 0

  name               = "${local.name_prefix}-observability-role"
  assume_role_policy = data.aws_iam_policy_document.observability_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-observability-role"
  })
}

resource "aws_iam_role_policy_attachment" "observability_ssm" {
  count = var.enable_observability_stack ? 1 : 0

  role       = aws_iam_role.observability[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "observability" {
  count = var.enable_observability_stack ? 1 : 0

  name = "${local.name_prefix}-observability-profile"
  role = aws_iam_role.observability[0].name

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-observability-profile"
  })
}

resource "aws_instance" "observability" {
  count = var.enable_observability_stack ? 1 : 0

  ami                         = data.aws_ami.al2023_x86_64.id
  instance_type               = var.observability_instance_type
  subnet_id                   = local.persistence.private_subnet_ids[var.observability_subnet_index]
  vpc_security_group_ids      = [aws_security_group.observability[0].id]
  iam_instance_profile        = aws_iam_instance_profile.observability[0].name
  associate_public_ip_address = false
  user_data_replace_on_change = true

  user_data = templatefile("${path.module}/files/observability-user-data.sh.tftpl", {
    docker_compose_version = var.observability_docker_compose_version
    prometheus_image       = var.observability_prometheus_image
    tempo_image            = var.observability_tempo_image
    grafana_image          = var.observability_grafana_image
    prometheus_port        = var.observability_prometheus_port
    tempo_otlp_grpc_port   = var.observability_tempo_otlp_grpc_port
    tempo_otlp_http_port   = var.observability_tempo_otlp_http_port
    grafana_port           = var.observability_grafana_port
  })

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    encrypted   = true
    volume_size = 20
    volume_type = "gp3"
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-observability"
  })
}
