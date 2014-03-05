package com.android.joro.testapp;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "Timeline";
	private static final String TAG2 = "TimelineReceiving";
	private static final boolean D = true;

	public static final String NEW_RESULT_INTENT = "com.joro.biosense.NEW_RESULT_INTENT";
	private Intent intent;

	// Column names of SQLite ResultsData database
	public static final String C_ID = "_id";
	public static final String C_CREATED_AT = "created_at";
	public static final String C_PULSE = "pulse";
	public static final String C_OXY = "oxy";
	public static final String C_USER = "user";
	public static final String C_UPLOADED = "uploaded";

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	// Key name received from UpdateSpreadsheetService Broadcast
	public static final String STATUS = "status";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	// Insecure connection will not be used
	// private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Layout Views
	public TextView mTextView;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;

	// Local Bluetooth adapter
	private static BluetoothAdapter mBluetoothAdapter = null;

	// Member object for the chat services
	private BluetoothCommService mChatService = null;

	// Intent filter use to create filter for UpdateSpreadsheetService broadcast
	private IntentFilter filter;

	// Results data to access SQLite Database
	private ResultsData dbHelper;

	// Application context to be stored in this variable
	private static Context context;

	// Cursor adapter
	Cursor mCursor;

	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	private XYSeries mCurrentSeries;

	private XYSeriesRenderer mCurrentRenderer;

	private String mDateFormat;

	private GraphicalView mChartView;
	
	public double x;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Setting up UpdateReceiver broadcast receiver
		filter = new IntentFilter("com.joro.biosense.UPDATE_INTENT");

		// Setting layout of from xml resource file
		setContentView(R.layout.main);
		context = getApplicationContext();

		// Open Database helper
		dbHelper = new ResultsData(this);
		dbHelper.open();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Set initial link textview to specific string value
		mTextView = (TextView) findViewById(R.id.textConnectivityStatus);

		// Setting up Chart
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
		mRenderer.setAxisTitleTextSize(16);
		mRenderer.setChartTitleTextSize(20);
		mRenderer.setLabelsTextSize(15);
		mRenderer.setLegendTextSize(15);
		mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
		mRenderer.setZoomButtonsVisible(true);
		mRenderer.setPointSize(10);

		String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
		XYSeries series = new XYSeries(seriesTitle);
		mDataset.addSeries(series);
		mCurrentSeries = series;
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		mRenderer.addSeriesRenderer(renderer);
		renderer.setPointStyle(PointStyle.POINT);
		renderer.setFillPoints(true);
		mCurrentRenderer = renderer;
		x=0;
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		setupChart();
		if (D)
			Log.e(TAG, "+ ON RESUME +");
		// Bluetooth Communication service is started only if it hasn't been
		// started already.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothCommService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}

	}

	/**
     * 
     */
	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the BluetoothCommService to perform bluetooth connections
		mChatService = new BluetoothCommService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	/**
	 * NOT CURRENTLY USED-Used if device should be made discoverable to other
	 * Bluetooth devices for pairing.
	 * 
	 */
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message to connected device. In current format, it is only used
	 * to send the initial message to Norin measurement device to set up the
	 * desired output format of the data.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothCommService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothCommService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			// mOutEditText.setText(mOutStringBuffer);
		}
	}

	// Can't use this in Gingerbread. Only available in Honeycomb
	/*
	 * private final void setStatus(int resId) { final ActionBar actionBar =
	 * getActionBar(); actionBar.setSubtitle(resId); }
	 * 
	 * private final void setStatus(CharSequence subTitle) { final ActionBar
	 * actionBar = getActionBar(); actionBar.setSubtitle(subTitle); }
	 */

	// The Handler that gets information back from the BluetoothCommService
	/**
	 * Handler which receives messages from BluetoothCommService and recognizes
	 * state changes in the Bluetooth communication or read/write commands to
	 * and from the bluetooth device.
	 * 
	 */
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothCommService.STATE_CONNECTED:
					mTextView.setText(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					break;
				case BluetoothCommService.STATE_CONNECTING:
					mTextView.setText(getString(R.string.title_connecting));
					break;
				case BluetoothCommService.STATE_LISTEN:
				case BluetoothCommService.STATE_NONE:
					mTextView.setText(getString(R.string.title_not_connected));
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				Log.i(TAG, "Timeline: Writing");
				String writeMessage = new String(toHexString(writeBuf));

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// Construct a string from the valid bytes in the buffer
				//char[] g={(char)readBuf[0]};
				int value = (int)readBuf[0];
				double y= (double)value;
				
				String s=""+value;
				/*try {
					s = new String(readBuf, "US-ASCII");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				mCurrentSeries.add(x, y);
				x++;
				//if(x%100==0){
					mChartView.repaint();
				//}
				Log.d(TAG2, s);
				//String[] readMessage = createOutput(readBuf);
				//Date date = new Date();
				
				// This might effect the accuracy of the timing in the graphing
				// activity
				/*if (!(readMessage[0] == "poor" || readMessage[0] == "0" || readMessage[1] == "0")) {
					dbHelper.addResult(date.toString(), readMessage[2],
							readMessage[0], readMessage[1], "false");
				}*/

				break;
			case MESSAGE_DEVICE_NAME:
				// Display the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				// Displays toast with message
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	/**
	 * After the startActivityForResult is called to start and activity, the
	 * result is passed on to this method and the Bluetooth is either turned on,
	 * or a connection is initiated to a Bluetooth device.
	 * 
	 * @param requestCode
	 *            Request code from requesting activity can either request a
	 *            connection to a Bluetooth device, or request for the Bluetooth
	 *            to be turned on.
	 * @param resultCode
	 *            Results code can either confirm or deny the requested code.
	 * @param data
	 *            Intent containing device address extra.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		// No insecure connection will be available - Leaving it in case of
		// future implementation
		/*
		 * case REQUEST_CONNECT_DEVICE_INSECURE: // When DeviceListActivity
		 * returns with a device to connect if (resultCode ==
		 * Activity.RESULT_OK) { connectDevice(data, false); } break;
		 */
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/**
	 * @param data
	 *            Intent containing device address extra.
	 * @param secure
	 *            This boolean determines if the connection is secure or not. At
	 *            this moment, the secure connection is hard coded.
	 */
	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Inflating menu");
		// Menu layout can be found in xml resource
		getMenuInflater().inflate(R.menu.menu, menu);
		Log.d(TAG, "Menu inflated");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.itemPref:
			/*
			 * serverIntent = new Intent(this, PrefActivity.class);
			 * startActivity(serverIntent
			 * .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
			 */
			break;
		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			break;
		// No insecure connect scan will be available
		case R.id.itemToggleService:
			break;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;

	}

	/**
	 * Relevant data is extracted from bytes that are sent from the Bluetooth
	 * device and is then converted to a string array which can easily be
	 * outputed.
	 * 
	 * @param bytes
	 *            Array of bytes sent by bluetooth device.
	 * @return String array containing pulse, oxygen percentage and username
	 */
	public String[] createOutput(byte[] bytes) {

		String username = new String();
		// Setting up application to access shared preferences
		username = "George";

		String[] finalMessage = { "", "", "" };
		int val = byteArrayToInt(bytes, 0);
		// First to masks are for heart rate measurements
		int mask1 = 0x03000000;
		int mask2 = 0x007F0000;
		// Second mask is for oxygen level
		int mask3 = 0x0000FF00;
		// Third mask is for Smart Point Algorithm (quality measurement)
		int mask4 = 0x00000020;

		// Shifting of bits to the right
		int pulse = (val & mask1) >> 17 | (val & mask2) >> 16;
		int oxy = (val & mask3) >> 8;
		int quality = (val & mask4) >> 5;

		if (oxy <= 100 && quality == 1) {
			finalMessage[0] = String.valueOf(pulse);
			finalMessage[1] = String.valueOf(oxy);
			finalMessage[2] = username;
		} else {
			finalMessage[0] = "poor";
			finalMessage[1] = "poor";
			finalMessage[2] = username;
		}
		return finalMessage;
	}

	/**
	 * @param b
	 *            Byte to be converted
	 * @param offset
	 *            Offset of byte
	 * @return Converted integer
	 */
	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}
	
	void setupChart(){
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
			mChartView = ChartFactory.getLineChartView(this, mDataset,
					mRenderer);
			mRenderer.setClickEnabled(true);
			mRenderer.setSelectableBuffer(100);
			
			mChartView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					SeriesSelection seriesSelection = mChartView
							.getCurrentSeriesAndPoint();
					double[] xy = mChartView.toRealPoint(0);
					if (seriesSelection == null) {
						Toast.makeText(MainActivity.this,
								"No chart element was clicked",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(
								MainActivity.this,
								"Chart element in series index "
										+ seriesSelection.getSeriesIndex()
										+ " data point index "
										+ seriesSelection.getPointIndex()
										+ " was clicked"
										+ " closest point value X="
										+ seriesSelection.getXValue() + ", Y="
										+ seriesSelection.getValue()
										+ " clicked point value X="
										+ (float) xy[0] + ", Y="
										+ (float) xy[1], Toast.LENGTH_SHORT)
								.show();
					}
				}
			});
			
			mChartView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					SeriesSelection seriesSelection = mChartView
							.getCurrentSeriesAndPoint();
					if (seriesSelection == null) {
						Toast.makeText(MainActivity.this,
								"No chart element was long pressed",
								Toast.LENGTH_SHORT);
						return false; // no chart element was long pressed, so
										// let something
						// else handle the event
					} else {
						Toast.makeText(MainActivity.this,
								"Chart element in series index "
										+ seriesSelection.getSeriesIndex()
										+ " data point index "
										+ seriesSelection.getPointIndex()
										+ " was long pressed",
								Toast.LENGTH_SHORT);
						return true; // the element was long pressed - the event
										// has been
						// handled
					}
				}
			});
			
			mChartView.addZoomListener(new ZoomListener() {
				public void zoomApplied(ZoomEvent e) {
					String type = "out";
					if (e.isZoomIn()) {
						type = "in";
					}
					System.out.println("Zoom " + type + " rate "
							+ e.getZoomRate());
				}

				public void zoomReset() {
					System.out.println("Reset");
				}
			}, true, true);
			mChartView.addPanListener(new PanListener() {
				public void panApplied() {
					System.out.println("New X range=["
							+ mRenderer.getXAxisMin() + ", "
							+ mRenderer.getXAxisMax() + "], Y range=["
							+ mRenderer.getYAxisMax() + ", "
							+ mRenderer.getYAxisMax() + "]");
				}
			});
			layout.addView(mChartView, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		} else {
			mChartView.repaint();
		}
	}

	/**
	 * @param bytes
	 *            Byte to be converted to hexadecimal string
	 * @return Hexadecimal string
	 */
	public static String toHexString(byte[] bytes) {
		char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v / 16];
			hexChars[j * 2 + 1] = hexArray[v % 16];
		}
		return new String(hexChars);
	}

}