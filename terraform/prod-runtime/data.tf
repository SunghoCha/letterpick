data "aws_caller_identity" "current" {}

data "terraform_remote_state" "persistence" {
  backend = "s3"

  config = {
    bucket = "letterpick-terraform-state-730335589937-ap-northeast-2"
    key    = "letterpick/prod-persistence/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
