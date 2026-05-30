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

  user_data = templatefile("${path.module}/files/k6-runner-user-data.sh.tftpl", {
    k6_script = file("${path.module}/files/public-feed-search.js")
  })

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
