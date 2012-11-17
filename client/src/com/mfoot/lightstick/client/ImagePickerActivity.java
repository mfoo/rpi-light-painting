package com.mfoot.lightstick.client;

import static com.mfoot.lightstick.client.MainActivity.SERVER_ADDRESS;
import static com.mfoot.lightstick.client.MainActivity.SERVER_PASSWORD;
import static com.mfoot.lightstick.client.MainActivity.SERVER_USERNAME;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ImagePickerActivity extends Activity implements
		MediaScannerConnectionClient {

	private static final String FILE_TYPE = "image/*";
	private static final String IMAGE_FOLDER = "lightscythe";
	private static final String LIGHTSCYTHE_FILE = "input.png";

	private MediaScannerConnection conn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_picker);
	}

	@Override
	public void onResume() {
		super.onResume();
		Button selectImageButton = (Button) findViewById(R.id.select_image);
		selectImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("la", "select image button clicked");
				startScan();
			}
		});
	}

	private void startScan() {
		if (conn != null) {
			conn.disconnect();
		}
		conn = new MediaScannerConnection(this, this);
		conn.connect();
	}

	@Override
	public void onMediaScannerConnected() {
		Log.d("bla", "media scanner connected");
		Log.d("bla:", Environment.getExternalStorageDirectory().toString());

		// Ensure that the media scanner scans the media directory.
		File file = new File(Environment.getExternalStorageDirectory()
				.toString() + "/" + IMAGE_FOLDER);
		conn.scanFile(file.listFiles()[0].getAbsolutePath(), FILE_TYPE);
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		Log.d("bla", "media scan completed");
		try {
			if (uri != null) {
				Log.d("bla", "uri isn't null");
				Intent intent = new Intent(Intent.ACTION_PICK);
				String string = "file://"
						+ Environment.getExternalStorageDirectory()
								.getAbsolutePath() + File.separator
						+ IMAGE_FOLDER + File.separator;
				// Log.d("bla", string);
				uri = Uri.parse(string);
				intent.setDataAndType(uri, FILE_TYPE);

				startActivityForResult(intent, 1);
			} else {
				Log.d("bla", "uri is null");
			}
		} finally {
			conn.disconnect();
			conn = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.d("bla", "activity result code" + RESULT_OK + " " + resultCode);
		if (resultCode == RESULT_OK) {
			Uri photoUri = data.getData();
			if (photoUri != null) {
				Log.d("Bla", "uri not null");
				try {
					Bitmap currentImage = MediaStore.Images.Media.getBitmap(
							this.getContentResolver(), photoUri);
					((ImageView) findViewById(R.id.selected_image))
							.setImageBitmap(currentImage);
					findViewById(R.id.no_image_selected).setVisibility(
							View.INVISIBLE);
					
					ProgressDialog dialog = new ProgressDialog(this);
					dialog.setMessage("Uploading file to server...");
					dialog.setCancelable(false);
					dialog.show();
					uploadFile(photoUri);
					dialog.cancel();
				} catch (Exception e) {
					Log.d("Bla", e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private void uploadFile(Uri imageUri) {
		JSch jsch = new JSch();
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		config.put("compression.s2c", "zlib,none");
		config.put("compression.c2s", "zlib,none");

		Session session;
		try {
			session = jsch.getSession(SERVER_USERNAME, SERVER_ADDRESS);
			session.setConfig(config);
			session.setPort(22);
			session.setPassword(SERVER_PASSWORD);
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			// Get the absolute path of a media store uri
			// see http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
			String[] proj = { MediaStore.Images.Media.DATA };
	        Cursor cursor = managedQuery(imageUri, proj, null, null, null);
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();

			File imageFile = new File(cursor.getString(column_index));
			FileInputStream is = new FileInputStream(imageFile);

			channel.put(is, LIGHTSCYTHE_FILE);
			channel.disconnect();
			session.disconnect();
			
			((Button) findViewById(R.id.play)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					executeScytheCommand("sudo python lightscythe.py");
				}
				
			});
			
			Button stopButton = (Button) findViewById(R.id.stop);
			stopButton.setEnabled(true);
			stopButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO: Keep the PID, this is way too greedy
					executeScytheCommand("sudo killall python");
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("bla", e.getMessage());
		}
	}
	
	private void executeScytheCommand(String command) {
		JSch jsch = new JSch();
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		config.put("compression.s2c", "zlib,none");
		config.put("compression.c2s", "zlib,none");

		Session session;
		try {
			session = jsch.getSession(SERVER_USERNAME, SERVER_ADDRESS);
			session.setConfig(config);
			session.setPort(22);
			session.setPassword(SERVER_PASSWORD);
			session.connect();
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);
			channel.connect();
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
