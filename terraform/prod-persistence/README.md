# prod-persistence

상태: persistence 리소스 복제 및 validate 확인, 아직 apply 대상 아님

이 root는 prod runtime을 꺼도 남아야 하는 장기 리소스를 소유한다.

소유 예정 리소스:

```text
VPC/subnet/route table/internet gateway
RDS
SES/S3/SNS/SQS mail pipeline
frontend S3 bucket
```

현재 `terraform/prod` full-start root는 그대로 둔다.
기존 state 전환 방식은 별도 결정 후 진행한다.
