package play.and.eat.com.recording;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import play.and.eat.com.recording.play.and.eat.com.recording.listener.SettingListener;

public class MainActivity extends Activity implements SettingListener {

    DevicePolicyManager _devicePolicyManager;
    RecodingFragment _frameRecode;

    private String _ip = "xxx.xxx.xxx.xxx"; // IP
    private int _port = 9999; // PORT번호
    SharedPreferences _pref;
    String _userName;
    boolean _isTeacher = false;
    String _uuid = null;
    int _ftpPort = 21;

    KeepAliveService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _devicePolicyManager = (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (null == savedInstanceState) {
            _frameRecode = RecodingFragment.newInstance(this, this);
            getFragmentManager().beginTransaction().replace(R.id.container, _frameRecode).commit();
        }

        Intent Service = new Intent(this, KeepAliveService.class);
        bindService(Service, mConnection, Context.BIND_AUTO_CREATE);
    }


    //서비스 커넥션 선언.
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            KeepAliveService.MainServiceBinder binder = (KeepAliveService.MainServiceBinder) service;
            mService = binder.getService(); //서비스 받아옴
            mService.registerCallback(mCallback); //콜백 등록
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    //서비스에서 아래의 콜백 함수를 호출하며, 콜백 함수에서는 액티비티에서 처리할 내용 입력
    private KeepAliveService.ICallback mCallback = new KeepAliveService.ICallback() {
        public void recvData(String result) {
            //처리할 일들..
            try {
                JSONObject obj = new JSONObject(result);
                Log.d("lee - ", "callback : " + obj.toString());
                if (obj.getString("id").equals("recode")) {
                    Log.d("녹화 == ", "1");
                    if (!_frameRecode.isRecording()){
                        runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _frameRecode.startRecordingVideo();
                        }
                    });
                    }

                } else if (obj.getString("id").equals("stop")) {
                    Log.d("중지 == ", "2");
                    if (_frameRecode.isRecording()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                _frameRecode.stopRecordingVideo();
                            }
                        });
                    }
                }else if(obj.getString("id").equals("file")){
                    if(mService.isSending) {
                        Log.d("lee - ", "파일 전송중 ");
                    }else {
                        mService.sendFiles(Common.getRootPath());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public void checkAdmin() {
        ComponentName componentName = new ComponentName(getApplicationContext(), ShutdownConfigAdminReceiver.class);

        if (!_devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
        }

        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
        _ip = _pref.getString(Common.IP_KEY, "");
        _port = _pref.getInt(Common.PORT_KEY, 0);
        _userName = _pref.getString(Common.NAME_KEY, "No Name");
        _isTeacher = _pref.getBoolean(Common.IS_TEACHER_KEY, false);
        _uuid = _frameRecode.getUUID(this);
        _frameRecode._userName = _userName;
        _ftpPort = _pref.getInt(Common.FTP_PORT, 21);

        Log.d("lee - ", "uuid : " + _uuid);
        connection();
    }

    public void offScreen() {
        _devicePolicyManager.lockNow();
    }

    public void connection() {
        if (_ip.equals("")) {
            Toast.makeText(this, "서버와의 연결이 필요합니다.", Toast.LENGTH_LONG).show();
        } else {
            mService.myServiceFunc(_ip, _port, _userName, _uuid, _isTeacher, _ftpPort);
        }
    }

    @Override
    public void onSaved(String ip, int port, String userName, boolean isTeacher, int ftpPort) {
        Log.d("lee - ", ip + " : " + port + " // " + userName);

        SharedPreferences.Editor edit = _pref.edit();
        String saveIp = _pref.getString(Common.IP_KEY, "");
        int savePort = _pref.getInt(Common.PORT_KEY, 0);

        _ip = ip;
        _port = port;
        _userName = userName;
        _isTeacher = isTeacher;
        _frameRecode._userName = userName;

        if(_ftpPort != ftpPort) {
            _ftpPort = ftpPort;
            edit.putInt(Common.FTP_PORT,ftpPort);
            edit.commit();
        }

        if (!ip.equals(saveIp) || port != savePort || !userName.equals(_userName) || isTeacher != _isTeacher) {
            edit.putInt(Common.PORT_KEY, port);
            edit.putString(Common.IP_KEY, ip);
            edit.putBoolean(Common.IS_TEACHER_KEY, isTeacher);
            edit.putString(Common.NAME_KEY, userName);
            edit.commit();

            connection();
        }
        Toast.makeText(this, "저장완료 : " + ip, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, KeepAliveService.class);
        stopService(intent);
        unbindService(mConnection);
    }

    public void sendMsg(){
        JSONObject data = new JSONObject();
        try {
            data.put("identifier", "user_info");
            data.put("user", _isTeacher ? "T" : "S");
            data.put("name", _userName);
            if (_uuid != null)
                data.put("device_id", _uuid);
            Log.d("lee - ", "connectionSuccdee : "+data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mService.requestApi(data);
    }
}
