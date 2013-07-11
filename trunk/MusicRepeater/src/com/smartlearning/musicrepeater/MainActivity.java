package com.smartlearning.musicrepeater;

import java.io.File;
import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private TextView mTextView;
	private TextView mPositionTextView;
	private Button mStartButton;
	private Button mPauseButton;
	private ProgressBar mProgressBar;
	private SeekBar mSeekBar;
	private SeekBar mSeekBar1;
	private SeekBar mSeekBar2;
	private SeekBar mSeekBar3;
	private MediaPlayer mMediaPlayer = null;
	private MediaRecorder mMediaRecorder = null;

	private static final int PICK_AUDIO = 1001;
	public static final String MIMETYPE_ALLAUDIOS = "audio/*";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mStartButton = (Button) findViewById(R.id.startButton);
		mStartButton.setOnClickListener(mStartButtonClicked);

		mPauseButton = (Button) findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(mPauseButtonClicked);

		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mSeekBar.setOnSeekBarChangeListener(mSeekBarChanged);

		mTextView = (TextView) findViewById(R.id.textView1);
		// mTextView.setText("Click start to play!");

		mPositionTextView = (TextView) findViewById(R.id.positionTextView);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mSeekBar3 = (SeekBar) findViewById(R.id.seekBar3);
		mSeekBar3.setOnSeekBarChangeListener(mSeekBarChanged);

		mSeekBar1 = (SeekBar) findViewById(R.id.seekBar1);
		mSeekBar1.setMax(mVolumeRank.length - 1);

		mSeekBar2 = (SeekBar) findViewById(R.id.seekBar2);
		mSeekBar2.setMax(mVolumeRank.length - 1);
		mSeekBar2.setProgress(mVolumeRank.length / 2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void start() {
		Intent audioIntent = new Intent(Intent.ACTION_GET_CONTENT);
		audioIntent.setType(MIMETYPE_ALLAUDIOS);
		startActivityForResult(audioIntent, PICK_AUDIO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case PICK_AUDIO:
				Cursor audioCursor = getContentResolver().query(data.getData(),
						null, null, null, null);
				audioCursor.moveToFirst();
				String audioPath = audioCursor.getString(1); // music path
				Uri uir = Uri.fromFile(new File(audioPath));
				mTextView.setText(audioPath);// + ", " + uir);
				play(uir);
				break;
			}
		}
	}

	private void play(Uri uri) {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

		mMediaPlayer = MediaPlayer.create(this, uri);
		mMediaPlayer.start();

		mMediaPlayer.setOnCompletionListener(mMediaPlayerCompleted);// 设置对播放结束事件的监听器
		mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);

		mMediaPlayer.setVolume((float) 0.5, (float) 0.5);
		mSeekBar.setProgress(50);

		// mTextView.setText("Now music is playing~");

		changeProgressBar();// 用Porgressbar来指示播放的进度

		startRecorder();
	}

	private void startRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.stop();
			mMediaRecorder.release(); // Now the object cannot be reused
		}

		String path = Environment
				.getExternalStoragePublicDirectory(AUDIO_SERVICE) + "1.amr";

		// mTextView.setText(path);

		mMediaRecorder = new MediaRecorder();

		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mMediaRecorder.setOutputFile(path);
		mMediaRecorder.setAudioEncodingBitRate(12200);

		try {
			mMediaRecorder.prepare();
		} catch (IOException exception) {

			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
			return;
		}

		// Handle RuntimeException if the recording couldn't start
		try {
			mMediaRecorder.start(); // Recording is now started

		} catch (RuntimeException exception) {
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
			return;
		}

		mHandler.postDelayed(mUpdateTimer, 5000);
	}

	private void stopRecorder() {
		if (mMediaRecorder == null) {
			return;
		}

		mMediaRecorder.stop();
		mMediaRecorder.release(); // Now the object cannot be reused

		mMediaRecorder = null;
	}

	private void updateProgress() {
		int position = mMediaPlayer.getCurrentPosition();
		int pos = position / 1000;
		mPositionTextView.setText("" + pos + " seconds");
		mProgressBar.setProgress(position);
		// mSeekBar3.setProgress(position);
		
	}

	private void changeProgressBar() {
		mProgressBar.setMax(mMediaPlayer.getDuration());
		mSeekBar3.setMax(mMediaPlayer.getDuration());

		new Thread(new Runnable() {
			public void run() {
				while (true) {
					mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public OnCompletionListener mMediaPlayerCompleted = new OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub
			// mTextView.setText("music play is completed!");
			// mTextView.append("\n Click start to replay");
			stopRecorder();
		}
	};

	public OnClickListener mStartButtonClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stubMSG_UPDATE_PROGRESS
			if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
				start();
			}
		}
	};

	public OnClickListener mPauseButtonClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
			} else {
				mMediaPlayer.start();
			}
		}
	};
	
	void setVolume(int musicVol) {
		if (mMediaPlayer == null) {
			return;
		}
		
		mMediaPlayer.setVolume((float) (0.01 * musicVol),
				(float) (0.01 * musicVol));
	}
	
	void setPosition(int position) {
		if (mMediaPlayer == null) {
			return;
		}
		
		mMediaPlayer.seekTo(position);
		Toast.makeText(this, "Seek to " + position / 1000, Toast.LENGTH_LONG)
				.show();
	}

	public OnSeekBarChangeListener mSeekBarChanged = new OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (seekBar == mSeekBar) {
				setVolume(progress);
				
			} else if (seekBar == mSeekBar3) {
				setPosition(progress);
			}

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}
	};

	final Handler mHandler = new MyHandler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			updateVolume();
		}
	};

	private static final int mVolumeRank[] = { 0, 512, 1024, 2048, 4096, 8192,
			16384, 32767 };

	private void updateVolume() {

		if (mMediaRecorder == null) {
			return;
		}

		int volume = mMediaRecorder.getMaxAmplitude();
		int size = mVolumeRank.length;
		int value = size;
		for (int i = 0; i < size; i++) {
			if (volume < mVolumeRank[i]) {
				break;
			}
			value = i;
		}

		Log.e(TAG, "Volume " + value + ", " + volume);
		mSeekBar1.setProgress(value);

		if (value > mSeekBar2.getProgress()) {
			mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() - 10000);
			Toast.makeText(this, "Replay the last 10 seconds",
					Toast.LENGTH_LONG).show();
		} else {
			mHandler.postDelayed(mUpdateTimer, 200);
		}
	}

	MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer arg0) {
			mHandler.postDelayed(mUpdateTimer, 200);
		}

	};

	private static final int MSG_UPDATE_PROGRESS = 101;

	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				updateProgress();
				break;
			default:
				break;
			}
		}
	}
}
