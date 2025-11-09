----

# `shim` - ISFX 백엔드

## 1\. Tech Stack (기술 스택)

  * **Language**: `Java 21`
  * **Framework**: `Spring Boot 3.5.7`
  * **Build**: `Gradle (Wrapper)`
  * **DB**: `MySQL 8.0.43`
  * **API Docs**: `Springdoc OpenAPI (Swagger UI)`

## 2\. Prerequisites (필수 환경)

  * `JDK 21` (Temurin 권장)
  * `MySQL 8.0.43`
  * `Git`

## 3\. Project Structure (프로젝트 구조)

`shim` 프로젝트는 \*\*계층형 패키지 구조(Layered Architecture)\*\*를 따릅니다.

```
com.isfx.shim
 ├─ ShimApplication.java  (메인)
 ├─ controller  # API 엔드포인트
 ├─ service     # 핵심 비즈니스 로직
 ├─ repository  # DB 연동 (JPA)
 ├─ entity      # DB 테이블 매핑 엔티티
 ├─ dto         # 데이터 전송 객체 (Request/Response)
 ├─ config      # Spring Security, WebConfig 등 설정
 └─ global      # 예외 처리, 공용 유틸, JWT 로직 등
     ├─ exception
     ├─ util
     └─ security
```

## 4\. Environment (개발 환경)

  * **기본 포트**: `8080`
  * **프로필**: `dev` / `prod`
      * `application.yml` (공통) : `spring.profiles.active=dev` (기본값)
      * `application-dev.yml` (개발용)
      * `application-prod.yml` (운영용)
  * **MySQL DB 생성**:
    ```sql
    CREATE DATABASE IF NOT EXISTS shim
      DEFAULT CHARACTER SET utf8mb4
      COLLATE utf8mb4_0900_ai_ci;
    ```

## 5\. Configuration (JPA 설정)

  * **개발 (dev)**:
      * `spring.jpa.hibernate.ddl-auto=update`
      * `spring.sql.init.mode=never` (SQL 스크립트 자동 실행 끔)
  * **운영 (prod)**:
      * `spring.jpa.hibernate.ddl-auto=validate`
      * (권장) `Flyway` 또는 `Liquibase`를 통한 DB 마이그레이션 관리

## 6\. How to Run (Local) (실행 방법)

1.  IntelliJ에서 프로젝트 열기 → Gradle 자동 동기화
2.  **Gradle 설정 확인**
      * `Build and run using`: `Gradle`
      * `Run tests using`: `Gradle`
      * `Distribution`: `Wrapper`
      * `Gradle JVM`: `JDK 21`
3.  `ShimApplication` 실행

## 7\. Git Workflow (Git 협업 방식)

  * **기본 브랜치**: `main`

  * **작업 브랜치 규칙**: `feature/<기능>-<본인이름>`

      * 예) `feature/login-jinsu`, `feature/post-jinsu`

  * **작업 흐름**:

    ```bash
    # 1. 작업 브랜치 생성 및 이동
    git checkout -b feature/login-jinsu

    # (작업 수행)

    # 2. 커밋
    git add .
    git commit -m "[ADD] 로그인 API 추가"

    # 3. 빌드 테스트 (필수)
    ./gradlew clean build

    # 4. 푸시
    git push -u origin feature/login-jinsu
    ```

  * **PR (Pull Request) 규칙**:

      * **`main` 브랜치에 직접 PUSH 금지 → PR로만 병합**
      * **`.gitignore` 관리 철저**: PUSH 전, 민감한 파일(예: `application-prod.yml`, `application-prod.yml`)이 `.gitignore`에 꼭 추가되었는지 확인합니다.
      * PR 제목 예시:
          * `feat(auth): implement login API`
          * `fix(study): null check on comment`
      * PR 본문에 변경 요약 / 테스트 방법 / 관련 이슈 포함
      * CI에서 `./gradlew clean build` 통과 필수

  * **원격 `main`과 충돌 시**:

      * `git pull origin main` (또는 `rebase`)으로 `main`의 최신 변경사항을 가져와 충돌 해결 후 PUSH

## 8\. Commit Convention (커밋 메시지)

  * `[INIT]`: 초기 세팅 (최초 1회)
  * `[HOTFIX]`: 배포 중 긴급 수정
  * `[ADD]`: 기능 추가
  * `[FIX]`: 버그 수정
  * `[REFACTOR]`: 리팩토링 (로직 수정 X, 코드 개선)
  * `[DOCS]`: 문서 수정 (README 등)
  * `[CHORE]`: 빌드, 설정 등 기타 작업
  * **예시**: `[ADD] 회원가입 API 추가`, `[FIX] 로그인 토큰 만료 오류 수정`

## 9\. DTO Naming (DTO 명명 규칙)

  * **엔티티명 + 행위(CRUD/Get) + 형태(Req/Res) + Dto**
  * (회원가입): `UserCreateReqDto`, `UserCreateResDto`
  * (스터디 조회): `StudyGetReqDto`, `StudyGetResDto`
  * **비CRUD (예: 로그인)**: `AuthLoginReqDto`, `AuthLoginResDto`
