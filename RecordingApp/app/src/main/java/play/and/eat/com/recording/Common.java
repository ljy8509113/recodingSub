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

    public static String getRootPath(){
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        String folder = "";
        if(root == null || root.equals("")){
            folder = "/recodeFiles/";
        }else{
            folder = root+"/recodeFiles/";
        }

        File f = new File(folder);
        f.mkdir();

        return folder;
    }
}
