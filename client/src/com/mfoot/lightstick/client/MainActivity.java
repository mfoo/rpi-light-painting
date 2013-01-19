package com.mfoot.lightstick.client;

import java.io.InputStream;
import java.util.Properties;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class MainActivity extends Activity {
	public static final String SERVER_ADDRESS = "192.168.1.1";
	public static final String SERVER_USERNAME = "pi";
	public static final String SERVER_PASSWORD = "root";
	private static final String SERVER_CONNECT_RESPONSE = "server_status";
	private static final String RASPI_WIFI_SSID = "lightstick";

	private static ProgressDialog mDialog;
	private static ImageView SSH_CONNECTION_IMAGE;
	private static Button SSH_CONNECTION_BUTTON;

	private static Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle b = msg.getData();
			
			if(b.containsKey(SERVER_CONNECT_RESPONSE)) {
				// Handle the message to stop the spinner
				Boolean value = b.getBoolean(SERVER_CONNECT_RESPONSE);
				if(value != null && value && mDialog != null) {
					mDialog.cancel();
					SSH_CONNECTION_IMAGE.setImageResource(android.R.drawable.checkbox_on_background);
					SSH_CONNECTION_BUTTON.setEnabled(false);
				}
			}
		}
		
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SSH_CONNECTION_IMAGE = (ImageView) findViewById(R.id.ssh_connection_status);
		SSH_CONNECTION_BUTTON = (Button) findViewById(R.id.ssh_connect);
	}

	@Override
	protected void onResume() {
		super.onResume();

		attachMenuButtonHandlers();
		handleButtonStatus();

		TextView textView = (TextView) findViewById(R.id.wireless_connection);
		textView.setText("dave");
	}

	private void attachMenuButtonHandlers() {
		ImageView imagePickerView = (ImageView) findViewById(R.id.play_menu_button);
		ImageView editImageView = (ImageView) findViewById(R.id.edit_menu_button);
		
		imagePickerView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
				startActivity(intent);
			}
		});
		
		editImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ImageEditorActivity.class);
				startActivity(intent);
			}
		});
		
		// TODO: EditImageView
	}

	private void handleButtonStatus() {
		String name = getCurrentWifiSSID();

		ImageView wirelessStatusCheckBox = (ImageView) findViewById(R.id.wireless_connection_status);
		Button wirelessConnectionButton = (Button) findViewById(R.id.wireless_connect);

		if (RASPI_WIFI_SSID.equals(name)) {
			// Connected to the correct network
			Log.d("bla", "Correct wifi network found");
			
			wirelessStatusCheckBox
					.setImageResource(android.R.drawable.checkbox_on_background);
			wirelessConnectionButton.setEnabled(false);
			mDialog = new ProgressDialog(this);
			mDialog.setMessage("Checking for server connection...");
			mDialog.setCancelable(false);
			mDialog.show();

			Thread sshConnectionCheckerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					boolean status = false;
					try {
						status = makeSSHConnection();
					} catch (Exception e) {
						Log.e(getClass().getName(), e.getMessage());
					} finally {
						Bundle b = new Bundle();
						b.putBoolean(SERVER_CONNECT_RESPONSE, status);
						Message msg = new Message();
						msg.setData(b);
						handler.sendMessage(msg);
					}
				}
			});
			
			sshConnectionCheckerThread.start();

		} else {
			wirelessStatusCheckBox
					.setImageResource(android.R.drawable.checkbox_off_background);
			wirelessConnectionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Open the wireless network selection menu screen
					startActivity(new Intent(
							WifiManager.ACTION_PICK_WIFI_NETWORK));
				}
			});

		}
	}

	private String getCurrentWifiSSID() {
		WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		String name = wifiInfo.getSSID();
		return name;
	}

	private boolean makeSSHConnection() throws Exception {
		Log.d(getClass().getName(), "Attempting SSH connection.");
		JSch jsch = new JSch();
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		config.put("compression.s2c", "zlib,none");
		config.put("compression.c2s", "zlib,none");

		Session session = jsch.getSession(SERVER_USERNAME, SERVER_ADDRESS);
		session.setConfig(config);
		session.setPort(22);
		session.setPassword(SERVER_PASSWORD);
		session.connect();

		Log.d(getClass().getName(), "Session connected");

		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand("ls /");
		InputStream in = channel.getInputStream();

		channel.connect();

		byte[] tmp = new byte[1024];
		while (true) {
			while (in.available() > 0) {
				int i = in.read(tmp, 0, 1024);
				if (i < 0)
					break;
				Log.d(getClass().getName(), new String(tmp, 0, i));
			}
			if (channel.isClosed()) {
				Log.d(getClass().getName(),
						"exit-status: " + channel.getExitStatus());
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception ee) {
			}
		}
		channel.disconnect();
		session.disconnect();

		return true;
	}
	
    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
        }
    }

}
