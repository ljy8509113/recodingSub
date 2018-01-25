package play.and.eat.com.recording;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;
import play.and.eat.com.recording.play.and.eat.com.recording.listener.SettingListener;

public class MainActivity extends Activity implements SettingListener {

//    DevicePolicyManager _devicePolicyManager;
    RecodingFragment _frameRecode;

    private String _ip = "xxx.xxx.xxx.xxx"; // IP
    private int _port = 9999; // PORT번호
    SharedPreferences _pref;
    String _userName;
    boolean _isTeacher = false;
    String _uuid = null;
    int _ftpPort = 21;

    KeepAliveService mService = null;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;

    private static final String TAG = "MainActivity";
    private static final String FRAGMENT_DIALOG = "dialog";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

//        _devicePolicyManager = (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
//        ComponentName componentName = new ComponentName(getApplicationContext(), ShutdownConfigAdminReceiver.class);
//
//        Log.d("lee", "checkAdmin " + _devicePolicyManager.isAdminActive(componentName));
//        if (!_devicePolicyManager.isAdminActive(componentName)) {
//            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
//            startActivityForResult(intent, 0);
//        }else{
//            checkPermission();
//        }

        checkPermission();

    }

    void checkPermission(){
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }

        init();
    }

    void init(){
        _frameRecode = new RecodingFragment();//RecodingFragment.newInstance(this, this);
        _frameRecode._listener = this;
        _frameRecode._activity = this;
        getFragmentManager().beginTransaction().replace(R.id.container, _frameRecode).commit();

        Intent Service = new Intent(MainActivity.this, KeepAliveService.class);
        bindService(Service, mConnection, Context.BIND_AUTO_CREATE);


    }

    //서비스 커넥션 선언.
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            KeepAliveService.MainServiceBinder binder = (KeepAliveService.MainServiceBinder) service;
            mService = binder.getService(); //서비스 받아옴
            mService.registerCallback(mCallback); //콜백 등록
            Log.d("lee", "onServiceConnected");
            setInfo();
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

                    JSONObject data = new JSONObject();
                    try {
                        data.put("identifier", "recode");
                        data.put("device_id", _uuid);
                        Log.d("lee - ", "req : "+data.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mService.requestApi(data);

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
                    JSONObject data = new JSONObject();
                    try {
                        data.put("identifier", "stop");
                        data.put("device_id", _uuid);
                        Log.d("lee - ", "req : "+data.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mService.requestApi(data);
                }else if(obj.getString("id").equals("file")){
                    if(mService.isSending) {
                        Log.d("lee - ", "파일 전송중 ");
                    }else {
                        mService.sendFiles(Common.getMoviePath());
                    }
                }else if(obj.getString("identifier").equals("error")){
                    Toast.makeText(MainActivity.this, obj.getString("msg"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    void setInfo(){
        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
        _ip = _pref.getString(Common.IP_KEY, "");
        _port = _pref.getInt(Common.PORT_KEY, 0);
        _userName = _pref.getString(Common.NAME_KEY, "NoName");
        _isTeacher = _pref.getBoolean(Common.IS_TEACHER_KEY, false);
        _uuid = _frameRecode.getUUID(this);
        _frameRecode._userName = _userName;
        _ftpPort = _pref.getInt(Common.FTP_PORT, 21);

        Log.d("lee - ", "uuid : " + _uuid);
        connection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermission();
    }

//    public void offScreen() {
//        _devicePolicyManager.lockNow();
//    }

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


    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
//            new RecodingFragment.ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
            new MainActivity.ConfirmationDialog().show(getFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                boolean isSuccess = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        MainActivity.ErrorDialog.newInstance(getString(R.string.permission_request)).show(getFragmentManager(), FRAGMENT_DIALOG);
                        isSuccess = false;
                        break;
                    }
                }

                if (isSuccess) {
                    init();
                }
            } else {
                MainActivity.ErrorDialog.newInstance(getString(R.string.permission_request)).show(getFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(), VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getActivity().finish();
                                }
                            })
                    .create();
        }
    }

    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static MainActivity.ErrorDialog newInstance(String message) {
            MainActivity.ErrorDialog dialog = new MainActivity.ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }
    }
}
