package com.joro.imutest.application;

import android.app.Application;

public class IMUTest extends Application{
	
	private boolean trackingOn=false;
	
	public void setTrackingState(boolean state){
		trackingOn=state;
	};
	
	public boolean checkTrackingState(){
		return trackingOn;
	};
	
	
}
