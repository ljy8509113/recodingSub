package play.and.eat.com.recording.play.and.eat.com.recording.listener;

/**
 * Created by KOITT on 2017-12-18.
 */

public interface FileDownloadListener {
    void progress(String fileName, int persent);
    void downLoadComplate(String fileName);
}
