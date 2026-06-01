// 이 root는 GitHub Actions가 dev/prod ECS service를 배포할 때 필요한 장기 IAM role을 만든다.
// secret 값은 받지 않고, repository/environment와 AWS 리소스 이름 기준만 입력으로 둔다.

variable "project" {
  type    = string
  default = "letterpick"
}

variable "dev_environment" {
  type    = string
  default = "dev"
}

variable "prod_environment" {
  type    = string
  default = "prod"
}

variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "terraform_state_bucket_name" {
  type    = string
  default = "letterpick-terraform-state-730335589937-ap-northeast-2"
}

variable "dev_terraform_state_key" {
  type    = string
  default = "letterpick/dev/terraform.tfstate"
}

variable "prod_terraform_state_key" {
  type    = string
  default = "letterpick/prod/terraform.tfstate"
}

variable "prod_persistence_terraform_state_key" {
  type    = string
  default = "letterpick/prod-persistence/terraform.tfstate"
}

variable "prod_runtime_terraform_state_key" {
  type    = string
  default = "letterpick/prod-runtime/terraform.tfstate"
}

variable "github_repository" {
  type    = string
  default = "SunghoCha/letterpick"
}

variable "performance_dataset_bucket_name" {
  type    = string
  default = null
}

variable "performance_dataset_bucket_force_destroy" {
  type    = bool
  default = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
