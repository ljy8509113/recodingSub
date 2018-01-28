package play.and.eat.com.recording.data;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import java.util.UUID;

import play.and.eat.com.recording.Common;

/**
 * Created by ljy on 2018-01-28.
 */

public class SettingData {
    private static SettingData instance = null;
    public String ip = null;
    public boolean isTeacher;
    public String name = null;
    public int port;
    public int ftpPort = 21;
    public String ftpId = null;
    public String ftpPw = null;
    public String uuid = null;

    SharedPreferences pref;

    public static SettingData Instance(){
        if(instance == null)
            instance = new SettingData();

        return instance;
    }

    public void loadData(SharedPreferences pref, Context context){
        this.pref = pref;
        ip = pref.getString(Common.IP_KEY, "");
        isTeacher = pref.getBoolean(Common.IS_TEACHER_KEY, false);
        name = pref.getString(Common.NAME_KEY, "NO_NAME");
        port = pref.getInt(Common.PORT_KEY,0);
        ftpPort = pref.getInt(Common.FTP_PORT, 21);
        ftpId = pref.getString(Common.FTP_ID,"");
        ftpPw = pref.getString(Common.FTP_PW,"");
        uuid = getUUID(context);
    }

    public void saveData(String ip, int port, boolean isTeacher, String name, int ftpPort, String ftpId, String ftpPw){
        this.ip = ip;
        this.port = port;
        this.isTeacher = isTeacher;
        if(isTeacher)
            this.name = Common.NICK_TEACHER;
        else
            this.name = name;
        this.ftpPort = ftpPort;
        this.ftpId = ftpId;
        this.ftpPw = ftpPw;

        saveData();
    }

    public void saveData(){
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(Common.IP_KEY, ip);
        edit.putInt(Common.PORT_KEY, port);
        edit.putBoolean(Common.IS_TEACHER_KEY, isTeacher);
        edit.putString(Common.NAME_KEY, name);
        edit.putInt(Common.FTP_PORT, ftpPort);
        edit.putString(Common.FTP_ID, ftpId);
        edit.putString(Common.FTP_PW, ftpPw);
        edit.commit();
    }

    public String getUUID(Context mContext){
        // Activity에서 실행하는경우
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            final String tmDevice, tmSerial, androidId;

            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();

            androidId = "" + android.provider.Settings.Secure.getString(mContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
            String deviceId = deviceUuid.toString();
            return deviceId;
        }else{
            return null;
        }
    }

}
