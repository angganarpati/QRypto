package com.project.qrypto.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Base64;

import com.google.zxing.BarcodeFormat;
import com.project.qrypto.R;
import com.project.qrypto.keymanagement.Contact;
import com.project.qrypto.keymanagement.Key;
import com.project.qrypto.keymanagement.KeyManager;
import com.project.qrypto.util.AES;
import com.project.qrypto.util.Content;
import com.project.qrypto.util.Encoder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("SdCardPath") 
public class EncryptActivity extends ListActivity {

	private EditText text;
	@SuppressWarnings("unused")
	private String displayName;
	private String currentAddress;
	private ContactSpinnerAdapter contactAdapter;

	@SuppressWarnings("resource")
	private void sendQR(String encryptText) {
			Encoder qrCodeEncoder = new Encoder(encryptText, 
		             null, 
		             Content.Type.TEXT,  
		             BarcodeFormat.QR_CODE.toString(), 500);
			
			try {
				Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
				
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				
				// for compress image file
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
				File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.png");
				
				
				f.createNewFile();
				FileOutputStream fo = new FileOutputStream(f);
				fo.write(bytes.toByteArray());

				} catch (Exception e) {
					e.printStackTrace();
				}
			
			// Action to send QR code via share option
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("image/png");
			sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.png"));
			startActivity(Intent.createChooser(sendIntent, getString(R.string.title_send_email)));
			finish();
		}
	



	@SuppressLint("ShowToast")
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// get key manager method (static factor method)
		try {
			KeyManager.getInstance().init(this);
		} catch (Exception e) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG);
		}
		
		setContentView(R.layout.encrypt_main);
		
		//Setup the List View 
		ListView view = getListView();
		registerForContextMenu(view);
		
		final Button send = (Button) findViewById(R.id.send);
		
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
				
				getListView().setSelection(getListView().getCount() - 1);
				
				EncryptActivity.this.displayName = displayName.getText().toString();
				EncryptActivity.this.currentAddress = address.getText().toString();
				
				// enable this if spinner not null value
				text.setEnabled(true);
				send.setEnabled(true);
			}
			// default choose earliest data
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// input some text here
		text = (EditText) findViewById(R.id.textBox);
		
		
		if(savedInstanceState != null) {
			text.setText(savedInstanceState.getString("textField"));
		}
		
		// send button to encrypt & generate QRcode then share it
		send.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				String message = text.getText().toString();
				if(message.length() > 0) {
					text.setText("");
					
					//Get the proper key
					Key key = KeyManager.getInstance().getLookup().get(currentAddress);
					
					try {
						
						byte[] clearText = message.getBytes();
						
						// call class aes to encrypt message then run sendQR function to generate QRCode
						sendQR( new String(Base64.encode(AES.handle(true, clearText, key.key))));
						
						
						
					} catch (InvalidCipherTextException e) {
						e.printStackTrace();
						Toast.makeText(EncryptActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		// populate contact into spinner & set last choose as default
		contactAdapter.populate();
		int savedPosition = contactAdapter.getPosition(getPreferences(MODE_PRIVATE).getString("currentSelectedAddress", ""));
		if(savedPosition != -1) {
			personSpinner.setSelection(savedPosition);
		}
		
	}
	
	// show contact spinner    
	private class ContactSpinnerAdapter extends BaseAdapter {
        
		// get from Contact class
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		Activity context;
		
		public ContactSpinnerAdapter(Activity context) {
			this.context = context;
		}
		
		//populate array contact & show contact spinner 
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
		
		// display it 
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


		
		
	