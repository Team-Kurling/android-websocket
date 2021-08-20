package com.example.please;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity {

    private WebSocketClient mWebSocketClient;
    TextView count;
    TextView machine;
    TextView myUID;
    String nowMachine="";
    boolean nowStatus=false;
    String nowCount;

    //list of NFC technologies detected:
    private final String[][] techList = new String[][]{
            new String[] {

                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    NdefFormatable.class.getName(),
                    TagTechnology.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(),
                    Ndef.class.getName()
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createWebSocketClient();

    }


    private WebSocketClient webSocketClient;

    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://172.30.1.10:80/");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                System.out.println("onOpen");
            }

            @Override
            public void onTextReceived(String message) {
                System.out.println("onTextReceived");
                System.out.println(message);
                count = (TextView) findViewById(R.id.count);
                count.setText(message);
                nowCount=message;
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

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.addHeader("Origin", "http://developer.example.com");
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();

    }

    @Override
    protected void onResume(){
        super.onResume();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        IntentFilter filter = new IntentFilter();

        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        try{
            nfcAdapter.enableForegroundDispatch(this,pendingIntent,new IntentFilter[]{filter},this.techList);
        }
        catch (NullPointerException e){

        }

    }
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        if(intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            myUID = (TextView) findViewById(R.id.machine);
            myUID.setText(ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            webSocketClient.send(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
        }

    }

    private String ByteArrayToHexString(byte []array) {
        int i,j,in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out="";

        for(j = 0;j<array.length;++j)
        {
            in = (int) array[j] & 0xff;
            i = (in>>4) & 0x0f;
            out += hex[i];
            i = in& 0x0f;
            out += hex[i];
        }
        switch (out) {
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
        if(nowMachine.equals(out)){
            nowStatus=false;
            webSocketClient.close();
        }
        nowMachine=out;
        nowStatus=true;
        return out;
    }

}