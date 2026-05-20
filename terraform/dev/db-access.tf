// 이 파일은 dev RDS를 DBeaver 같은 로컬 DB 클라이언트에서 볼 수 있게 하는
// SSM 터널용 private EC2를 만든다. 이 EC2는 앱 서버가 아니라 RDS 접근 중계점이다.
// public IP와 SSH inbound 없이 SSM Agent만 통해 접근하고, RDS는 private 상태를 유지한다.

data "aws_ami" "al2023_arm64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-6.1-arm64"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
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

data "aws_iam_policy_document" "db_access_host_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  name               = "${local.name_prefix}-db-access-host-role"
  assume_role_policy = data.aws_iam_policy_document.db_access_host_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-db-access-host-role"
  })
}

resource "aws_iam_role_policy_attachment" "db_access_host_ssm" {
  count = var.enable_db_access_host ? 1 : 0

  role       = aws_iam_role.db_access_host[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  name = "${local.name_prefix}-db-access-host-profile"
  role = aws_iam_role.db_access_host[0].name

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-db-access-host-profile"
  })
}

resource "aws_instance" "db_access_host" {
  count = var.enable_db_access_host ? 1 : 0

  ami                         = data.aws_ami.al2023_arm64.id
  instance_type               = var.db_access_instance_type
  subnet_id                   = aws_subnet.private[var.db_access_subnet_index].id
  vpc_security_group_ids      = [aws_security_group.db_access_host[0].id]
  iam_instance_profile        = aws_iam_instance_profile.db_access_host[0].name
  associate_public_ip_address = false

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
    Name = "${local.name_prefix}-db-access-host"
  })
}
