package com.project.qrypto.activity;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;

import org.spongycastle.util.encoders.Base64;


import com.project.qrypto.R;
import com.project.qrypto.keymanagement.Contact;
import com.project.qrypto.keymanagement.Key;
import com.project.qrypto.keymanagement.KeyManager;
import com.project.qrypto.util.AES;
import com.project.qrypto.util.Decoder;
import com.project.qrypto.util.FileHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class DecryptActivity extends Activity {
	
	public static final String EXTRA_TEXT = "text";
	
	TextView chiperText;
	Button decrypt;
	String message;
	
	String displayName;
	String currentAddress;
	private ContactSpinnerAdapter contactAdapter;
	
	
    
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			KeyManager.getInstance().init(this);
		} catch (Exception e) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
		}
		
		setContentView(R.layout.decrypt_main);
		final Button decrypt = (Button) findViewById(R.id.decrypt);
		
		handleActions(getIntent());
		
		
		
		//Setup the person spinner
		Spinner personSpinner = (Spinner) findViewById(R.id.personSpinner);
		this.contactAdapter = new ContactSpinnerAdapter(this);
		personSpinner.setAdapter(contactAdapter);
		personSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				TextView displayName = (TextView) view.findViewById(R.id.display_name);
				TextView address = (TextView) view.findViewById(R.id.address);
				
				Spinner personSpinner = (Spinner) findViewById(R.id.personSpinner);
				getPreferences(MODE_PRIVATE).edit().putString("currentSelectedAddress", 
						((Contact) contactAdapter.getItem(personSpinner.getSelectedItemPosition())).address).commit();
				
				
				DecryptActivity.this.displayName = displayName.getText().toString();
				DecryptActivity.this.currentAddress = address.getText().toString();
				
				decrypt.setEnabled(true);
				
			}

			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		
		decrypt.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				chiperText = (TextView) findViewById(R.id.chiperText);
				message = chiperText.getText().toString();
				
				//check if textview in empty
				if (message.matches("")) {
					Toast.makeText(DecryptActivity.this, R.string.error_decrypt,
							Toast.LENGTH_SHORT).show();
				    return;
				}
				
					// Can be decrypt now
					Key key = KeyManager.getInstance().getLookup()
							.get(currentAddress);
					
						try {
							String pruned = message;
							byte[] clearText = AES.handle(false,
									Base64.decode(pruned), key.key);
							message = new String(clearText);
							Log.i("success", message);
							
							handlePlaintext();
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(DecryptActivity.this, R.string.error_decrypt,
							Toast.LENGTH_SHORT).show();
						}

			}
		});
		
		contactAdapter.populate();
		int savedPosition = contactAdapter.getPosition(getPreferences(MODE_PRIVATE).getString("currentSelectedAddress", ""));
		if(savedPosition != -1) {
			personSpinner.setSelection(savedPosition);
		}

	}
	
	private void handlePlaintext() {
		setContentView(R.layout.plaintext_display);
		
		TextView name = (TextView) findViewById(R.id.name);
		name.setText(Html.fromHtml(displayName));
		
		TextView plainText = (TextView) findViewById(R.id.plaintext);
		Log.i("plain text", message);
		plainText.setText(Html.fromHtml(message));
		
		Button goToEncrypt = (Button) findViewById(R.id.goto_encrypt);
		goToEncrypt.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				DecryptActivity.this.startActivity(new Intent(DecryptActivity.this, EncryptActivity.class));
				finish();
			}
		});
		
	}
	
	private void handleActions(Intent intent) {
			
	        String action = intent.getAction();
	        Bundle extras = intent.getExtras();
	        String type = intent.getType();
	        Uri uri = intent.getData();
	
	        if (extras == null) {
	            extras = new Bundle();
	        }
	        
	        /*
	         * Device's Action
	         */
	        if (Intent.ACTION_SEND.equals(action) && type != null) {
	        	
	            	uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	            	
	            	/*
	            	 *  decode file qr code to text
	            	 */	
	            	try {
	            		String path = FileHelper.getPath(this, uri);
	                    String text = Decoder.getQrCodeText(path);
	                    
	                    Log.i("decode result", text);
	                    extras.putString(EXTRA_TEXT, text);
	                    
	                    chiperText = (TextView) findViewById(R.id.chiperText);
	            		chiperText.setText(Html.fromHtml(text));
	            		
	            		
					} catch (Exception e) {
						e.printStackTrace();
					} 
	     
	        } 
	       
	}
		
	
	private class ContactSpinnerAdapter extends BaseAdapter {

		ArrayList<Contact> contacts = new ArrayList<Contact>();
		Activity context;
		
		public ContactSpinnerAdapter(Activity context) {
			this.context = context;
		}
		
		public void populate() {
			contacts.clear();
			
			for(Entry<String, Key> entry : KeyManager.getInstance().getLookup().entrySet()) {
				Contact c = new Contact();
				Key value = entry.getValue();
				
				if(value != null && value.key != null) {
					c.address = entry.getKey();
					c.name = value.displayName;
					
					contacts.add(c);
				}
			}
			
			java.util.Collections.sort(contacts, new Comparator<Contact>() {

				public int compare(Contact contact1, Contact contact2) {
					return contact1.name.compareTo(contact2.name);
				}
				
			});
			
			this.notifyDataSetChanged();
		}
		
		public int getCount() {
			return contacts.size();
		}

		public Object getItem(int position) {
			return contacts.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getPosition(String address) {
			for(int i=0; i < contacts.size(); ++i) {
				if(contacts.get(i).address.equals(address)) {
					return i;
				}
			}
			return -1;
		}
		
		public View getView(int position, View view, ViewGroup parent) {
			if(view == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				view = inflater.inflate(R.layout.contacts_spinner_row, null);
			}
			
			TextView displayName = (TextView) view.findViewById(R.id.display_name);
			TextView address = (TextView) view.findViewById(R.id.address);
			
			Contact contact = contacts.get(position);
			displayName.setText(contact.name);
			address.setText(contact.address);
			
			return view;
		}
		
	}
	
}
