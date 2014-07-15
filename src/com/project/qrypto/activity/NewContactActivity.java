/**	This file is part of Masq.

    Masq is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Masq is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Masq.  If not, see <http://www.gnu.org/licenses/>.
**/

package com.project.qrypto.activity;

import java.util.HashMap;

import org.spongycastle.util.encoders.Base64;

import com.project.qrypto.R;
import com.project.qrypto.util.AES;
import com.project.qrypto.util.Temp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Activity for getting a new contact's key
 * @author Richard Laughlin
 */
public class NewContactActivity extends Activity {

	private static final int GENERATE_KEY_RESULT = 1;
	
	Temp hashdata;
	
	private Button saveButton;
	
	private String contactAddress;
	private String contactName;
	
	private byte[] key;
	
	@Override
	public void onCreate(Bundle instance) {
		super.onCreate(instance);
		setContentView(R.layout.management_new_contact);
		
		hashdata = new Temp(getApplicationContext());
		
		Button generateButton = (Button) findViewById(R.id.generateButton);
		generateButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(NewContactActivity.this, GenerateActivity.class);
				intent.putExtra("count", 1);
				NewContactActivity.this.startActivityForResult(intent, GENERATE_KEY_RESULT);
			}
		});
		
		
		saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("address", contactAddress);
				resultIntent.putExtra("name", contactName);
				resultIntent.putExtra("key", key);
				setResult(Activity.RESULT_OK, resultIntent);
				hashdata.clearData();
				finish();
			}
		});
		
		//Setup contact
		contactAddress = getIntent().getStringExtra("address");
		contactName = getIntent().getStringExtra("name");
		
		if(contactAddress == null || contactName == null) {
			finish();
			Log.d("NewContactActivity", "Intent missing required values");
		}
		
		//Setup key -- used for edit
		key = getIntent().getByteArrayExtra("key");
		saveButton.setEnabled(key != null);
		getKey();
		
	}
	
	private void getKey(){

		HashMap<String, String> data = hashdata.getUserData();  
        // name
        String hash = data.get(Temp.KEY_NAME); 
        
        byte[] values = null;
		
		try {
			values = Base64.decode(hash);
		} catch(Exception e) {
			e.printStackTrace();
		}
		setKey(values);
	}
	
	private void setKey(byte[] newKey) {

		//Set the new key
		if(newKey != null && newKey.length == AES.AES_KEY_SIZE) {
			key = newKey;
		} else {
			Toast.makeText(this, R.string.invalid_key, Toast.LENGTH_SHORT).show();
		}
		
		//Set the save button to enabled/disabled
		saveButton.setEnabled(key != null);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch(requestCode) {
            case GENERATE_KEY_RESULT:
            	if(resultCode == Activity.RESULT_OK){
            		setKey(data.getByteArrayExtra("data"));
            	}
            	break;
            
            	}
            }
    }
	
}
