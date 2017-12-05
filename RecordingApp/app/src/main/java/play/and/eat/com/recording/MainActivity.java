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

import play.and.eat.com.recording.play.and.eat.com.recording.listener.SettingListener;

public class MainActivity extends Activity implements SettingListener{

    DevicePolicyManager _devicePolicyManager;
    RecodingFragment _frameRecode;

    private Handler mHandler;

    private Socket socket;

    private BufferedReader networkReader;
    private BufferedWriter networkWriter;

    private String _ip = "xxx.xxx.xxx.xxx"; // IP
    private int _port = 9999; // PORT번호
    private String html = "";
    SharedPreferences _pref;
    String _userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _devicePolicyManager= (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (null == savedInstanceState) {
            _frameRecode = RecodingFragment.newInstance(this, this);
            getFragmentManager().beginTransaction().replace(R.id.container, _frameRecode).commit();
        }
        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
        _ip = _pref.getString(Common.IP_KEY, "");
        _port = _pref.getInt(Common.PORT_KEY, 0);
        _userName = _pref.getString(Common.NAME_KEY,"No Name");
        connection();
    }

    public void checkAdmin(){
        ComponentName componentName = new ComponentName(getApplicationContext(), ShutdownConfigAdminReceiver.class);

        if(!_devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
        }
    }

    public void offScreen(){
        _devicePolicyManager.lockNow();
    }

    public void connection(){
        if(_ip.equals("")){
            Toast.makeText(this, "서버와의 연결이 필요합니다.", Toast.LENGTH_LONG).show();
        }else{
            mHandler = new Handler();
            startSocket.start();
            checkUpdate.start();
        }
    }

    private Thread startSocket = new Thread(){
        public void run(){
            try {
                setSocket(_ip, _port);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    };

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

    @Override
    public void onSaved(String ip, int port, String userName) {
        Log.d("lee - ", ip + " : "+port+" // "+userName);

        SharedPreferences.Editor edit = _pref.edit();
        String saveIp = _pref.getString(Common.IP_KEY, "");
        int savePort = _pref.getInt(Common.PORT_KEY, 0);

        boolean isRestartSocket = false;
        if(!ip.equals(saveIp)) {
            isRestartSocket = true;
            edit.putString(Common.IP_KEY, ip);
        }
        if(port != savePort){
            isRestartSocket = true;
            edit.putInt(Common.PORT_KEY, port);
        }

        edit.putString(Common.NAME_KEY, userName);
        edit.commit();

        _ip = ip;
        _port = port;
        _userName = userName;

        if(isRestartSocket){
            startSocket.interrupt();
            checkUpdate.interrupt();
            connection();
        }

        Toast.makeText(this, "저장완료 : "+ip, Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startSocket.interrupt();
        checkUpdate.interrupt();
    }
}
