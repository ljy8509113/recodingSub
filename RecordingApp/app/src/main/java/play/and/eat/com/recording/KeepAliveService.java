package play.and.eat.com.recording;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import play.and.eat.com.recording.data.SettingData;
import play.and.eat.com.recording.play.and.eat.com.recording.listener.FileDownloadListener;
import play.and.eat.com.recording.play.and.eat.com.recording.listener.TCPClientListener;


/**
 * Created by ljy on 2017-12-09.
 */

public class KeepAliveService extends Service implements TCPClientListener, FileDownloadListener {
//    public String _ip = "";
//    public int _port = 0;
//    TCPClient _client = null;
//    String _userName;
//    boolean _isTeacher = false;
//    String _uuid = null;
//    int _ftpPort = 21;

    int _progress = 0;
//    SharedPreferences _pref;
//    String _ftpId = "";
//    String _ftpPw = "";

    boolean isSending = false;

    FileUpLoad _fileUpload = null;

    @Override
    public void progress(final String fileName, int persent) {
        if(_progress < persent){
            _progress = persent;
            JSONObject data = new JSONObject();
            try {
                data.put("identifier", "progress");
                data.put("persent", _progress+"");
                data.put("current", (sendIndex+1)+"");
                data.put("max", _list.length+"");
                data.put("name", fileName);
                data.put("device_id", SettingData.Instance().uuid);
                TCPClient.Instance().WriteCommand(data.toString());
//                        Log.d("lee - ", "connectionSuccdee : "+data.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void downLoadComplate(String fileName) {
        _progress = 0;
        JSONObject data = new JSONObject();
        try {
            data.put("identifier", "downEnd");
            data.put("max", _list.length + "");
            data.put("current", (sendIndex+1) + "");
            data.put("device_id", SettingData.Instance().uuid);
            data.put("name", fileName);
            TCPClient.Instance().WriteCommand(data.toString());
//            Log.d("lee - ", "connectionSuccdee : " + data.toString());
//            isSending = false;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(_list[sendIndex].delete()){
            sendIndex++;
            if(_list.length == sendIndex){
                _list = null;
                _fileUpload.close();
            }else{
                sendFile();
            }
        }
    }

     @Override
     public void downLoadFail(String msg){
         _progress = 0;
         sendIndex++;
         if(_list.length == sendIndex){
             _list = null;
             _fileUpload.close();
         }else{
             sendFile();
         }
     }

    //서비스 바인더 내부 클래스 선언
    public class MainServiceBinder extends Binder {
        KeepAliveService getService() {
            return KeepAliveService.this; //현재 서비스를 반환.
        }
    }

    private final IBinder mBinder = new MainServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("서비스호출", "onStartCommand()실행됨");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(TCPClient.Instance().isConnected())
            TCPClient.Instance().Disconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void connectionSuccess() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
//        _ftpId = _pref.getString(Common.FTP_ID,"");
//        _ftpPw = _pref.getString(Common.FTP_PW,"");

        JSONObject data = new JSONObject();
        try {
            data.put("identifier", "user_info");
            data.put("user", SettingData.Instance().isTeacher ? "T" : "S");
            data.put("name", SettingData.Instance().name);
            if (!SettingData.Instance().uuid.equals(""))
                data.put("device_id", SettingData.Instance().uuid);
            TCPClient.Instance().WriteCommand(data.toString());
            Log.d("lee - ", "connectionSuccdee : "+data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiver(String result) {
        Log.d("lee - ", "onReceiver : " + result);
        mCallback.recvData(result);
    }

    @Override
    public void connectionSnap() {
        TCPClient.Instance().Connect(SettingData.Instance().ip, SettingData.Instance().port, this, SettingData.Instance().uuid);
    }

    @Override
    public void error(String msg) {
        JSONObject data = new JSONObject();
        try {
            data.put("identifier", "error");
            data.put("msg", msg);
            Log.d("lee","error recvData");
            mCallback.recvData(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //콜백 인터페이스 선언
    public interface ICallback {
        public void recvData(String result); //액티비티에서 선언한 콜백 함수.
    }

    private ICallback mCallback;

    //액티비티에서 콜백 함수를 등록하기 위함.
    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    //액티비티에서 서비스 함수를 호출하기 위한 함수 생성
    public void myServiceFunc(){
        //서비스에서 처리할 내용
//        _ip = ip;
//        _port = port;
//        _userName = userName;
//        _uuid = uuid;
//        _isTeacher = isTeacher;
//        _ftpPort = ftpPort;
        Log.d("lee - ", "myServiceFunc");

        TCPClient.Instance().Connect(SettingData.Instance().ip, SettingData.Instance().port, this, SettingData.Instance().uuid);

    }

    public void requestApi(JSONObject obj){
        TCPClient.Instance().WriteCommand(obj.toString());
    }

    String _filePath = null;
    File _list [] = null;
    int sendIndex = 0;

    public void sendFiles(String path){
        _filePath = path;
        File f = new File(path);
        _list = f.listFiles();

        if(_list != null && _list.length > 0){
            _fileUpload = new FileUpLoad(SettingData.Instance().ip, SettingData.Instance().ftpPort, SettingData.Instance().ftpId, SettingData.Instance().ftpPw, "utf-8", "./", this);
            _fileUpload.login();
            isSending = true;
            sendIndex = 0;

            sendFile();
        }else{
            downLoadComplate("");
        }

    }

    void sendFile(){
            //_client.WriteData(_list[sendIndex].getPath());
            if(SettingData.Instance().ftpId.equals("") || SettingData.Instance().ftpPw.equals("")){
                Toast.makeText(getApplicationContext(), "FTP ID 또는 PASSWORD 가 없습니다.", Toast.LENGTH_LONG).show();
            }else {
                _fileUpload.uploadFile(_list[sendIndex]);
            }
    }

}
