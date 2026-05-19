// 이 파일은 Terraform state를 로컬 파일이 아니라 S3에 저장하도록 설정한다.
// state는 "Terraform이 어떤 AWS 리소스를 관리 중인지" 기억하는 파일이다.
// dev 환경을 여러 번 켜고 끌 때 같은 state key를 기준으로 변경 사항을 계산한다.
terraform {
  backend "s3" {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/dev/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
