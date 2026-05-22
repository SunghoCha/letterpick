// 이 파일은 private subnet의 outbound 통신 경로를 만든다.
// ECS task와 RDS는 private subnet에 두지만, task는 ECR image pull, Secrets Manager,
// CloudWatch Logs, OAuth provider 접근처럼 외부 통신이 필요하다.
// prod 1단계에서는 구조를 단순하게 유지하기 위해 public subnet 하나에 NAT Gateway 1개를 둔다.

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat-eip"
  })
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat"
  })

  depends_on = [
    aws_internet_gateway.main,
  ]
}

resource "aws_route" "private_nat" {
  count = var.availability_zone_count

  route_table_id         = aws_route_table.private[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.main.id
}
