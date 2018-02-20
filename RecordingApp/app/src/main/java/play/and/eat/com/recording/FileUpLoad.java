package play.and.eat.com.recording;

import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import play.and.eat.com.recording.play.and.eat.com.recording.listener.FileDownloadListener;

/**
 * Created by ljy on 2017-12-10.
 */

public class FileUpLoad implements CopyStreamListener{

    /**
     * FTP 관련 작업을 처리할 수 있는 클래스<br/>
     * FTPConnector() 생성자를 통하여 객체를 생성할 수 있다.
     *
     * @author redPig(redPig761@gmail.com) *
     */
    private String SERVER = ""; // FTP 호스트 주소
    private int port = 21; //포트번호
    private String ID = "root"; // 유저 아이디
    private String PASS = "dkssud"; // 유저 패스워드
    private String mEncodingSet = ""; // 케릭터 셋
    private String mDefaultWorkDirectory = "/"; // 기본 작업 디렉토리

    private FTPClient ftp = null;
    private FileInputStream fis = null;
    long fileLength = 0;
    FileDownloadListener _downListener;

    String _fileName = "";

    /**
     * FTPConnector의 객체를 생성합니다.<br/>
     *
     * @param server       서버 호스트 주소입니다.
     * @param id           로그인 아이디입니다.
     * @param pass         로그인 패스워드입니다.
     * @param encodingSet  인코딩 종류를 지정합니다. UTF-8 or EUC-KR
     * @param workDirctory 작업을 진행할 FTP 서버의 디렉터리를 지정합니다.<br/>
     *                     예)/www/test
     */
    public FileUpLoad(String server, int port, String id, String pass,
                      String encodingSet, String workDirctory, FileDownloadListener listener) {
        // TODO Auto-generated constructor stub
        this.SERVER = server;
        this.port = port;
        this.ID = id;
        this.PASS = pass;
        this.mEncodingSet = encodingSet;
        this.mDefaultWorkDirectory = workDirctory;
        _downListener = listener;

        ftp = new FTPClient(); // 객체 생성
    }

    /**
     * FTP서버로 접속을 시도합니다.<br/>
     *
     * @return 서버 접속 성공 여부를 리턴합니다.
     */
    public boolean login() {
        boolean loginResult = false;
        try {
            ftp.setControlEncoding(mEncodingSet);
            ftp.connect(SERVER, port);
            loginResult = ftp.login(ID, PASS);
            ftp.enterLocalPassiveMode(); // PassiveMode 접속
            ftp.makeDirectory(mDefaultWorkDirectory);
            ftp.changeWorkingDirectory(mDefaultWorkDirectory);
        } catch (IOException e) {
            Log.e("FTP_LOGIN_ERR", e.toString());
        }

        if (!loginResult) {
            Log.e("FTP_LOGIN_ERR", "로그인 실패");
            return false;
        } else {
            Log.i("FPT_LOGIN_OK", "로그인 성공");
            return true;
        }
    }

    /**
     * FTP서버로 파일을 전송합니<br/>
     *
     * @param file 전송할 파일의 객체를 필요로 합니다.
     * @return 파일전송 성공 실패 여부를 리턴합니다.
     */
    public boolean uploadFile(File file) {
        if (!ftp.isConnected()) {
            Log.e("UPLOAD_ERR", "현재 FTP 서버에 접속되어 잇지 않습니다.");
            return false;
        }

        boolean uploadResult = false;
        try {
            ftp.setFileType(FTP.BINARY_FILE_TYPE); // 스트림으로 보낼 파일의 유형
            fileLength = file.length();
            _fileName = file.getName();
            fis = new FileInputStream(file);
            ftp.setCopyStreamListener(this);
            uploadResult = ftp.storeFile(file.getName(), fis);
            if (!uploadResult) {
                Log.e("FTP_SEND_ERR", "파일 전송을 실패하였습니다.");
                uploadResult = false;
            }
        } catch (Exception e) {
            Log.e("FTP_SEND_ERR", "파일전송에 문제가 생겼습니다. " + e.toString());
            uploadResult = false;
        } finally {
            try {
                fis.close();
                if(!uploadResult)
                    _downListener.downLoadFail(" 동영상 전송에 실패하였습니다.("+_fileName+")");
                else
                _downListener.downLoadComplate(_fileName);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.i("FTP", "ftp 접속 스트림 닫고 로그아웃중 오류 발생");
                e.printStackTrace();
                uploadResult = false;
            }
        }
        return uploadResult;
    }

    @Override
    public void bytesTransferred(CopyStreamEvent copyStreamEvent) {

    }

    @Override
    public void bytesTransferred(long l, int i, long l1) {
        int percent = (int)( ((double)l/(double)fileLength)  * 100);
//        Log.d("lee", "progress : "+ percent);
//        if(percent == 100){
//            _downListener.downLoadComplate(_fileName);
//        }else{
            Log.d("progress : ", percent + "");
            _downListener.progress(_fileName, percent);
//        }

        //Log.d("lee - ", "l : " + l + " // i" + i + " // l1 " +l1 );
        //Log.d("lee - ", "progress : " + percent + " // l : "+ l +" // total : " + fileLength);
    }

    public void close(){
        if (fis != null) {
            try {
                fis.close();
                ftp.logout();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.i("FTP", "ftp 접속 스트림 닫고 로그아웃중 오류 발생");
                e.printStackTrace();
            }
        }

        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.i("FTP", "ftp 접속 종료중 문제 발생");
            }
        }
    }
}

