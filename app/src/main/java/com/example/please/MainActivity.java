package com.example.please;

import androidx.appcompat.app.AppCompatActivity;

//android, nfc 등을 이용하려면 필요한 클래스들 import
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import java.net.URI;
import java.net.URISyntaxException;


import tech.gusavila92.websocketclient.WebSocketClient;
//****웹소켓 통신을 가능하게 해줌(매우 중요!!!)-gradle에 등록해줘야 사용가능
//https://github.com/gusavila92/java-android-websocket-client에 사용방법 나옴

public class MainActivity extends AppCompatActivity {

    TextView count; //화면에 운동 횟수 띄움

    TextView myUID; //운동기구 이름
    String nowMachine=""; //현재 기구이름 안드로이드에 임시저장
    boolean nowStatus=false; //현재 상태: 연결 상태일 경우-true,  비연결-false
    String nowCount; //현재 횟수 안드로이드에 임시저장

    //list of NFC technologies detected:
    //nfc 기능을 사용하고 구현하기 위해 필요한 기술적 세팅으로 보면 될듯
    private final String[][] techList = new String[][]{
            new String[] {

                    NfcA.class.getName(), //NFC-A (ISO 14443-3A) 프로퍼티들과 I/O 기능을 제공
                    NfcB.class.getName(), //NFC-B (ISO 14443-3B) 프로퍼티들과 I/O 기능을 제공.
                    NfcF.class.getName(), //NFC-F (JIS 6319-4) 프로퍼티들과 I/O 기능을 제공.
                    NfcV.class.getName(), //NFC-V (ISO 15693) 프로퍼티들과 I/O 기능을 제공.
                    NdefFormatable.class.getName(), //NDEF 형식으로 접근 가능한 태그들을 위한 정형화된 기능을 제공.
                    TagTechnology.class.getName(), //모든 태그 기술 클래스들이 구현해야 할 인터페이스.
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(), //만약 안드로이드 장비가 MIFARE를 지원한다면 MIFARE Classic 프로퍼티들과 I/O 에 접근할 수 있게 해줌
                    MifareUltralight.class.getName(), //만약 안드로이드 장비가 MIFARE를 지원한다면 MIFARE Ultralight 프로퍼티들과 I/O 에 접근할 수 있는 기능을 제공.
                    Ndef.class.getName() //NDEF data에 접근할 수 있도록 지원하고 NDEF 형식의 NFC tags와 연동할 수 있는 기능을 제공.
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createWebSocketClient();  //웹소켓 클라이언트 생성 및 가동(밑에 메서드 정의)

    }


    private WebSocketClient webSocketClient; //웹소켓 클라이언트 생성

    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://172.30.1.10:80/"); // "ws://ip주소:포트/"로 서버랑 연결
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                System.out.println("onOpen");
            } //연결됨

            @Override
            public void onTextReceived(String message) { //esp32서버로부터 메세지받음
                System.out.println("onTextReceived");
                System.out.println(message);
                count = (TextView) findViewById(R.id.count); //xml에서 횟수띄우는 TextView를 찾아 연결
                count.setText(message); //화면에 띄우는 텍스트 바뀜 (횟수 바뀜)
                nowCount=message; //현재 횟수 안드로이드에 저장
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                System.out.println("onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                System.out.println("onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                System.out.println("onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                System.out.println("onCloseReceived");
            }
        };

        webSocketClient.setConnectTimeout(10000); //해당 시간 안에 서버와 연결 못하면 실패
        webSocketClient.setReadTimeout(60000); //해당 시간 안에 데이터 못 받아오면 실패
        webSocketClient.addHeader("Origin", "http://developer.example.com");
        webSocketClient.enableAutomaticReconnection(5000); //자동 재연결 관련
        webSocketClient.connect(); //연결시도

    }

    @Override
    protected void onResume(){
        super.onResume();

        //역시 nfc 기능을 사용하기 위해 필요한 기술적 세팅
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        IntentFilter filter = new IntentFilter();

        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        try{ //디버깅 중에 예외처리가 필요하다하여 자체적으로 예외처리해줌
            nfcAdapter.enableForegroundDispatch(this,pendingIntent,new IntentFilter[]{filter},this.techList);
        }
        catch (NullPointerException e){

        }

    }
    @Override
    protected void onNewIntent(Intent intent){ //태그된 정보 새로 받아옴
        super.onNewIntent(intent);
        if(intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) { //태그된 정보를 넘겨받음
            myUID = (TextView) findViewById(R.id.machine); //운동기구 이름 띄우는 TextView 찾아 연결
            myUID.setText(ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID))); //트리거 이름 받아서 화면에 띄움
            webSocketClient.send(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)); //서버로 트리거 정보 전송
        }

    }

    private String ByteArrayToHexString(byte []array) {
        int i,j,in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out="";

        for(j = 0;j<array.length;++j) //nfc 태그 받아온 걸 String형으로 바꿔줌
        {
            in = (int) array[j] & 0xff;
            i = (in>>4) & 0x0f;
            out += hex[i];
            i = in& 0x0f;
            out += hex[i];
        }
        switch (out) { //태그별로 트리거(별칭) 지정
            case "042AA0627B7280":
                out = "Letpulldown1";
                break;
            case "0426A0627B7280":
                out = "Letpulldown2";
                break;
            case "0422A0627B7280":
                out = "Letpulldown3";
                break;
            case "041EA0627B7280":
                out = "Letpulldown4";
                break;
            default:
                out = "This is not mine";
                break;
        }
        if(nowMachine.equals(out)){ //같은 nfc태그를 두번 태그한 경우
            //->연결 종료, 현상태 false
            nowStatus=false;
            webSocketClient.close();
        }
        if(!nowMachine.equals(out)&& nowStatus){ //사용 중인 nfc태그가 있는 상태에서 다른 nfc를 태그한 경우
            //즉 현재 운동기구(nowMachine)과 태그한 기구정보(out)이 같고, 현재 사용 중(true)인 경우

            webSocketClient.send("reconnect"); //reconnect 신호를 서버에 보냄->서버 측 코드에서 기구정보를 다시 읽을 준비함
            webSocketClient.send(out); //서버에 새로 인식한 기구정보를 보냄
            count = (TextView) findViewById(R.id.count);
            count.setText(""); //횟수 초기화
        }
        nowMachine=out; //태그한 기구 트리거정보 안드로이드에 저장
        nowStatus=true; //현재 사용 중(true)
        return out; //기구 트리거 반환
    }

}