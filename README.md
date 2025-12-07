# Holiday Keeper

전 세계 공휴일 데이터를 저장·조회·관리하는 Mini Service

## 🎯 프로젝트 개요

Nager.Date API를 활용하여 2020~2025년의 전 세계 공휴일 데이터를 수집하고 관리하는 REST API 서비스입니다.

### 주요 기능

1. **데이터 적재**: 최초 실행 시 모든 국가의 공휴일 데이터 자동 수집
2. **검색**: 연도, 국가, 기간, 타입 등 다양한 필터로 공휴일 조회 (페이징 지원)
3. **재동기화**: 특정 연도/국가 데이터를 외부 API에서 재호출하여 업데이트
4. **삭제**: 특정 연도/국가의 공휴일 레코드 전체 삭제
5. **배치 자동화**: 매년 1월 2일 01:00 KST에 전년도·금년도 데이터 자동 동기화

## 🌟 기술적 특징

### 1. Record 활용

- **Record 클래스 적극 활용**
    - 모든 요청/응답 DTO를 Record로 구현하여 불변성 보장
    - 보일러플레이트 코드 최소화 및 가독성 향상
    - `HolidaySearchRequest`, `HolidayResponse`, `HolidayRefreshResponse` 등

### 2. 비동기 논블로킹 처리

- **WebClient + Reactor 기반 병렬 처리**
    - 여러 국가의 공휴일 데이터를 동시에 수집
    - 블로킹 방식 대비 **약 10배 이상 성능 향상**
    - 전체 국가(100+) 데이터 동기화 시간 대폭 단축

### 3. 동적 쿼리 with Querydsl

- **Querydsl 기반 유연한 검색 엔진**
    - 다중 필터 조건을 동적으로 조합
    - 타입 안전한 쿼리 작성

### 4. 계층별 책임 분리

- **Converter 패턴으로 변환 로직 분리**
    - `HolidayConverter`: API 응답 ↔ Entity ↔ DTO 변환 전담
    - Service 계층의 비즈니스 로직과 변환 로직 분리

### 5. 글로벌 예외 처리

- **@RestControllerAdvice 기반 통합 예외 처리**
    - 모든 예외를 일관된 형식으로 응답
    - 클라이언트 친화적인 에러 메시지
    - HTTP 상태 코드 자동 매핑

### 6. 데이터 정합성 보장

- **실제 변경 감지 알고리즘**
    - 날짜뿐만 아니라 모든 필드(localName, name, fixed 등) 비교
    - Set 연산을 통한 효율적인 차이 계산
    - 재동기화 시 `actualChangedCount`로 실제 변경 건수 정확히 추적

### 7. API 문서 자동화

- **SpringDoc OpenAPI 3 통합**
    - 코드 변경 시 문서 자동 업데이트
    - Swagger UI로 즉시 테스트 가능
    - `@Schema` 어노테이션으로 상세한 설명 제공

## 🚀 시작하기

### 사전 요구사항

- JDK 21 이상
- Gradle 8.x 이상 (또는 내장 Gradle Wrapper 사용)

### 빌드 및 실행

#### 방법 1: Gradle Wrapper 사용 (권장)

```bash
# 1. 프로젝트 클론
git clone https://github.com/JaeY0ung/holidaykeeper-backend.git
cd holidaykeeper-backend

# 2. 빌드
./gradlew clean build

# 3. 실행
./gradlew bootRun
```

#### 방법 2: JAR 파일로 실행

```bash
# 1. 빌드
./gradlew clean build

# 2. JAR 파일 실행
java -jar build/libs/app.jar
```

#### 방법 3: IDE에서 실행

### 접속 URL

애플리케이션이 시작되면:

- **서버**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
    - JDBC URL: `jdbc:h2:file:./data/holidaydb;AUTO_SERVER=TRUE`
    - Username: `sa`
    - Password: (비워두기)

### 최초 실행 시

애플리케이션이 시작되면 자동으로 다음 작업이 수행됩니다:

1. 외부 API에서 사용 가능한 모든 국가 목록 조회
2. 각 국가별 2020~2025년 공휴일 데이터 수집 및 저장
3. 완료 후 API 사용 가능

⚠️ **주의**: 최초 실행 시 모든 데이터를 수집하는 데 평균 5초 이내의 시간이 소요될 수 있습니다.

## 📚 API 명세

### REST API 엔드포인트 요약

| 메서드    | 경로                   | 설명            | 주요 파라미터                                               |
|--------|----------------------|---------------|-------------------------------------------------------|
| GET    | `/holidays`          | 공휴일 검색 (페이징)  | year, countryCode, fromDate, toDate, type, page, size |
| POST   | `/holidays/sync/all` | 전체 데이터 재적재    | -                                                     |
| POST   | `/holidays/refresh`  | 특정 연도/국가 재동기화 | year, countryCode                                     |
| DELETE | `/holidays`          | 특정 연도/국가 삭제   | year, countryCode                                     |

### Swagger UI 및 OpenAPI 문서 확인

애플리케이션 실행 후 다음 URL에서 API 문서를 확인하고 테스트할 수 있습니다:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
    - 브라우저에서 접속하여 모든 API를 시각적으로 확인
    - "Try it out" 기능으로 즉시 테스트 가능
    - 요청/응답 예시 자동 표시

### 1. 공휴일 검색

```http
GET /holidays
```

**Query Parameters:**

| 파라미터        | 타입            | 필수 | 설명               | 예시                   |
|-------------|---------------|----|------------------|----------------------|
| startDate   | string(date)  | X  | 시작 날짜 (ISO 8601) | 2024-01-01           |
| endDate     | string(date)  | X  | 종료 날짜 (ISO 8601) | 2024-12-31           |
| countryCode | String        | X  | 국가 코드 (2자리)      | KR, US               |
| types       | array<string> | X  | 공휴일 타입           | PUBLIC, BANK, SCHOOL |
| page        | Integer       | X  | 페이지 번호 (0부터 시작)  | 0                    |
| size        | Integer       | X  | 페이지 크기           | 20                   |

**응답 예시:**

```json
{
  "holidays": [
    {
      "id": 1,
      "date": "2024-01-01",
      "localName": "신정",
      "name": "New Year's Day",
      "countryId": 66,
      "countryName": "South Korea",
      "types": [
        "PUBLIC"
      ]
    }
  ],
  "pageInfo": {
    "currentPage": 0,
    "pageSize": 20,
    "numberOfElements": 15,
    "totalElements": 15,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true,
    "isEmpty": false
  }
}
```

### 2. 전체 데이터 재적재

```http
POST /holidays/sync/all
```

모든 국가의 모든 연도(2020-2025) 공휴일 데이터를 재적재합니다.

**응답 예시:**

```json
{
  "totalCount": 714,
  "successCount": 714,
  "failCount": 0,
  "countryCount": 119,
  "yearRange": "2020-2025",
  "startTime": "2025-12-07T22:15:25.410331",
  "endTime": "2025-12-07T22:15:29.492224",
  "durationSeconds": 4
}
```

### 3. 재동기화 (Refresh)

```http
POST /holidays/refresh?year={year}&countryCode={countryCode}
```

특정 연도와 국가의 공휴일 데이터를 외부 API에서 다시 가져와 업데이트합니다.

**Query Parameters:**

- `year` (필수): 연도 (예: 2024)
- `countryCode` (필수): 국가 코드 (예: KR)

**응답 예시:**

```json
{
  "oldCount": 15,
  "newCount": 15,
  "actualDeletedCount": 1,
  "actualAddedCount": 12
}
```

**응답 필드 설명:**

- `oldCount`: DB에서 삭제된 레코드 수
- `newCount`: DB에 저장된 레코드 수
- `actualDeletedCount`: 실제로 삭제된 공휴일 개수
- `actualAddedCount`: 실제로 추가된 공휴일 개수

### 4. 삭제

```http
DELETE /holidays?year={year}&countryCode={countryCode}
```

특정 연도와 국가의 모든 공휴일 레코드를 삭제합니다.

**Query Parameters:**

- `year` (필수): 연도 (예: 2024)
- `countryCode` (필수): 국가 코드 (예: KR)

**응답 예시:**

```json
{
  "deletedCount": 15
}
```

## 🗄 데이터베이스 설계

### 테이블 설명

#### country

- **country_id**: 국가 고유 ID (PK, Auto Increment)
- **country_code**: 국가 코드 (2자리)
- **country_name**: 국가명
- **created_at**: 생성일자
- **updated_at**: 수정일자

#### country_holiday

- **holiday_id**: 공휴일 고유 ID (PK, Auto Increment)
- **holiday_date**: 공휴일 날짜
- **holiday_local_name**: 현지 언어 공휴일명
- **holiday_name**: 영문 공휴일명
- **country_id**: 국가 ID (FK)
- **fixed**: 고정 공휴일 여부
- **global**: 전국 공휴일 여부
- **counties**: 지역별 공휴일 정보
- **holiday_launch_year**: 공휴일 시작 연도
- **holiday_types**: 공휴일 타입 (PUBLIC, BANK, SCHOOL 등)
- **created_at**: 생성일자
- **updated_at**: 수정일자

## 🧪 테스트

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test
```

### 테스트 커버리지

- **Service Layer**: HolidayService 주요 메서드 테스트
- **Controller Layer**: REST API 엔드포인트 테스트
- **Test 항목**:
    - 공휴일 검색 기능
    - 재동기화 기능
    - 삭제 기능
    - 전체 데이터 로드 기능

## 📦 배치 자동화

### 스케줄 설정

매년 **1월 2일 01:00 KST**에 자동으로 전년도 및 금년도 데이터를 동기화합니다.

```java

@Scheduled(cron = "0 0 1 2 1 *", zone = "Asia/Seoul")
public void autoSyncHolidays() {
    // 전년도 및 금년도 공휴일 데이터 자동 동기화
}
```

### 동작 방식

1. 모든 국가 목록 조회
2. 각 국가별로:

- 전년도 데이터 재동기화
- 금년도 데이터 재동기화

3. 로그 기록 및 완료

## 🔧 설정 파일

### application.yml

주요 설정 항목:

```yaml
# 외부 API 설정
nager:
  api:
    base-url: https://date.nager.at/api/v3

# 공휴일 데이터 범위 설정
holiday:
  start-year: 2020
  end-year: 2025
```

## 📄 제출 정보

### GitHub 레포지터리

- **URL**: https://github.com/JaeY0ung/holidaykeeper-backend
- **제출 기한**: 2024년 12월 7일 23:59

### 빌드 환경

- **Java**: 21
- **Gradle**: 8.5
- **Spring Boot**: 3.4.0

### 실행 확인 방법

1. **Swagger UI에서 API 테스트**

- 브라우저에서 http://localhost:8080/swagger-ui.html 접속
- "Try it out" 기능으로 모든 API 즉시 테스트 가능

2. **H2 Console에서 데이터 확인**

- http://localhost:8080/h2-console 접속
- 실제 저장된 공휴일 데이터 SQL 쿼리로 확인

3. **테스트 실행**

```bash
   ./gradlew clean test
```

---

**플랜잇스퀘어 백엔드 개발자 채용 과제**  
작성일: 2025년 12월