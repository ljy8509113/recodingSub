package play.and.eat.com.recording;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import play.and.eat.com.recording.play.and.eat.com.recording.listener.TCPClientListener;

/**
 * Created by ljy on 2017-12-09.
 */

public class KeepAliveService extends Service implements TCPClientListener {
    public String _ip = "";
    public int _port = 0;
    TCPClient _client = null;
    String _userName;
    boolean _isTeacher = false;
    String _uuid = null;

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
        _client = new TCPClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("서비스호출", "onStartCommand()실행됨");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void connectionSuccess() {
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
    public void myServiceFunc(String ip, int port, String userName, String uuid, boolean isTeacher){
        //서비스에서 처리할 내용
        _ip = ip;
        _port = port;
        _userName = userName;
        _uuid = uuid;
        _isTeacher = isTeacher;
        _client.Connect(_ip, port, this);
    }

    public void requestApi(JSONObject obj){
        _client.WriteCommand(obj.toString());
    }
}
