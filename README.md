# LUMI란?

LUMI(루미)란 음성으로 호텔 객실을 제어하기 위한 안드로이드 어플리케이션입니다.

물론, 상용으로 출시하는 어플리케이션은 아니며 기능에 제약이 있습니다.

본 프로젝트의 시스템 구성은 Github에 업로드 된 LUMI 어플리케이션 이 외에도 관리 서버, 호텔 객실마다 존재하는 객실 제어 유닛들이 존재합니다.



## 주요 기능

1. 호텔 예약
2. 로그인
3. 음성 제어
   - 제어 가능 기기: TV, 조명, 에어컨

- 사용자가 로그인을 하게 되면 관리 서버와의 통신을 통해 예약된 방 정보를 수신 
- 이후 음성 제어 기능을 이용하게 되면 로그인 시 전달받은 방 정보를 바탕으로 예약된 호텔 객실에 존재하는 객실 제어 유닛과의 통신을 통해 객실을 제어



## 주요 소스코드 설명

1. 자연어 처리: NaturalLanguageProcessor.java, UserCommand.java
   - 음성 인식된 문장에서 제어할 개체와 명령을 분리하여 HashMap으로 저장 후 사용



## 사용 라이브러리

1. 음성인식(STT): 네이버 Open API - Clova Speech Recognition API
2. Ripple 효과: https://github.com/skyfishjy/android-ripple-background
3. Shimmer 효과: https://github.com/RomainPiel/Shimmer-android
4. 뷰 바인딩: https://github.com/JakeWharton/butterknife