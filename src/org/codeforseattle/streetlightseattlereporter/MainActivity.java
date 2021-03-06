/**
 * The main activity class. The support version is used so that 
 * the app runs successfully on Gingerbread.
 */
package org.codeforseattle.streetlightseattlereporter;

import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.annotation.TargetApi;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initializeGui();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void initializeGui()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			EditText nameField = (EditText) findViewById(R.id.name_field);
			String userName = getUserName();
			if (userName != null)
				nameField.setText(userName);
		}
		EditText phoneField = (EditText) findViewById(R.id.phone_field);
		String line1Number = getDefaultPhoneNumber();
		if (line1Number != null)
			phoneField.setText(line1Number);
		
		PackageManager pm = getPackageManager();
		boolean cameraPresent = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		
		Button scanBtn = (Button) findViewById(R.id.scan_barcode_button);
		if (! cameraPresent) // Do not try bar code scanning without the camera.
			scanBtn.setEnabled(false);
	}
	
	/**
	 * Based on code from http://stackoverflow.com/questions/20360506/get-owner-name-of-an-android-device?lq=1
	 * 
	 * @return the phone's display name
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private String getUserName()
	{
		String userName = null;
		Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		if (c.getCount() != 0)
		{
			c.moveToFirst();
			userName = c.getString(c.getColumnIndex("display_name"));
		}
		c.close();
		return userName;
	}
	
	/**
	 * There is no reliable way to get the phone number. If the phone number is current and stored in 
	 * line 1 of the SIM card, this method should return it.
	 * 
	 * @return the phone number
	 */
	private String getDefaultPhoneNumber()
	{
		TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String line1Number = mTelephonyMgr.getLine1Number();
		if (line1Number != null && line1Number.startsWith("1") && line1Number.length() == 11)
		{
			line1Number = line1Number.substring(1);
			String formattedNumber = PhoneNumberUtils.formatNumber(line1Number);
			return formattedNumber;
		}
		return line1Number;
	}
	
	/**
	 * Respond to the button click and scan in the bar code.
	 * 
	 * @param view
	 */
	public void onScanBarcodeButtonClick(View view)
    {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;
      
        if (!isIntentSafe)
        {
        	String title = "Missing ZXing Barcode Scanner";
        	String message = "To scan a barcode, please install the ZXing Barcode Scaner app from Google Playstore.";
        	boolean isHtml = false;
        	boolean displayIntent = true;
        	displayDialog(title, message, isHtml, displayIntent);

            // Check again for the necessary scanner.
            activities = packageManager.queryIntentActivities(intent, 0);
            isIntentSafe = activities.size() > 0;
            if (! isIntentSafe)
            	return;
        }
        intent.putExtra("SCAN_MODE", "CODE_39"); // Seattle city light poles use Code 39 style bar codes.
      
        try {
    	    startActivityForResult(intent, 0);   // Start the Barcode Scanner.
        }
        catch (ActivityNotFoundException ex)
        {
        	String title = "Caught ActivityNotFoundException";
        	String message = "To scan a barcode, please install the ZXing Barcode Scaner app from Google Playstore.";
        	boolean isHtml = false;
        	boolean displayIntent = true;
        	displayDialog(title, message, isHtml, displayIntent);
        }
    }
	
	/**
	 * After performing the scan, this code places the scan results in the pole number field. Status results, 
	 * including the type of bar code found, could be displayed if the status field were put back into the GUI.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (requestCode == 0)
		{
			// TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
			TextView tvResult = (TextView) findViewById(R.id.pole_number_field);
	      
			if (resultCode == RESULT_OK)
			{
				// tvStatus.setText(intent.getStringExtra("SCAN_RESULT_FORMAT"));
				tvResult.setText(intent.getStringExtra("SCAN_RESULT"));
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				// tvStatus.setText("Press a button to start a scan.");
				tvResult.setText("Scan cancelled.");
			}
		}
	}
	
	/**
	 * Respond to the button click and clear the form.
	 * 
	 * @param view
	 */
	public void onClearFormButtonClick(View view)
	{
		// Ensure that the keyboard disappears when the button is pressed.
		InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
		inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		
		boolean clearEmailAddress = true;
		clearForm(clearEmailAddress);
		
		Toast.makeText(MainActivity.this, "Form cleared.", Toast.LENGTH_SHORT).show();
	}
	
	protected void clearForm(boolean clearEmailAddress)
	{
		Resources resources = getResources();
		clearField(R.id.extension_field, resources.getString(R.string.extension_message));
		clearField(R.id.pole_number_field, resources.getString(R.string.pole_number_message));
		clearField(R.id.address_field, resources.getString(R.string.address_message));
		((Spinner) findViewById(R.id.spinner_problem_type)).setSelection(0);
		clearField(R.id.problem_description_field, resources.getString(R.string.comments_message));
		((EditText) findViewById(R.id.email_field)).requestFocus();
		
		if (clearEmailAddress) {
			clearField(R.id.email_field, resources.getString(R.string.email_message));
			((EditText) findViewById(R.id.email_field)).requestFocus();
		}
		else {
			((EditText) findViewById(R.id.pole_number_field)).requestFocus();
		}
	}
	
	private void clearField(int id, String hint)
	{
	    EditText field = (EditText)findViewById(id);
	    field.setText("");
	    field.setHint(hint);
	}
	
	/**
	 * Get the text from the fields on the form and put it into a string array.
	 * @param view
	 */
	public void onSubmitTroubleReportButtonClick(View view)
	{
		// ensure that the keyboard disappears when the button is pressed.
		InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
		inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
		
		boolean fieldsValid = validateFields();
		if (! fieldsValid)
			return;
		
		String[] str = {((EditText) findViewById(R.id.name_field)).getText().toString().trim(),
			((EditText) findViewById(R.id.phone_field)).getText().toString().trim(),
			((EditText) findViewById(R.id.extension_field)).getText().toString().trim(),
			((EditText) findViewById(R.id.email_field)).getText().toString().trim(),
			((EditText) findViewById(R.id.pole_number_field)).getText().toString().trim(),
			((EditText) findViewById(R.id.address_field)).getText().toString().trim(),
			((Spinner) findViewById(R.id.spinner_problem_type)).getSelectedItem().toString(),
			((EditText) findViewById(R.id.problem_description_field)).getText().toString().trim(),
			getString(R.string.submit_button)};
		
	    FragmentManager fm = getSupportFragmentManager();
	    AsyncTaskFragment asyncTaskFragment = (AsyncTaskFragment) fm.findFragmentByTag("asyncTask");

	    if (asyncTaskFragment == null) {
	    	asyncTaskFragment = new AsyncTaskFragment();
	      fm.beginTransaction().add(asyncTaskFragment, "asyncTask").commit();
	    }
		asyncTaskFragment.performHttpPostRequest(str);
	}
	
	protected void displayDialog(String title, String message, boolean isHtml, final boolean displayIntent)
	{
		BasicAlertDialogFragment alertDialog = new BasicAlertDialogFragment();
		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("message", message);
		args.putBoolean("isHtml", isHtml);
		args.putBoolean("displayIntent", displayIntent);
		alertDialog.setArguments(args);
		alertDialog.show(getSupportFragmentManager(), "alert dialog");
	}
	
	private boolean validateFields()
	{
		String message = "";
		String str = ((EditText) findViewById(R.id.name_field)).getText().toString().trim();
		if (str.equals(""))	{
        	message += "<p>Include your name.</p>";
		}
		str = ((EditText) findViewById(R.id.phone_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Include your phone number.</p>";
		}
		str = ((EditText) findViewById(R.id.email_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Enter your email address.</p>";
		}
		str = ((EditText) findViewById(R.id.pole_number_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Either scan or enter the 7-digit lamppost identifier.</p>";
		}
		str = ((EditText) findViewById(R.id.address_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Note the pole's location.</p>";
		}
		if (message.equals(""))	{
			return true;
		}
		String title = "";
		boolean isHtml = true;
    	boolean displayIntent = false;
    	displayDialog(title, "<html><body><p>Before submitting your report, please fix these issues:</p>" + 
    	    message + "</body></html>", isHtml, displayIntent);
    	return false;
	}

}
