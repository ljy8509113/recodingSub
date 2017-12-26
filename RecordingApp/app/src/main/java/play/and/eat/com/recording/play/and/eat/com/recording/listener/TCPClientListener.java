package play.and.eat.com.recording.play.and.eat.com.recording.listener;

import org.json.JSONObject;

/**
 * Created by ljy on 2017-12-08.
 */

public interface TCPClientListener {
    void connectionSuccess();
    void onReceiver(String result);
    void connectionSnap();
    void error(String msg);
}
