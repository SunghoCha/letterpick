terraform {
  backend "s3" {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/prod-runtime/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
