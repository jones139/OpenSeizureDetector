/**
 * 
 */
package uk.org.openseizuredetector.wayn;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Graham Jones
 * Class to respond to SMS messages.  The action taken depends on the 
 * contents of the SMS message.
 * If it contains 'WAYN', the current location is determined, 
 * and an SMS reply sent with details
 * of the current locations.
 */
public class SMSReceiver extends BroadcastReceiver 
    implements LocationReceiver 
{    
    /**
     * 
     */
    boolean mActive;
    String mPassword;
    LocationFinder lf = null;
    Context mContext = null;
    String smsNumber = null;
    int mTimeOutSec = 60;
    boolean mUseGPS = true;
    String TAG = "SMSReceiver";
    
    /*
     * (non-Javadoc)
     * 
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
	public void onReceive(Context contextArg, Intent intentArg) {
	//---get the SMS message passed in---
	Bundle bundle = intentArg.getExtras();        
	SmsMessage[] msgs = null;
	mContext = contextArg;
	getPrefs(contextArg);
	Log.d(TAG, "onReceive()");
	if (bundle == null) {
	    showToast("Empty SMS Message Received - Ignoring");
	    Log.d(TAG, "onReceive() - Empty SMS Message - Ignoring");
	} else {
	    if (mActive) {
		Log.d(TAG, "onReceive() - mActive, so processing...");
		//---retrieve the SMS message received---
		Object[] pdus = (Object[]) bundle.get("pdus");
		msgs = new SmsMessage[pdus.length];            
		for (int i=0; i<msgs.length; i++){
		    String str = "";
		    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
		    str += "SMS from " + msgs[i].getOriginatingAddress();                     
		    str += " :";
		    str += msgs[i].getMessageBody().toString();
		    str += "\n";        
		    showToast(str);
		}
		String msg0 = msgs[0].getMessageBody().toString();
		if (msg0.toUpperCase().contains(mPassword)) {
		    // Start the LocationFinder service if it is not running.
		    if (lf==null) {
			lf = new LocationFinder(contextArg);
		    }
		    Log.d(TAG, "onReceive() - Message contains the Password - getting location...");
		    showToast("WAYN Password found - getting Location....");
		    // Get the location using the LocationFinder.
		    smsNumber = msgs[0].getOriginatingAddress();
		    lf.getLocationLL((LocationReceiver) this,mTimeOutSec,mUseGPS);
		} else if (msg0.toUpperCase().contains("GEO:")) {
		    Log.d(TAG, "onReceive() - Message contains geo: - displaying location...");
		    int nPos = msg0.toUpperCase().indexOf("GEO:");
		    String uriStr = msg0.substring(nPos);
		    Log.d(TAG,"onReceive() - uriStr = "+uriStr);
		    showToast("onReceive() - uriStr = "+uriStr);
		    Uri uri = Uri.parse(uriStr);
		    Intent intent = new Intent(
			 android.content.Intent.ACTION_VIEW, uri);
		    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    mContext.startActivity(intent);
		} else {
		    Log.d(TAG, "onReceive() - Message does not contain Password or geo: - Ignoring");

		    showToast("Message does not contain WAYN Password or geo: - ignoring");
		}
	    } else {
		Log.d(TAG, "onReceive() - mActive False - Ignoring");
		showToast("WAYN Disabled - Ignoring");
	    }
	}
    }
    
    private void getPrefs(Context context) {
	SharedPreferences settings = context.getSharedPreferences(
								  "WhereAreYou", Context.MODE_PRIVATE);
	mActive = settings.getBoolean("Active", true);
	mPassword = settings.getString("Password", "WAYN");
	mTimeOutSec = settings.getInt("TimeOutSec", 60);
	mUseGPS = settings.getBoolean("UseGPS",true);
    }
    

    /**
     * onLocationFound()
     * Called once we know our location - sends SMS message with the locatin
     * to the number that requested it.
     */
    public void onLocationFound(LonLat ll) {
	Log.d(TAG, "onLocationFound.");
	if (ll != null) {
	    //String resultStr = "Location is "+ll.toStr()+"\n<a href=\""+ll.toGeoUri()+"\">View on Map</a>";
	    String resultStr = "Location is "+ll.toStr()+"\n"+ll.toGeoUri();
	    Log.d(TAG, "resultStr=" + resultStr);
	    Log.d(TAG, "onLocationFound() - Replying resultStr="+resultStr);
	    
	    // ---display the new SMS message on the screen.---
	    Log.d(TAG,"onLocationFound:  " +ll.toString());
	    showToast("Replying: " + resultStr);
	    // Now reply to the message with our location.
	    SmsManager sm = SmsManager.getDefault();
	    sm.sendTextMessage(smsNumber, null, resultStr, null, null);
	    
	} else {
	    showToast("Failed to find location - sorry!");
	    Log.d(TAG, "onLocationFound() - Failed to find location - ll is null");
	}
	
    }
    
   // Callback for debugging info from LocationFinder
    public void msgBox(String msg) {
	// TODO Do nothing - we operate silently...
	Log.d(TAG,"msgbox() - msg = "+msg);
    }


    // Show a 'Toast' message box.
    private void showToast(String msg) {
	Toast.makeText(mContext,
		       msg,
		       Toast.LENGTH_SHORT).show();

    }

}
