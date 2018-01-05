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

import play.and.eat.com.recording.play.and.eat.com.recording.listener.FileDownloadListener;
import play.and.eat.com.recording.play.and.eat.com.recording.listener.TCPClientListener;


/**
 * Created by ljy on 2017-12-09.
 */

public class KeepAliveService extends Service implements TCPClientListener, FileDownloadListener {
    public String _ip = "";
    public int _port = 0;
    TCPClient _client = null;
    String _userName;
    boolean _isTeacher = false;
    String _uuid = null;
    int _ftpPort = 21;

    int _progress = 0;
    SharedPreferences _pref;
    String _ftpId = "";
    String _ftpPw = "";

    boolean isSending = false;

    @Override
    public void progress(String fileName, int persent) {
        if(_progress < persent){
            _progress = persent;
            JSONObject data = new JSONObject();
            try {
                data.put("identifier", "progress");
                data.put("persent", _progress+"");
                data.put("current", (sendIndex+1)+"");
                data.put("max", _list.length+"");
                data.put("name", fileName);
                data.put("device_id", _uuid);
                _client.WriteCommand(data.toString());
                Log.d("lee - ", "connectionSuccdee : "+data.toString());
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
            data.put("device_id", _uuid);
            data.put("name", fileName);
            _client.WriteCommand(data.toString());
            Log.d("lee - ", "connectionSuccdee : " + data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
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
        if(_client != null && _client.isConnected())
            _client.Disconnect();
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

        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
        _ftpId = _pref.getString(Common.FTP_ID,"");
        _ftpPw = _pref.getString(Common.FTP_PW,"");

        JSONObject data = new JSONObject();
        try {
            data.put("identifier", "user_info");
            data.put("user", _isTeacher ? "T" : "S");
            data.put("name", _userName);
            if (_uuid != null)
                data.put("device_id", _uuid);
            _client.WriteCommand(data.toString());
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
        if(_client == null)
            _client = new TCPClient();

        _client.Connect(_ip, _port, this, _uuid);
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
    public void myServiceFunc(String ip, int port, String userName, String uuid, boolean isTeacher, int ftpPort){
        //서비스에서 처리할 내용
        _ip = ip;
        _port = port;
        _userName = userName;
        _uuid = uuid;
        _isTeacher = isTeacher;
        _ftpPort = ftpPort;
        Log.d("lee - ", "myServiceFunc");
        if(_client == null){
            _client = new TCPClient();
            _client.Connect(_ip, _port, this, _uuid);
        }
    }

    public void requestApi(JSONObject obj){
        _client.WriteCommand(obj.toString());
    }

    String _filePath = null;
    File _list [] = null;
    int sendIndex = 0;

    public void sendFiles(String path){
        _filePath = path;
        File f = new File(path);
        _list = f.listFiles();

//        for(File file : _list ) {
//            int endIndex = file.getName().indexOf(".");
//            String name = file.getName().substring(0, endIndex);
//            String ext = file.getName().substring(endIndex, file.getName().length());
//
//            if (!ext.equalsIgnoreCase("zip")) {
//                File reNameFile = new File(path + name + ".zip");
//                file.renameTo(reNameFile);
//            }
//        }
//
//        _list = f.listFiles();
        sendFile();
    }

    void sendFile(){
        if(_list != null && _list.length > 0){
            //_client.WriteData(_list[sendIndex].getPath());
            if(_ftpId.equals("") || _ftpPw.equals("")){
                Toast.makeText(getApplicationContext(), "FTP ID 또는 PASSWORD 가 없습니다.", Toast.LENGTH_LONG).show();
            }else{
                FileUpLoad fileUpload = new FileUpLoad(_ip, _ftpPort, _ftpId, _ftpPw, "utf-8", "./", this);
                fileUpload.login();
                isSending = true;
                sendIndex = 0;

                for(File f : _list){
                    if(fileUpload.uploadFile(f)){
                        sendIndex++;
                        f.delete();
                    }
                }
               _list = null;
                fileUpload.close();
            }

        }else{
            downLoadComplate("");
        }
    }

}
