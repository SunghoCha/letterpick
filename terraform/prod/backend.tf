// 이 파일은 Terraform state를 로컬 파일이 아니라 S3에 저장하도록 설정한다.
// state는 "Terraform이 어떤 AWS 리소스를 관리 중인지" 기억하는 파일이다.
// prod 1단계는 일체형 root이므로 하나의 state key로 전체 실행 환경을 관리한다.
terraform {
  backend "s3" {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/prod/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
