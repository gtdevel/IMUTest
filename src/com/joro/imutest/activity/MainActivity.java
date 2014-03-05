package com.joro.imutest.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.joro.imutest.R;
import com.joro.imutest.application.IMUTest;
import com.joro.imutest.service.TrackingService;

public class MainActivity extends Activity {

	Button button;
	IMUTest state;
	Context context;
	Intent motionIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		state = (IMUTest) getApplicationContext();
		button = (Button) findViewById(R.id.button1);
		context=this;
		
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(state.checkTrackingState()==true){
					button.setText(getString(R.string.button_start));
					state.setTrackingState(false);
					motionIntent=new Intent(context,TrackingService.class);
					stopService(motionIntent);
				}else{	
					button.setText(getString(R.string.button_stop));
					state.setTrackingState(true);
					motionIntent=new Intent(context,TrackingService.class);
					startService(motionIntent);
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		if(isMyServiceRunning()==true){
			state.setTrackingState(true);
			button.setText(getString(R.string.button_stop));
		}else{
			state.setTrackingState(false);
			button.setText(getString(R.string.button_start));
		}
		// TODO Auto-generated method stub
		super.onResume();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (TrackingService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}
