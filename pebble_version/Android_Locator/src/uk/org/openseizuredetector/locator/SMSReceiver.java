/**
 * 
 */
package uk.org.openseizuredetector.locator;

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
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Graham Jones
 * Class to respond to SMS messages.  The action taken depends on the 
 * contents of the SMS message.
 * If it contains the correct password, the current location is determined, 
 * and an SMS reply sent with details
 * of the current locations.
 * If it contains "geo:", a map viewer (such as OsmAnd) is opened to display 
 * the location.
 */
public class SMSReceiver extends BroadcastReceiver 
    implements LocationReceiver 
{    
    /** is the SMS Responder active? */
    boolean mActive;
    /** is the GeoSMS dispaly function active? */
    boolean mGeoSMS;
    /** Password required for location response */
    String mPassword;
    /** Base text of location response message */
    String mMessageText;

    LocationFinder2 lf = null;
    Context mContext = null;
    String smsNumber = null;
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
	// retrieve settings
	getPrefs(contextArg);
	Log.d(TAG, "onReceive()");
	if (bundle == null) {
	    //showToast("Empty SMS Message Received - Ignoring");
	    Log.d(TAG, "onReceive() - Empty SMS Message - Ignoring");
	} else {
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
		Log.d(TAG, "onReceive() - msg = "+str);
	    }
	    String msg0 = msgs[0].getMessageBody().toString();
	    // Check if it is a location request.
	    if (mActive && msg0.toUpperCase().contains(mPassword.toUpperCase())) {
		// Start the LocationFinder service if it is not running.
		if (lf==null) {
		    lf = new LocationFinder2(contextArg);
		}
		Log.d(TAG, "onReceive() - Message contains the Password - getting location...");
		showToast("WAYN Password found - getting Location....");
		// Get the location using the LocationFinder.
		smsNumber = msgs[0].getOriginatingAddress();
		lf.getLocationLL((LocationReceiver) this);
	    } else {
		Log.d(TAG, "onReceive() - inactive, or Message does "
		      +"not contain Password - Ignoring");
	    }
	    // Check if it is a GeoSMS message
	    if (mGeoSMS && msg0.toUpperCase().contains("GEO:")) {
		Log.d(TAG, "onReceive() - Message contains geo: "
		      +"- displaying location...");
		int nPos = msg0.toUpperCase().indexOf("GEO:");
		String uriStr = msg0.substring(nPos);
		Log.d(TAG,"onReceive() - uriStr = "+uriStr);
		showToast("GeoSMS Received - displaying on Map\n("+uriStr+")");
		Uri uri = Uri.parse(uriStr);
		Intent intent = new Intent(
			android.content.Intent.ACTION_VIEW, uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	    } else {
		Log.d(TAG, "onReceive() - GeoSMS Inactive or Message "
		      +"does not contain geo: - Ignoring");
	    }
	}
    }
    
    private void getPrefs(Context context) {
	SharedPreferences SP = PreferenceManager
	    .getDefaultSharedPreferences(mContext);
	mActive = SP.getBoolean("RespondToSMS", true);
	mGeoSMS = SP.getBoolean("DisplayGeoSMS", true);
	mPassword = SP.getString("Password", "WAYN");
	mMessageText = SP.getString("MessageText", "OSDLocator Response:\n");
    }
    

    /**
     * onLocationFound()
     * Called once we know our location - sends SMS message with the locatin
     * to the number that requested it.
     */
    public void onLocationFound(LonLat ll) {
	Log.d(TAG, "onLocationFound.");
	String resultStr;
	if (ll != null) {
	    resultStr = mMessageText+"\n"
		+ll.toGeoUri()+"\n"
		+ll.date()
		;
	    Log.d(TAG, "resultStr=" + resultStr);
	    Log.d(TAG, "onLocationFound() - Replying resultStr="+resultStr);
	    
	    // ---display the new SMS message on the screen.---
	    Log.d(TAG,"onLocationFound:  " +ll.toString());
	} else {
	    Log.d(TAG, "onLocationFound() - Failed to find location - ll is null");
	    resultStr = mMessageText+"\n - Location Unknown.";
	}
	showToast("WAYN Replying: \n" + resultStr);
	// Now reply to the message with our location.
	SmsManager sm = SmsManager.getDefault();
	sm.sendTextMessage(smsNumber, null, resultStr, null, null);	
    }
    

    // Show a 'Toast' message box.
    private void showToast(String msg) {
	Toast.makeText(mContext,
		       msg,
		       Toast.LENGTH_SHORT).show();

    }

}
