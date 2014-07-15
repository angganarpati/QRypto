

package com.project.qrypto.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;

import org.spongycastle.util.encoders.Base64;

import com.google.zxing.BarcodeFormat;
import com.project.qrypto.R;
import com.project.qrypto.keymanagement.Contact;
import com.project.qrypto.keymanagement.Key;
import com.project.qrypto.keymanagement.KeyManager;
import com.project.qrypto.util.Content;
import com.project.qrypto.util.Decoder;
import com.project.qrypto.util.Dialogs;
import com.project.qrypto.util.Encoder;
import com.project.qrypto.util.FileHelper;
import com.project.qrypto.util.Temp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Manages the Keystore and Contacts
 * @author Richard Laughlin
 */
@SuppressLint("ShowToast") 
public class KeyManagementActivity extends ExpandableListActivity {
	
	public static final String EXTRA_TEXT = "text";
	public static final int SELECT_CONTACT = 1;
	public static final int SETUP_KEY = 2;
	
	Temp hashdata;
	
	
	private KeyManagementAdapter adapter;
	private static FileOutputStream fo;

	
	// convert key to qr code then send with share option
	@SuppressLint("SdCardPath") 
	public static void sendKey(Context ctx, String key){
		
		Encoder qrCodeEncoder = new Encoder(key, 
	             null, 
	             Content.Type.TEXT,  
	             BarcodeFormat.QR_CODE.toString(), 500);
		
		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			
			// to compress file
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
			File f = new File(Environment.getExternalStorageDirectory() + File.separator + "key_file.png");
			
			
			f.createNewFile();
			fo = new FileOutputStream(f);
			fo.write(bytes.toByteArray());

			} catch (Exception e) {
				e.printStackTrace();
			}
		// Action to send QR code via share option
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("image/png");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/key_file.png"));
		ctx.startActivity(Intent.createChooser(sendIntent, ctx.getString(R.string.title_send_email)));
		((Activity) ctx).finish();
		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	try {
    		KeyManager.getInstance().init(this);
    	} catch (Exception e) {
    		Toast.makeText(this, R.string.error, Toast.LENGTH_LONG);
    	}
    	
    	hashdata = new Temp (KeyManagementActivity.this);

    	setContentView(R.layout.management_main);	
    	handleActions(getIntent());
    	
    	//Inflate the header so we can play with the buttons
    	LayoutInflater inf = (LayoutInflater) this.getLayoutInflater();
		View headerView = inf.inflate(R.layout.management_header, null);
		
   
		Button addContact = (Button) headerView.findViewById(R.id.addContact);
		// this opens the activity. note the  Intent.ACTION_GET_CONTENT
	    // and the intent.setType
	    addContact.setOnClickListener( new OnClickListener() {
	    	
	        public void onClick(View v) {
				Intent intent = new Intent(KeyManagementActivity.this, DisplayContactsActivity.class);
	            startActivityForResult(intent, SELECT_CONTACT);
	        }
	        
	    });
	    
	    //Add the new header to the list view
	    this.getExpandableListView().addHeaderView(headerView);
	    
	    //Setup the adapter 
	    adapter = new KeyManagementAdapter(this);
		this.setListAdapter(adapter);
		
		//Load the adapter
		adapter.populate();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch(requestCode) {
            case SELECT_CONTACT:
            	if(resultCode == Activity.RESULT_OK) {
            		
	            	//Get the information we need
	            	String name = data.getStringExtra("name");  
	            	String address = data.getStringExtra("address");
	            	
	            	//Clean the address of garbage
	            	address = address.replaceAll("[^0-9]", "");
	            	
	            	//Setup an intent to create a new contact
	            	Intent intent = new Intent(this, NewContactActivity.class);
					intent.putExtra("address", address);
					intent.putExtra("name", name);
					this.startActivityForResult(intent, SETUP_KEY);
            	}
	            break;
            case SETUP_KEY:
            	if(resultCode == Activity.RESULT_OK) {
            		//Get information back from the NewContactActivity
            		String address = data.getStringExtra("address");
            		String name = data.getStringExtra("name");
            		byte[] key = data.getByteArrayExtra("key");
            		
            		//Create the new contact and fill in the data
            		Key contact = new Key();
            		contact.displayName = name;
            		contact.key = key;
                	
            		//Add the contact
                	KeyManager.getInstance().getLookup().put(address, contact);
                	
                	//Re-Populate the listing
                	adapter.populate();
            	}
            	break;
            }
        }
    }
	@Override
	public void onResume(){
		super.onResume();
	}
	
	@Override
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
                    
                    hashdata.createTempData(text);
                    
                    Intent cx = new Intent(KeyManagementActivity.this, DisplayContactsActivity.class);
    	            startActivityForResult(cx, SELECT_CONTACT);
            		
				} catch (Exception e) {
					e.printStackTrace();
				} 
     
        } 
       
	}
	
	/**
	 * An adapter that displays contacts
	 * @author Richard Laughlin
	 */
	private static class KeyManagementAdapter extends BaseExpandableListAdapter {

		private abstract class ClickListener implements OnClickListener {
			protected Contact contact;
			
			public ClickListener(Contact contact) {
				this.contact = contact;
			}
			
		}
		
		private int lastExpandedGroupPosition = -1;
		private KeyManagementActivity context;
		private ArrayList<Contact> contacts = new ArrayList<Contact>();
		
		public KeyManagementAdapter(KeyManagementActivity keyActivity) {
			this.context = keyActivity;
		}
		
		public void populate() {
			//Clear the contacts to avoid duplicates
			contacts.clear();
			
			//Load the contacts from the KeyManager map into the list
			for(Entry<String, Key> entry : KeyManager.getInstance().getLookup().entrySet()) {
				Contact c = new Contact();
				Key value = entry.getValue();
				
				c.address = entry.getKey();
				c.name = value.displayName;
				
				contacts.add(c);
			}
			
			//Sort the list in alphabetical order
			java.util.Collections.sort(contacts, new Comparator<Contact>() {

				public int compare(Contact arg0, Contact arg1) {
					return arg0.name.compareTo(arg1.name);
				}
				
			});
			
			//Notify to update the view
			this.notifyDataSetChanged();
		}
		
		public Object getChild(int groupPosition, int childPosition) {
			return contacts.get(groupPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
			Contact contact = (Contact) getGroup(groupPosition);
			if(view == null) {
				LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inf.inflate(R.layout.management_child, null);
			}
			
			Button edit = (Button) view.findViewById(R.id.edit);
			edit.setOnClickListener(new ClickListener(contact) {

				public void onClick(View v) {
					Intent intent = new Intent(context, NewContactActivity.class);
					intent.putExtra("address", contact.address);
					intent.putExtra("name", contact.name);
					intent.putExtra("key", KeyManager.getInstance().getLookup().get(contact.address).key);
					context.startActivityForResult(intent, SETUP_KEY);
				}
				
			});
			
			Button viewButton = (Button) view.findViewById(R.id.view);
			viewButton.setOnClickListener(new ClickListener(contact) {
				public void onClick(View v) {
					 Key key = KeyManager.getInstance().getLookup().get(contact.address);
					
					//Display the QR code
            		sendKey(context, new String(Base64.encode(key.key)));
            		
				}
			});
			
			Button delete = (Button) view.findViewById(R.id.delete);
			delete.setOnClickListener(new ClickListener(contact) {
				public void onClick(View v) {
					Dialogs.showConfirmation(context, R.string.validate, new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							KeyManager.getInstance().getLookup().remove(contact.address);
							populate();
						}
					});
				}
			});
				
			return view;
		}

		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		public Object getGroup(int groupPosition) {
			return contacts.get(groupPosition);
		}

		public int getGroupCount() {
			return contacts.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
	    public void onGroupExpanded(int groupPosition){
	        //collapse the old expanded group, if not the same
	        //as new group to expand
	        if(groupPosition != lastExpandedGroupPosition){
	            context.getExpandableListView().collapseGroup(lastExpandedGroupPosition);
	        }

	        super.onGroupExpanded(groupPosition);           
	        lastExpandedGroupPosition = groupPosition;
	    }
		
		public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
			
			Contact contact = (Contact) getGroup(groupPosition);
			if(view == null) {
				LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inf.inflate(R.layout.management_group, null);
			}
			
			TextView tv = (TextView) view.findViewById(R.id.contact);
			tv.setText(contact.name);
			
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}

	
}
