package com.project.qrypto.util;

import java.util.Hashtable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class Decoder {
    
    public static String getQrCodeText(final String path) {
    	Bitmap bMap = BitmapFactory.decodeFile(path);
    	
    	// resize original image
    	bMap = Bitmap.createScaledBitmap(bMap, 100, 100, true);
    	
    	// copy image 
    	bMap = bMap.copy(Bitmap.Config.ARGB_8888, true);
    	
    	// get image size
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(),
                bMap.getHeight());
        LuminanceSource source = new com.google.zxing.RGBLuminanceSource(
                bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    	
        // try to avoid NotFoundException 
        Hashtable<DecodeHintType, Object> hint = new Hashtable<DecodeHintType, Object>();
        hint.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);

        // decoding
        QRCodeReader QRreader = new QRCodeReader();
        Result result = null;
    	
    	try {
			result = QRreader.decode(bitmap, hint);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String text = result.getText();
    	Log.i("decoder working result", text);
    	
    	return text;
    }

}
