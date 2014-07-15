package com.project.qrypto.util;

import java.util.HashMap;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Temp {
	// Shared Preferences
	SharedPreferences pref;
	
	// Editor for Shared preferences
	Editor editor;
	
	// Context
	Context _context;
	
	// Shared pref mode
	int PRIVATE_MODE = 0;
	
	// Sharedpref file name
	private static final String PREF_NAME = "temp";
	
	// User name (make variable public to access from outside)
	public static final String KEY_NAME = "data";
	
	
	
	// Constructor
	
	public Temp(Context context) {
		// TODO Auto-generated constructor stub
		this._context = context;
		pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
		editor = pref.edit();
	}

	/**
	 * Create login session
	 * */
	public void createTempData(String data){
		
		// Storing name in pref
		editor.putString(KEY_NAME, data);
		
		// commit changes
		editor.commit();
	}	
	
	/**
	 * Get stored session data
	 * */
	public HashMap<String, String> getUserData(){
		HashMap<String, String> data = new HashMap<String, String>();
		// user name
		data.put(KEY_NAME, pref.getString(KEY_NAME, null));
		
		// return user
		return data;
	}
	
	/**
	 * Clear session details
	 * @return 
	 * */
	

	public void clearData() {
		// TODO Auto-generated method stub
		
		editor.clear();
		editor.commit();	
		
	}
	
}
