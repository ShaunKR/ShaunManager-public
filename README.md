# ShaunManager — 공개용 근태 자동화 예제

Java 8 / Spring Boot 2.7.3 / Gradle 7.6.6 기반 모놀리식 프로젝트입니다.
달력 프론트, 날짜별 근무 계획, 출퇴근 등록 연동 구조, 암호화 설정, 단계별 진단과 선택적 Telegram 알림을 포함합니다.

이 저장소는 운영 저장소에서 소스만 추출한 공개용 사본입니다. 운영 Git 이력, 인증 설정, 키, DB, 로그와 배포 파일은 포함하지 않습니다.
회사 서버 주소와 회사 식별자는 `example.invalid` 및 `demo-company`로 대체했으며 근무 장소는 예시입니다.
실제 ERP에 바로 연결되는 배포본이 아닙니다. ERP 연동 경로·프로토콜은 구현 예제로 남아 있습니다.

## 빌드와 테스트

Java 8을 지정한 환경에서:

```sh
bash gradlew test bootJar
```

테스트는 모의 ERP 응답과 테스트용 데이터를 사용합니다.
산출물은 `build/libs/shaun-manager.jar`입니다.

## 로컬 실행

개별 환경에서 `SHAUN_ACCESS_TOKEN`을 설정한 후 실행합니다. 공유 기본 토큰은 없습니다.
토큰이 비어 있으면 인증 요청은 거절됩니다. 기본 수신 주소는 `127.0.0.1:9091`입니다.

```sh
java -jar build/libs/shaun-manager.jar --shaun.scheduler.enabled=false
```

`/calendar.html`에서 달력을 사용하며 API에는 `X-Access-Token` 헤더를 전달합니다.
ERP 설정은 `config/amaranth-login.enc`, 복호화 키는 `SHAUN_CONFIG_KEY` 환경변수로 제공하는 구조입니다.
기존 암호화 설정을 옮길 때는 기존 키를 사용해야 합니다. 이 사본은 키를 생성하거나 포함하지 않습니다.
실제 연동에는 권한 있는 환경에서 `AMARANTH_DISCOVERY_URL`, `AMARANTH_GATEWAY_URL`, `AMARANTH_MOBILE_ID`와 로그인 설정을 별도 구성해야 합니다.

## 주요 기능

- 달력에서 근무 구분, 근무 장소, 주간/야간조, 출근·퇴근 실행 제외 설정
- 주간 정상근무 자동 등록: 출근 08:15~08:25, 퇴근 18:25~18:35
- 야간 정상근무 자동 등록: 출근 13:45~13:55, 퇴근 23:00~23:05; 반차 미지원
- H2 파일 DB에 계획과 실제 기록 저장; 달력 조회는 로컬 기록 사용
- 실행 전 상태 저장, 결과가 불확실하면 자동 재전송하지 않음
- `POST /api/system/check`: 설정·로그인·조회·DB 읽기 진단; 출퇴근 등록과 DB 쓰기는 수행하지 않음
- 실제 등록 API: `POST /api/amaranth/attendance/check-in`, `check-out`
- 선택적 Telegram 알림과 로켓 미션 예제; 기본 비활성화

## 공개 범위

원본 저장소의 커밋·작성자 이력과 원격 연결을 가져오지 않았습니다.
프로젝트 이름과 코드 네임스페이스는 유지했습니다. 날짜와 테스트 문자열은 예제 데이터입니다.
운영 계정·환경 설정을 추가한 파일은 커밋하지 마세요.
공개용 소스 변경은 기존 운영 서버나 기존 저장소에 반영되지 않습니다.
