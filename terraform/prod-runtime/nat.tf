resource "aws_eip" "nat" {
  domain = "vpc"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat-eip"
  })
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = local.persistence.public_subnet_ids[0]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat"
  })
}

resource "aws_route" "private_nat" {
  count = length(local.persistence.private_route_table_ids)

  route_table_id         = local.persistence.private_route_table_ids[count.index]
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.main.id
}
