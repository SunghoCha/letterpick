// 이 파일은 이 Terraform root가 어떤 Terraform 버전과 provider 버전을 전제로 하는지 고정한다.
// 같은 코드를 다른 PC나 CI에서 실행해도 AWS provider 동작이 크게 달라지지 않게 하는 기준점이다.
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}
