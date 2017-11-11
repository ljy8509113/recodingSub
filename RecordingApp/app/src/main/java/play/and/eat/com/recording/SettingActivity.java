package play.and.eat.com.recording;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingActivity extends Activity implements View.OnClickListener {

    Button _buttonBack;
    EditText _editIp;
    RadioButton _radioTeacher;
    RadioButton _radioStudent;
    RadioGroup _radioGroup;
    Button _buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        _buttonBack = (Button)findViewById(R.id.button_back);
        _editIp = (EditText)findViewById(R.id.edit_ip);
        _radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        _buttonSave = (Button)findViewById(R.id.button_save);
        _radioTeacher = (RadioButton)findViewById(R.id.radio_0);
        _radioStudent = (RadioButton)findViewById(R.id.radio_1);

        _buttonBack.setOnClickListener(this);
        _buttonSave.setOnClickListener(this);

//        _radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                // TODO Auto-generated method stub
//                switch (checkedId) {
//                    case R.id.radio_0:
//                        //선생
//                        break;
//                    case R.id.radio_1:
//                        //학생
//                        break;
//                }
//            }
//        });



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_back:

                break;
            case R.id.button_save :

                break;
        }
    }
}
