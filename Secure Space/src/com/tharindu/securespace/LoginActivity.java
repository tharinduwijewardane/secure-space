package com.tharindu.securespace;

import com.paranoiaworks.unicus.android.sse.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity{
	
	EditText loginPass;
	Button login;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.th_la_login);
		
		loginPass = (EditText) findViewById(R.id.etEnterLoginPass_th);
		login = (Button) findViewById(R.id.bLogin_th);
		login.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);				
			}
		});
		
	}

	
	
}
