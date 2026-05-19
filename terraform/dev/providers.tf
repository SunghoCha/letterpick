// 이 파일은 AWS provider의 기본 실행 region을 정한다.
// 아래 region 안에서 VPC, RDS, ECS 같은 AWS 리소스를 만들게 된다.
provider "aws" {
  region = var.aws_region
}
