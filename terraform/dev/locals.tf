// 이 파일은 여러 리소스에서 반복해서 쓸 계산값을 한 곳에 둔다.
// name_prefix는 AWS 리소스 이름의 앞부분을 통일하고,
// common_tags는 나중에 비용 추적과 리소스 검색을 쉽게 하기 위한 공통 태그다.
locals {
  name_prefix = "${var.project}-${var.environment}"

  service_connect_namespace_name    = "${local.name_prefix}.local"
  service_connect_port_name         = "http"
  trending_service_connect_dns_name = "trending-service"
  trending_service_connect_base_url = "http://${local.trending_service_connect_dns_name}:${var.container_port}"

  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  })

  availability_zones = slice(data.aws_availability_zones.available.names, 0, var.availability_zone_count)
}
