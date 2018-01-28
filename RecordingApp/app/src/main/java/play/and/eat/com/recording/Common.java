package play.and.eat.com.recording;

import android.os.Environment;

import java.io.File;

/**
 * Created by jeounglee on 2017. 11. 12..
 */

public class Common {
    public final static String SHARE_DATA_KEY = "save_data";
    public final static String IS_TEACHER_KEY = "isTeacher";
    public final static String IP_KEY = "ip";
    public final static String NAME_KEY = "name";
    public final static String PORT_KEY = "port";
    public final static String FTP_PORT = "ftpPort";
    public final static String FTP_ID = "ftpId";
    public final static String FTP_PW = "ftpPass";
    public final static String NICK_TEACHER = "teacher";

    public static String getMoviePath(){
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        String folder = "";
        if(root == null || root.equals("")){
            folder = "/recodeFiles";
        }else{
            folder = root+"/recodeFiles";
        }

        File f = new File(folder);
        if(f.isDirectory() == false)
            f.mkdir();

        return folder;
    }
}
