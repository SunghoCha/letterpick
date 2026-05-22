// 이 파일은 AWS provider의 기본 실행 region을 정한다.
// 아래 region 안에서 VPC, RDS, ECS 같은 AWS 리소스를 만들게 된다.
provider "aws" {
  region = var.aws_region
}

// CloudFront에 붙는 ACM 인증서는 AWS 규칙상 us-east-1에 있어야 한다.
// prod의 나머지 리소스는 ap-northeast-2에 두되, 프론트 인증서만 이 provider를 사용한다.
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}
