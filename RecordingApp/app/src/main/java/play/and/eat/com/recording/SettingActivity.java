package play.and.eat.com.recording;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.regex.Pattern;

public class SettingActivity extends Activity implements View.OnClickListener {

    Button _buttonBack;
    EditText _editIp;
    RadioGroup _radioGroup;
    Button _buttonSave;

    final String DATA_KEY = "save_data";
    final String IS_TEACHER_KEY = "isTeacher";
    final String IP_KEY = "ip";
    SharedPreferences _pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        _buttonBack = (Button)findViewById(R.id.button_back);
        _editIp = (EditText)findViewById(R.id.edit_ip);
        _radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        _buttonSave = (Button)findViewById(R.id.button_save);

        _buttonBack.setOnClickListener(this);
        _buttonSave.setOnClickListener(this);

        _pref = getSharedPreferences(DATA_KEY, MODE_PRIVATE);

        String ip = _pref.getString(IP_KEY, "");
        boolean isTeacher = _pref.getBoolean(IS_TEACHER_KEY, false);

        if(ip.equals("") == false){
            _editIp.setText(ip);

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
                onBackPressed();
                break;
            case R.id.button_save :
                 int selectedId = _radioGroup.getCheckedRadioButtonId();

                 if(selectedId == R.id.radio_0){
                     _pref.edit().putBoolean(IS_TEACHER_KEY, true);
                 }else{
                    _pref.edit().putBoolean(IS_TEACHER_KEY, false);
                 }

                String validIp = "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$";
                String ip = _editIp.getText().toString();

                if (!Pattern.matches(validIp, ip)) {
                    Toast.makeText(this, "IP 확인이 필요합니다.", Toast.LENGTH_LONG).show();
                    return;
                }else{
                    _pref.edit().putString(IP_KEY, ip);
                    _pref.edit().commit();

                    onBackPressed();
                }

                break;
        }
    }
}
