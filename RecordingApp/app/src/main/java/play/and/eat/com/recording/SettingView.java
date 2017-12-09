package play.and.eat.com.recording;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.regex.Pattern;

import play.and.eat.com.recording.play.and.eat.com.recording.listener.SettingListener;

/**
 * Created by jeounglee on 2017. 11. 22..
 */

public class SettingView extends FrameLayout implements View.OnClickListener {

    Button _buttonBack;
    EditText _editIp;
    EditText _editName;
    EditText _editPort;
    RadioGroup _radioGroup;
    Button _buttonSave;

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
        _buttonSave = (Button)v.findViewById(R.id.button_save);

        _buttonBack.setOnClickListener(this);
        _buttonSave.setOnClickListener(this);

        addView(v);

    }

    public void setData(SharedPreferences pref){
        _pref = pref;
//        _pref = getContext().getApplicationContext().getSharedPreferences(Common.SHARE_DATA_KEY, Context.MODE_PRIVATE);

        String ip = _pref.getString(Common.IP_KEY, "");
        boolean isTeacher = _pref.getBoolean(Common.IS_TEACHER_KEY, false);
        String name = _pref.getString(Common.NAME_KEY, "");
        int port = _pref.getInt(Common.PORT_KEY,0);

        if(ip.equals("") == false){
            _editIp.setText(ip);
            _editName.setText(name);
            _editPort.setText(port + "");
            if(isTeacher){
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

//                if(selectedId == R.id.radio_0){
//                    _pref.edit().putBoolean(Common.IS_TEACHER_KEY, true);
//                }else{
//                    _pref.edit().putBoolean(Common.IS_TEACHER_KEY, false);
//                }

                String validIp = "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$";
                String ip = _editIp.getText().toString();
                int port = Integer.parseInt(_editPort.getText().toString());

                if (!Pattern.matches(validIp, ip)) {
                    Toast.makeText(getContext(), "IP 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                    return;
                }else {
                    if (_editName.getText().toString().equals("")) {
                        Toast.makeText(getContext(), "카메라 이름 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (port < 1) {
                        Toast.makeText(getContext(), "포트 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    _listener.onSaved(ip, port, _editName.getText().toString(), selectedId == R.id.radio_0);
                }

                break;
        }
    }
}
