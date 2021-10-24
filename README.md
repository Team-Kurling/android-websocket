# android_websocket

1. esp32 유닛을 컴퓨터에 연결. https://github.com/Team-Kurling/knurling-main-unit 에 있는 esp32.ino 를 컴파일
2. app/java/MainActivity 의 73번째 줄  "ws://ip주소:포트/" 를 본인에 맞는 ip주소와 포트로 변경
3. 앱이 실행되면 트리거인 nfc 스티커를 태그
  
    3-1. 트리거가 아닌 다른 nfc 스티커를 태그 했을 때는 횟수가 초기화
  
    3-2. 같은 nfc 스티커를 한 번 더 태그할 시, 연결이 disconnected
  
4. IR 센서에서 거리 값을 다르게 해보면서 테스트
