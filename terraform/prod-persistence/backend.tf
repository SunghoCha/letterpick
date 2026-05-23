terraform {
  backend "s3" {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/prod-persistence/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
