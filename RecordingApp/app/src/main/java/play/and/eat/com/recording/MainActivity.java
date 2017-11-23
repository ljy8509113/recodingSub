package play.and.eat.com.recording;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends Activity {

    DevicePolicyManager _devicePolicyManager;
    RecodingFragment _frameRecode;

    private Handler mHandler;

    private Socket socket;

    private BufferedReader networkReader;
    private BufferedWriter networkWriter;

    private String ip = "xxx.xxx.xxx.xxx"; // IP
    private int port = 9999; // PORT번호
    private String html = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (null == savedInstanceState) {
            _frameRecode = RecodingFragment.newInstance(this);
            getFragmentManager().beginTransaction().replace(R.id.container, _frameRecode).commit();
        }

        _devicePolicyManager= (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = new ComponentName(getApplicationContext(), ShutdownConfigAdminReceiver.class);

        if(!_devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
        }

        connection();
    }

    public void offScreen(){
        _devicePolicyManager.lockNow();
    }

    public void startRecoding(){
        _frameRecode.startRecordingVideo();
    }

    public void stopRecoding(){
        _frameRecode.stopRecordingVideo();
    }

    public void connection(){
        SharedPreferences pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
        String ip = pref.getString(Common.IP_KEY, "");
        int port = pref.getInt(Common.PORT_KEY, 0);

        if(ip.equals("")){
            Toast.makeText(this, "서버와의 연결이 필요합니다.", Toast.LENGTH_LONG).show();
        }else{
            mHandler = new Handler();
            try {
                setSocket(ip, port);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            checkUpdate.start();
        }
    }

    private Thread checkUpdate = new Thread() {

        public void run() {
            try {
                String line;
                Log.w("ChattingStart", "Start Thread");
                while (true) {
                    Log.w("Chatting is running", "chatting is running");
                    line = networkReader.readLine();
                    html = line;
                    mHandler.post(showUpdate);
                }
            } catch (Exception e) {

            }
        }
    };

    private Runnable showUpdate = new Runnable() {

        public void run() {
            Toast.makeText(MainActivity.this, "Coming word: " + html, Toast.LENGTH_SHORT).show();
        }

    };

    public void setSocket(String ip, int port) throws IOException {

        try {
            socket = new Socket(ip, port);
            networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg){
        PrintWriter out = new PrintWriter(networkWriter, true);
        out.println(msg);
    }
}
