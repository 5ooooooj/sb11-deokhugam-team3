# 📚 덕후감

> 책 읽는 즐거움을 공유하고, 지식과 감상을 나누는 책 덕후들의 커뮤니티 서비스

[![CI](https://github.com/5ooooooj/sb11-deokhugam-team3/actions/workflows/ci.yml/badge.svg)](https://github.com/5ooooooj/sb11-deokhugam-team3/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/5ooooooj/sb11-deokhugam-team3/branch/develop/graph/badge.svg)](https://codecov.io/gh/5ooooooj/sb11-deokhugam-team3)

<br>

## 📋 목차

* [서비스 소개](#서비스-소개)
* [팀원](#팀원)
* [기술 스택](#기술-스택)
* [주요 기능](#주요-기능)
* [아키텍처](#아키텍처)
* [ERD](#erd)
* [API 명세](#api-명세)
* [로컬 실행 방법](#로컬-실행-방법)
* [DB 마이그레이션](#db-마이그레이션)
* [CI/CD](#cicd)
* [컨벤션](#컨벤션)

<br>

## 서비스 소개

**덕후감**은 도서 검색, 리뷰, 댓글, 좋아요, 알림, 대시보드를 제공하는 책 커뮤니티 서비스입니다.

사용자는 도서를 등록하고 리뷰를 작성할 수 있으며, 댓글과 좋아요를 통해 다른 사용자와 독서 경험을 공유할 수 있습니다.
또한 OCR Space API와 Naver Book API를 활용해 ISBN 기반 도서 정보 자동 입력을 지원하고, 인기 도서·인기 리뷰·파워 유저 대시보드를 통해 서비스 내 독서 흐름을 확인할 수 있습니다.

<br>

## 팀원

| 이름  | 역할                                |
| --- | --------------------------------- |
| 강우진 | 팀장, 도서 관리 및 외부 API 연동             |
| 김지혜 | 댓글 관리, 알림 관리                      |
| 김하빈 | 리뷰 관리, 좋아요 관리, 테스트 전략 수립          |
| 이윤선 | 대시보드, Spring Batch, CI/CD, 인프라 구성 |
| 정수용 | 사용자 관리, 공통 모듈, 배포 환경 구성           |

<br>

## 기술 스택

| 분류                | 기술                                                       |
| ----------------- | -------------------------------------------------------- |
| Language          | Java 17                                                  |
| Framework         | Spring Boot 3.5.x                                        |
| Web               | Spring MVC, Validation                                   |
| Database          | PostgreSQL, AWS RDS                                      |
| ORM / Query       | Spring Data JPA, QueryDSL                                |
| Migration         | Flyway                                                   |
| Batch / Scheduler | Spring Batch, Spring Scheduler                           |
| Storage           | AWS S3                                                   |
| Infra             | Docker, AWS ECR, AWS ECS, GitHub Actions                 |
| Documentation     | springdoc-openapi, Swagger UI                            |
| External API      | Naver Book API, OCR Space API                            |
| Test              | JUnit 5, Mockito, EasyRandom, Testcontainers, LocalStack |
| Coverage          | JaCoCo, Codecov                                          |

<br>

## 주요 기능

### 👤 사용자 관리

* 이메일, 닉네임, 비밀번호 기반 사용자 등록
* 로그인
* 사용자 조회 및 닉네임 수정
* 사용자 논리 삭제
* 논리 삭제 후 1일이 지난 사용자 자동 물리 삭제
* 헤더 기반 사용자 식별 및 권한 검증

### 📖 도서 관리

* 도서 등록, 조회, 수정, 삭제
* 도서 등록자 기준 수정/삭제 권한 검증
* 도서 논리 삭제 및 물리 삭제
* 키워드 기반 도서 검색

  * 제목
  * 저자
  * ISBN
* 정렬 및 커서 기반 페이지네이션
* ISBN 기반 도서 정보 자동 조회
* OCR Space API를 활용한 이미지 내 ISBN 텍스트 추출
* ISBN-10 / ISBN-13 체크섬 검증
* Naver Book API를 통한 도서 정보 자동 수집
* Naver API 응답 정규화

  * HTML 태그 제거
  * 저자 구분자 정리
  * 긴 설명 1000자 이하 정리
* Naver 썸네일 URL 다운로드 후 Base64 변환 응답
* 도서 썸네일 이미지 S3 업로드
* S3 업로드 후 DB 저장 실패 시 롤백 보상 처리

### ✍️ 리뷰 관리

* 도서별 리뷰 작성
* 도서별 1인 1리뷰 제한
* 리뷰 조회, 수정, 삭제
* 리뷰 논리 삭제 및 물리 삭제
* 별점 등록
* 리뷰 목록 검색 및 커서 페이지네이션
* 리뷰 목록 응답에 `likedByMe` 포함
* 리뷰 좋아요 등록 및 취소
* 중복 좋아요 요청에 대한 멱등성 처리

### 💬 댓글 관리

* 리뷰별 댓글 작성
* 댓글 조회, 수정, 삭제
* 댓글 논리 삭제 및 물리 삭제
* 댓글 작성/삭제 시 리뷰의 `commentCount` 정합성 유지
* 시간순 정렬 및 커서 페이지네이션
* 삭제된 댓글 정리 스케줄러

### 🔔 알림 관리

* 내 리뷰에 댓글 발생 시 알림 생성
* 내 리뷰에 좋아요 발생 시 알림 생성
* 인기 리뷰 TOP 10 진입 시 알림 생성
* 알림 목록 조회
* 단건 알림 읽음 처리
* 전체 알림 읽음 처리
* 확인된 알림 7일 경과 시 자동 삭제

### 🏆 대시보드

* 인기 도서 순위
* 인기 리뷰 순위
* 파워 유저 순위
* 일간 / 주간 / 월간 / 역대 기간별 집계
* Spring Batch 기반 점수 산출
* 총 3개 영역 × 4개 기간 기준, 12개 Batch Job 구성
* 매일 00시 기준 대시보드 데이터 계산
* 배치 결과 테이블 저장 후 대시보드 API에서 조회

<br>

## 아키텍처

```text
                GitHub Actions
                  CI / CD
                     │
                     ▼
             AWS ECR Image Push
                     │
                     ▼
              AWS ECS Service
          ┌─────────────────────┐
          │   Spring Boot App   │
          │  static FE + API    │
          └─────────────────────┘
             │        │        │
             │        │        └── OCR Space API
             │        └────────── Naver Book API
             │
     ┌───────┴────────┐
     ▼                ▼
 PostgreSQL RDS      AWS S3
 데이터 저장          썸네일 / 파일 저장
```

<br>

## ERD

<img width="5260" height="3864" alt="deokhugam-team3-erd" src="https://github.com/user-attachments/assets/bc8ed7e4-a979-4d33-b87d-370e99810344" />

> 주요 테이블: `USERS`, `BOOKS`, `REVIEWS`, `COMMENTS`, `REVIEW_LIKES`, `NOTIFICATIONS`, `POPULAR_BOOKS`, `POPULAR_REVIEWS`, `POWER_USERS`

<br>

## API 명세

로컬 실행 후 Swagger UI에서 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui.html
```

또는

```text
http://localhost:8080/swagger-ui/index.html
```

| 도메인      | 주요 엔드포인트                                                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 사용자      | `POST /api/users`, `POST /api/users/login`, `GET /api/users/{userId}`, `PATCH /api/users/{userId}`, `DELETE /api/users/{userId}`                       |
| 도서       | `GET /api/books`, `POST /api/books`, `GET /api/books/{bookId}`, `PATCH /api/books/{bookId}`, `DELETE /api/books/{bookId}`                              |
| 도서 외부 연동 | `GET /api/books/info`, `POST /api/books/isbn/ocr`                                                                                                      |
| 리뷰       | `GET /api/reviews`, `POST /api/reviews`, `GET /api/reviews/{reviewId}`, `PATCH /api/reviews/{reviewId}`, `DELETE /api/reviews/{reviewId}`              |
| 리뷰 좋아요   | `POST /api/reviews/{reviewId}/like`                                                                                                                    |
| 댓글       | `GET /api/comments`, `POST /api/comments`, `GET /api/comments/{commentId}`, `PATCH /api/comments/{commentId}`, `DELETE /api/comments/{commentId}`      |
| 알림       | `GET /api/notifications`, `PATCH /api/notifications/{notificationId}`, `PATCH /api/notifications/read-all`                                             |
| 대시보드     | `GET /api/books/popular`, `GET /api/reviews/popular`, `GET /api/users/power`                                                                           |
| 배치 관리    | `POST /api/admin/batch/popular-books`, `POST /api/admin/batch/popular-reviews`, `POST /api/admin/batch/power-users`, `POST /api/admin/batch/dashboard` |

> 인증이 필요한 API는 요청 헤더에 `Deokhugam-Request-User-ID: {userId}`를 포함해야 합니다.

<br>

## 로컬 실행 방법

### 사전 요구사항

* Java 17+
* Docker
* Docker Compose

<br>

### 1. 저장소 클론

```bash
git clone https://github.com/5ooooooj/sb11-deokhugam-team3.git
cd sb11-deokhugam-team3
```

<br>

### 2. 환경 변수 설정

`.env.example`을 복사해 `.env`를 생성합니다.

```bash
cp .env.example .env
```

`.env` 예시입니다.

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=deokhugam
DB_USERNAME=deokhugam
DB_PASSWORD=deokhugam1234

# AWS
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=

# Naver Book API
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=

# OCR Space API
OCR_SPACE_API_KEY=

# App
SPRING_PROFILES_ACTIVE=local
```

<br>

### 3. PostgreSQL 실행

```bash
docker compose up -d postgres
```

<br>

### 4. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

> 최신 버전은 Flyway를 사용하므로 `schema.sql`을 직접 실행하지 않습니다.
> 애플리케이션 실행 시 `src/main/resources/db/migration` 하위 migration 파일이 자동 적용됩니다.

<br>

### 5. 테스트 실행

```bash
./gradlew test
```

커버리지 리포트까지 확인하려면 아래 명령어를 사용할 수 있습니다.

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification --continue
```

<br>

## DB 마이그레이션

이 프로젝트는 **Flyway**를 사용해 DB 스키마 변경 이력을 코드 기반으로 관리합니다.

기존에는 `schema.sql`을 기준으로 스키마를 관리하고 운영 RDS에는 필요한 DDL을 직접 실행하는 방식이었지만, 최신 버전에서는 `src/main/resources/db/migration` 하위의 Flyway migration 파일을 기준으로 DB 변경 사항을 관리합니다.

```text
src/main/resources/db/migration
└── V1__init_schema.sql
```

### 로컬 환경

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration

  jpa:
    hibernate:
      ddl-auto: validate
```

로컬 환경에서도 Hibernate가 테이블을 자동 생성하지 않고, Flyway migration으로 생성된 스키마를 기준으로 검증합니다.

### 운영 환경

운영 환경에서는 기존 RDS에 이미 스키마가 적용되어 있던 상황을 고려해 baseline 설정을 사용합니다.

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1
```

이후 DB 변경이 필요하면 운영 RDS에 직접 SQL을 실행하지 않고, `V2__...sql`, `V3__...sql`과 같은 migration 파일을 추가해 관리합니다.

<br>

## CI/CD

### CI

GitHub Actions를 통해 `main`, `develop` 브랜치 push와 PR 생성 시 빌드와 테스트를 수행합니다.

CI 단계에서는 PostgreSQL 서비스 컨테이너를 사용해 테스트 DB를 실행하고, 외부 API와 AWS S3 값은 테스트용 더미 값 또는 mock을 사용합니다.

```text
Push / Pull Request
        │
        ▼
Build
        │
        ▼
Test
        │
        ▼
JaCoCo Report
        │
        ▼
Codecov Upload
```

### CD

`main` 브랜치에 push되면 CD workflow가 실행됩니다.

```text
main push
   │
   ▼
Docker build
   │
   ▼
AWS ECR push
   │
   ▼
ECS Task Definition revision 등록
   │
   ▼
ECS Service update
```

AWS 접근 키 같은 민감한 값은 GitHub Secrets로 관리하고, AWS Region, ECR Repository, ECS Cluster, ECS Service 같은 배포 설정값은 GitHub Variables로 관리합니다.

<br>

## 컨벤션

### 브랜치 전략

```text
main                          — 릴리즈 브랜치
develop                       — 통합 개발 브랜치
feature/{이슈번호}-{기능요약}   — 기능 개발
fix/{이슈번호}-{수정요약}       — 버그 수정
refactor/{이슈번호}-{요약}      — 리팩터링
```

<br>

### 커밋 메시지

```text
Feat: 새로운 기능 추가
Fix: 버그 수정
Refactor: 기능 변경 없는 코드 개선
Style: 코드 포맷팅, 세미콜론 누락 등
Test: 테스트 코드 추가 및 수정
Docs: 문서 수정
Chore: 빌드, 설정, 기타 작업
Infra: 인프라 및 배포 관련 작업
```

예시:

```text
Feat: 도서 등록 API 구현 [#12]
Fix: ISBN 썸네일 Base64 응답 처리 [#102]
Test: 도서 이미지 최적화 테스트 보강 [#102]
Docs: README 실행 방법 수정 [#110]
```

<br>

### 코드 스타일

* Google Java Style Guide 적용
* `System.out.println()` 사용 금지
* 로그는 `log.info()`, `log.warn()`, `log.error()` 사용
* 패키지 구조는 계층형 구조 사용

```text
controller
service
repository
domain
dto
config
exception
client
batch
```

<br>

### 테스트

* JUnit 5 기반 테스트 작성
* Mockito를 활용한 단위 테스트
* Spring Boot Test 기반 통합 테스트
* Testcontainers / LocalStack을 활용한 외부 환경 테스트
* `@Tag("wip")` 테스트는 CI 실행 대상에서 제외

<br>

### 인증 / 권한

현재 프로젝트는 JWT를 사용하지 않고, 헤더 기반 사용자 식별 방식을 사용합니다.

```text
Deokhugam-Request-User-ID: {userId}
```

* 사용자 식별: 요청 Header
* 인증 검증: Interceptor
* 권한 검증: Service 계층
* 도서, 리뷰, 댓글 수정/삭제 시 작성자 또는 등록자 기준으로 검증

> 실제 운영 서비스로 확장할 경우 JWT 또는 Session 기반 인증 구조로 개선할 수 있습니다.
