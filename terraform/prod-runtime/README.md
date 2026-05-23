# prod-runtime

상태: runtime 리소스 복제 및 validate 확인, 아직 apply 대상 아님

이 root는 비용 때문에 켜고 끌 수 있는 prod 실행 리소스를 소유한다.

소유 예정 리소스:

```text
NAT Gateway/private NAT route
ALB/listener/target groups
ECS cluster/task definitions/services
CloudFront distribution/frontend Route53 A record
DB access host
runtime IAM/security group/log group
```

이 root는 persistence 리소스를 생성하지 않는다.
persistence root의 계약 값은 `terraform_remote_state`로 읽는다.
