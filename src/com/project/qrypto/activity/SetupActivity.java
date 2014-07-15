
package com.project.qrypto.activity;

import java.security.MessageDigest;

import com.project.qrypto.R;
import com.project.qrypto.keymanagement.KeyManager;
import com.project.qrypto.util.Dialogs;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class SetupActivity extends Activity {
	
	@Override
	public void onBackPressed() {
		//Disable Back Button
	}
	
	private void handleNewSetup() {
		setContentView(R.layout.setup_passphrase);
		
		final EditText password = (EditText) findViewById(R.id.passwordBox);
		final EditText repeat = (EditText) findViewById(R.id.repeatPassword);
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
			if(repeat.getText().toString().equals(password.getText().toString()) ) {
				try {
					String text = password.getText().toString(); 
					
					if(!text.equals("")) {
						MessageDigest md;
						md = MessageDigest.getInstance("SHA-256");
						md.update(text.getBytes("UTF-8"));
						KeyManager.getInstance().setKeyStoreKey(md.digest());
						KeyManager.getInstance().setPasswordProtected(true);
					} 
					
					finish();
				
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(SetupActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
				} 
				}else {
					Toast.makeText(SetupActivity.this, R.string.not_match, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	private void handleLogin() {
		setContentView(R.layout.login);
		
		final EditText loginText = (EditText) findViewById(R.id.loginText);
		
		Button login = (Button) findViewById(R.id.login);
		login.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				try {
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					md.update(loginText.getText().toString().getBytes("UTF-8")); // Change this to "UTF-16" if needed
					KeyManager.getInstance().setKeyStoreKey(md.digest());
					KeyManager.getInstance().load(SetupActivity.this);
					
					finish();
					
				} catch(Exception e) {
					Toast.makeText(SetupActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		Button forgot = (Button) findViewById(R.id.forgot);
		forgot.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Dialogs.showConfirmation(SetupActivity.this, R.string.forgot_msg, new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						KeyManager.getInstance().delete(SetupActivity.this);
						handleNewSetup();
					}
				});
			}
		});
	}
	
	@Override
	public void onCreate(Bundle instanceState) {
		super.onCreate(instanceState);
		if(!KeyManager.getInstance().isSetupComplete()) {
			handleNewSetup();
        } else if(KeyManager.getInstance().isPasswordProtected()) {
        	handleLogin();
        } else {
        	finish();
        }
	}
	
	@SuppressLint("ShowToast")
	public void onPause() {
		super.onPause();
		try {
    		//Commit it to the db
    		KeyManager.getInstance().commit(this);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "An error occured while saving the keystore.", Toast.LENGTH_SHORT);
		}
	}
	
}
