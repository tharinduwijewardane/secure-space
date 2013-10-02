package com.tharindu.securespace;

import com.paranoiaworks.unicus.android.sse.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity{
	
	TextView loginFeedBack;
	EditText loginPass;
	Button login;
	private PreferenceHelp prefHelp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.th_la_login);
		
		if(prefHelp == null){
			prefHelp = new PreferenceHelp(getApplicationContext());
		}
		
		loginFeedBack = (TextView) findViewById(R.id.tvLoginFeedback);
		loginPass = (EditText) findViewById(R.id.etEnterLoginPass_th);
		login = (Button) findViewById(R.id.bLogin_th);
		login.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				String enteredPW = loginPass.getText().toString();
				String loginPW = prefHelp.getPrefString(ConstVals.PREF_KEY_LOGIN_PASSWORD);
				if(enteredPW.equals(loginPW)){
					Intent intent = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intent);	
				}else{
					loginFeedBack.setText("Wrong Password. Try again.\nDefault password: 0");
					loginPass.setText("");
				}		
							
			}
		});
		
	}

	
	
}
