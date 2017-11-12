package play.and.eat.com.recording;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback{

    Button _buttonSetting;
    Button _buttonRecoding;
    SurfaceView _sView;
    SurfaceHolder _holder;
    MediaRecorder _recorder;
    String _filename;
//    String EXTERNAL_STORAGE_PATH;
    int _fileIndex = 0;
    String RECORDED_FILE = "";

    final int MY_PERMISSIONS_RECODE_AUDIO = 0;
    final int MY_PERMISSIONS_CAMERA = 1;
    final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 2;
    final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 3;

    SharedPreferences _pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _buttonRecoding = (Button) findViewById(R.id.button_recoding);
        _buttonSetting = (Button) findViewById(R.id.button_setting);
        _sView = (SurfaceView) findViewById(R.id.surface_view);

        _holder = _sView.getHolder();
        _holder.addCallback(this);
        
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            _holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        _pref = getSharedPreferences(Common.SHARE_DATA_KEY, MODE_PRIVATE);
//        String ip = _pref.getString(Common.IP_KEY, "");
//        boolean isTeacher = _pref.getBoolean(Common.IS_TEACHER_KEY, false);
        RECORDED_FILE = _pref.getString(Common.NAME_KEY, "Default");

        checkPermission(this, Manifest.permission.CAMERA, MY_PERMISSIONS_CAMERA);
        checkPermission(this, Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_RECODE_AUDIO);
        checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

        // 외장메모리가 있는지 확인한다.
        // Environment.getExternalStorageState() 를 통해서 현재 외장메모리를 상태를 알수있다.
        String state = Environment.getExternalStorageState();
        // Environment.MEDIA_MOUNTED 외장메모리가 마운트 flog
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getApplicationContext(), "외장 메모리가 마운트 되지않았습니다.", Toast.LENGTH_LONG).show();
        }

        _recorder = new MediaRecorder();

        // 오디오와영상 입력 형식 설정
        _recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        _recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        _recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // 오디오와영상 인코더 설정
        _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        _recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

        // 저장될 파일 지정
        _filename = getFilename();
        _recorder.setOutputFile(_filename);

        // 녹화도중에 녹화화면을 뷰에다가 출력하게 해주는 설정
        _recorder.setPreviewDisplay(_holder.getSurface());
//        _recorder.setOrientationHint(15);

        // 녹화 시작 버튼
        _buttonRecoding.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(_recorder != null){
                    stopRecoding();
                }else{
                    startRecoding();
                }
            }
        });
    }

    void startRecoding(){
        try {
            // 녹화 준비,시작
            _recorder.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            _recorder.release();
            _recorder = null;
        }
    }

    void stopRecoding(){
        if (_recorder == null)
            return;
        // 녹화 중지
        _recorder.stop();

        // 영상 재생에 필요한 메모리를 해제한다.
        _recorder.release();
        _recorder = null;

        ContentValues values = new ContentValues(10);

        values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
        values.put(MediaStore.Audio.Media.ALBUM, "Video Album");
        values.put(MediaStore.Audio.Media.ARTIST, "Mike");
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Recorded Video");
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Audio.Media.DATA, _filename);

        Uri videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (videoUri == null) {
            Log.d("SampleVideoRecorder", "Video insert failed.");
            return;
        }

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));

    }

    private String getFilename() {
        _fileIndex++;
        String newFilename = Common.getRootPath() + RECORDED_FILE +"_"+ _fileIndex + ".mp4";
        return newFilename;
    }

    public void checkPermission(Context context, String permission, int permissionCode){
        // Activity에서 실행하는경우
        if (ContextCompat.checkSelfPermission(context, permission)!= PackageManager.PERMISSION_GRANTED) {
            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)context, permission)) {
                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
                ActivityCompat.requestPermissions((Activity)context, new String[]{permission}, permissionCode);

            } else {
                ActivityCompat.requestPermissions((Activity)context, new String[]{permission}, permissionCode);
                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 권한 허가
            // 해당 권한을 사용해서 작업을 진행할 수 있습니다
        } else {
            // 권한 거부
            // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        _recorder.setPreviewDisplay(_holder.getSurface());
        try{
            _recorder.prepare();
        }catch (IOException e){

        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
