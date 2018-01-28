package play.and.eat.com.recording;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.regex.Pattern;

import play.and.eat.com.recording.data.SettingData;
import play.and.eat.com.recording.play.and.eat.com.recording.listener.SettingListener;

/**
 * Created by jeounglee on 2017. 11. 22..
 */

public class SettingView extends FrameLayout implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    Button _buttonBack;
    EditText _editIp;
    EditText _editName;
    EditText _editPort;
    RadioGroup _radioGroup;
    Button _buttonSave;
    EditText _editFtpPort;
    EditText _editFTPId;
    EditText _editFTPPw;

    EditText [] arrayEditText = {_editIp,_editName,_editPort,_editFTPId,_editFtpPort,_editFTPPw};

    SharedPreferences _pref;
    public SettingListener _listener;

    public SettingView(Context context) {
        super(context);
        init();
    }

    public SettingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init(){
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
        View v = li.inflate(R.layout.setting, this, false);

        _buttonBack = (Button)v.findViewById(R.id.button_back);
        _editIp = (EditText)v.findViewById(R.id.edit_ip);
        _editName = (EditText) v.findViewById(R.id.edit_name);
        _editPort = (EditText) v.findViewById(R.id.edit_port);
        _radioGroup = (RadioGroup)v.findViewById(R.id.radioGroup);
        _radioGroup.setOnCheckedChangeListener(this);

        _buttonSave = (Button)v.findViewById(R.id.button_save);
        _editFtpPort = (EditText)v.findViewById(R.id.edit_ftp_port);

        _editFTPId = (EditText)v.findViewById(R.id.edit_ftp_id);
        _editFTPPw = (EditText)v.findViewById(R.id.edit_ftp_password);

        _buttonBack.setOnClickListener(this);
        _buttonSave.setOnClickListener(this);

        addView(v);

    }

    public void setData(SharedPreferences pref){
        _pref = pref;
//        _pref = getContext().getApplicationContext().getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);
//
//        String ip = _pref.getString(Common.IP_KEY, "");
//        boolean isTeacher = _pref.getBoolean(Common.IS_TEACHER_KEY, false);
//        String name = _pref.getString(Common.NAME_KEY, "");
//        int port = _pref.getInt(Common.PORT_KEY,0);
//        int ftpPort = _pref.getInt(Common.FTP_PORT, 21);
//        String ftpId = _pref.getString(Common.FTP_ID,"");
//        String ftpPw = _pref.getString(Common.FTP_PW,"");

//        if(ip.equals("") == false){
//            _editIp.setText(ip);
//            _editName.setText(name);
//            _editPort.setText(port + "");
//            _editFtpPort.setText(ftpPort + "");
//            _editFTPId.setText(ftpId);
//            _editFTPPw.setText(ftpPw);
//
//            if(isTeacher){
//                _radioGroup.check(R.id.radio_0);
//            }else{
//                _radioGroup.check(R.id.radio_1);
//            }

        if(!SettingData.Instance().ip.equals("")){
            _editIp.setText(SettingData.Instance().ip);
            _editName.setText(SettingData.Instance().name);
            _editPort.setText(SettingData.Instance().port + "");
            _editFtpPort.setText(SettingData.Instance().ftpPort + "");
            _editFTPId.setText(SettingData.Instance().ftpId);
            _editFTPPw.setText(SettingData.Instance().ftpPw);

            if(SettingData.Instance().isTeacher){
                _radioGroup.check(R.id.radio_0);
            }else{
                _radioGroup.check(R.id.radio_1);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_back:
                this.setVisibility(GONE);
                break;
            case R.id.button_save :
                int selectedId = _radioGroup.getCheckedRadioButtonId();

                String validIp = "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$";
                String ip = _editIp.getText().toString();
//                int port = Integer.parseInt(_editPort.getText().toString());

                if (!Pattern.matches(validIp, ip)) {
                    Toast.makeText(getContext(), "IP 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                    return;
                }else {

                    if (_editPort.getText().toString().equals("")){
                        Toast.makeText(getContext(), "포트를 입력해주세요.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    int port = 0;
                    try {
                        port = Integer.parseInt(_editPort.getText().toString());
                    }catch (Exception e){
                        Toast.makeText(getContext(), "포트는 숫자만 입력 가능합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(R.id.radio_1 == selectedId){
                        if(_editName.getText().toString().equals("")){
                            Toast.makeText(getContext(), "카메라 이름 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                            return;
                        }else if(_editName.getText().toString().toLowerCase().equals(Common.NICK_TEACHER)){
                            Toast.makeText(getContext(), "학생용 카메라 이름에 "+Common.NICK_TEACHER +"를 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if (port < 1) {
                        Toast.makeText(getContext(), "포트 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(_editFTPId.getText().toString().equals("")){
                        Toast.makeText(getContext(), "FTP ID 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(_editFTPPw.getText().toString().equals("")){
                        Toast.makeText(getContext(), "FTP 패스워드 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

//                    SharedPreferences.Editor edit = _pref.edit();
//                    edit.putString(Common.FTP_ID, _editFTPId.getText().toString());
//                    edit.putString(Common.FTP_PW, _editFTPPw.getText().toString());
//                    edit.commit();
//
//                    int ftpPort = Integer.parseInt(_editFtpPort.getText().toString());

//                    String ip, int port, boolean isTeacher, String name, int ftpPort, String ftpId, String ftpPw

                    SettingData.Instance().saveData(
                            _editIp.getText().toString(),
                            Integer.parseInt(_editPort.getText().toString()),
                            selectedId == R.id.radio_0 ? true:false,
                            _editName.getText().toString(),
                            Integer.parseInt(_editFtpPort.getText().toString()),
                            _editFTPId.getText().toString(),
                            _editFTPPw.getText().toString()
                            );

                    _listener.onSaved();
                    this.setVisibility(View.GONE);

                }

                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if(i == R.id.radio_0){
            _editName.setEnabled(false);
        }else{
            _editName.setEnabled(true);
        }
    }
}
