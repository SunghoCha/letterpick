// deploy-base state는 dev runtime state와 분리한다.
// 이 root의 리소스는 dev를 destroy해도 계속 남아 있어야 한다.
terraform {
  backend "s3" {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/deploy-base/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
