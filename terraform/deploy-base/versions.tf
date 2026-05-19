// 이 root가 사용하는 Terraform/provider 버전을 고정한다.
// dev/prod runtime보다 오래 유지되는 배포 기반 리소스를 관리한다.
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}
