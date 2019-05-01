package com.fellow.yoo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class RecordingActivity extends Activity {
	Button play, reset;
	private MediaRecorder myAudioRecorder;
	private String outputFile = null;
	
	private boolean recording = false;
	private int count = 0;
	List<String> recordOutputFiles;
	private Animation recordAnimation;
	private ImageView recordButton;
	private TextView txtRecordTime;
	private TextView textButton;
	private String currentRecordTime;
	
	private int minutes = 0;
	private int seconds = 0;
	private double miliseconds = 0;
	private CountDownTimer countD;
	private byte[] fullRecords;
	private Button post;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recording);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(getString(R.string.record_message));  
		
		post = (Button) findViewById(R.id.button_post);
		play = (Button) findViewById(R.id.button_play);
		reset = (Button) findViewById(R.id.button_reset);
		txtRecordTime = (TextView) findViewById(R.id.textView_record_time);
		textButton = (TextView) findViewById(R.id.textView_record_button);
		txtRecordTime.setText("00:00:000"); 
		
		enableButton(false);
		reset.setVisibility(View.VISIBLE); 
		recordOutputFiles = new ArrayList<String>();
		
		recordAnimation = new AlphaAnimation(0, 1); 
		recordAnimation.setDuration(600); 
		recordAnimation.setInterpolator(new LinearInterpolator()); 
		recordAnimation.setRepeatCount(Animation.INFINITE); 
		recordAnimation.setRepeatMode(Animation.REVERSE);
		
		
	    recordButton = (ImageView) findViewById(R.id.image_record);
	    recordButton.setImageResource(R.drawable.record_audio); 
	    recordButton.setScaleType(ScaleType.FIT_XY);
	    
	    // outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording_" + count + ".3gp";
	    outputFile = this.getCacheDir() + "/recording_" + count + ".3gp";
	    recordButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(final View view) {
	        	if(post.isEnabled()){ // feature not able to pause and continue.
	        		return;	// so it can record on first time or reset only
	        	}
	        	
	        	if(!recording){
	        		startRecord();
	        		recordButton.startAnimation(recordAnimation); 
	        		textButton.setText(getString(R.string.pause_recording));  
	        		countD.start();
	        		if(mediaPlayer != null && mediaPlayer.isPlaying()){
						mediaPlayer.stop();
						play.setText(getString(R.string.play_record));  
					}
	        	}else{
	        		textButton.setText(getString(R.string.start_recording));  
	        		pauseRecord();
	        		view.clearAnimation();
	        		countD.cancel();
	        	}
	        	recording = !recording;
	        }
	    });
		
	    post.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				fullRecords = createCombineAllRecordFile();
				if(fullRecords != null){
					Intent returnIntent = new Intent();
					returnIntent.putExtra("record_time", currentRecordTime);
					returnIntent.putExtra("record_sound", fullRecords);
					setResult(RESULT_OK, returnIntent);
					finish();
		    	}
			}
		});
	    
		
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				if(mediaPlayer != null && mediaPlayer.isPlaying()){
					mediaPlayer.stop();
					play.setText(getString(R.string.play_record));  
				}else{
					fullRecords = createCombineAllRecordFile();
					playRecord();
					play.setText(getString(R.string.stop_record));  
				}
			}
		});
		
		reset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				if(mediaPlayer != null && mediaPlayer.isPlaying()){
					mediaPlayer.stop();
				}
				currentRecordTime = "00:00:000";
				txtRecordTime.setText(currentRecordTime); 
				recordOutputFiles.clear();
				miliseconds = seconds = minutes =count = 0;
				enableButton(false); 
			}
		});
		
		
		countD = new CountDownTimer(60*60*60*1000, 1) {
			 Random r = new Random();
			 
		     public void onTick(long millisUntilFinished) {
		    	 miliseconds+= 1000/58;
		    	 if(miliseconds > 999){
		    		 seconds++;
		    		 miliseconds = r.nextInt(10);
		    	 }
		    	 if(seconds > 59){
		    		 minutes++;
		    		 seconds = 0;
		    	 }
		    	 
		    	 currentRecordTime = getFormatTime();
		    	 txtRecordTime.setText(currentRecordTime); 
		     }
		     public void onFinish() {
		     }
		  };
	}
	
	private void enableButton(boolean status){
		post.setEnabled(status);
		play.setEnabled(status);
		reset.setEnabled(status);
	}
	
	private MediaPlayer mediaPlayer;
	private void playRecord(){
		mediaPlayer = new MediaPlayer();
		try {
			String outFile = getAudioSource(fullRecords);
			mediaPlayer.setDataSource(outFile);
			mediaPlayer.prepare();
		}catch (IOException e) {
			Log.e("Play Record : ", e.toString());
		}
		mediaPlayer.start();
		runTimer();
	}
	
	private void startRecord(){
		myAudioRecorder = new MediaRecorder();
		myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
		myAudioRecorder.setOutputFile(outputFile);
		try {
			myAudioRecorder.prepare();
			myAudioRecorder.start();
		}catch (IllegalStateException e) {
			Log.e("", e.toString());
		}catch (IOException e) {
			Log.e("", e.toString());
		}
		enableButton(false); 
	}
	
	private void pauseRecord(){
		myAudioRecorder.stop();
		myAudioRecorder.release();
		myAudioRecorder = null;
		Log.w("Audio recorded successfully : ", + count + " >> " + outputFile);
		recordOutputFiles.add(outputFile);
		count ++;
		// outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording_" + count + ".3gp";
		outputFile = this.getCacheDir() + "/recording_" + count + ".3gp";
		
		enableButton(true); 
		fullRecords = createCombineAllRecordFile();
	}
	
	private Timer timer;
	private void runTimer(){
		timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            System.err.println("m.getCurrentPosition() : " + mediaPlayer.getCurrentPosition());
                        } else {
                        	play.setText(getString(R.string.play_record)); 
                            timer.cancel();
                            timer.purge();
                        }
                    }
                });
            }
        }, 0, 1000);
	}
	
	protected String getAudioSource(byte[] audioSoundByteArray) {
		FileOutputStream fileOutputStream;
		String outputFile = "";
	    try {
	        File tempMpAudio = File.createTempFile("yooVoiceMassage", ".3gp", this.getCacheDir());
	        tempMpAudio.deleteOnExit();
	        fileOutputStream = new FileOutputStream(tempMpAudio);
	        fileOutputStream.write(audioSoundByteArray);
	        fileOutputStream.close();
	        Log.w("tempMpAudio.getAbsolutePath() : ", tempMpAudio.getAbsolutePath());
	        outputFile = tempMpAudio.getAbsolutePath();
	    } catch (IOException e) {
	        Log.e("Play Audio byte array : ", e.toString());
	    }
	    return outputFile;
	}
	
	
	public byte[] createCombineAllRecordFile(){
		
		byte allFileContent[] = null;
		FileInputStream ins = null;
		FileOutputStream fos = null;
	    try{
	    	
	    	File tempMpAudio = File.createTempFile("record_all", ".3gp", this.getCacheDir());
	    	tempMpAudio.deleteOnExit();
	    	fos = new FileOutputStream(tempMpAudio);
	    	
	    	for (String audName : recordOutputFiles) { 
	    		File f = new File(audName);
				Log.v("Record Message", "File Length : " + f.length());
				byte fileContent[] = new byte[(int) f.length()];
				ins = new FileInputStream(audName);
				int r = ins.read(fileContent);
				Log.v("Record Message", "Number Of Bytes Readed=====>>>" + r);
				fos.write(fileContent);
				Log.v("Record Message", "File " + audName + "is Appended");
			}
	   
	    	allFileContent = readAllByteRecord(tempMpAudio.getAbsolutePath());
	    	ins.close();
	        fos.close();
	        
	        Log.v("Record Message", "Closed combine file");
	        
	    }catch (FileNotFoundException e0){
	    	Log.e("conbine file : FileNotFoundException ", e0.toString());
	    } catch (IOException e1){
	    	Log.e("combine file : IOException ", e1.toString());
	    }
	    
	    return allFileContent;
	}
	
	public byte[] readAllByteRecord(String outputFile){
		
		byte allFileContent[] = null;
		FileInputStream ins = null;
	    try{
	    	File f = new File(outputFile);
			Log.v("Record Message", "out file, length : " + outputFile + ", " + f.length());
			
			byte fileContent[] = new byte[(int) f.length()];
			ins = new FileInputStream(outputFile);
			int r = ins.read(fileContent); 
			Log.v("Record Message", "Number Of Bytes Readed All file=====>>>" + r);
	    	ins.close();
	    	allFileContent = fileContent;
	    }catch (FileNotFoundException e0){
	    	Log.e("conbine file : FileNotFoundException ", e0.toString());
	    } catch (IOException e1){
	    	Log.e("combine file : IOException ", e1.toString());
	    }
	    
	    return allFileContent;
	}
	
	
	private String getFormatTime(){
		StringBuffer time = new StringBuffer();
		if(minutes < 10){
			time.append("0" + minutes + ":");
		}else{
			time.append(minutes + ":");
		}
		
		if(seconds < 10){
			time.append("0" + seconds + ":");
		}else{
			time.append(seconds + ":");
		}
		
		Double d = Double.valueOf(miliseconds);
		if(miliseconds < 10){
			time.append("00" + d.intValue());
		}else if(miliseconds < 100){
			time.append("0" + d.intValue());
		}else{
			time.append(d.intValue());
		}
		return time.toString();
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	        finish();
	        return true;
	    case R.id.action_post:
	    	
	    	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// getMenuInflater().inflate(R.menu.record_message, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	

	@Override
	protected void onDestroy() {
		if(mediaPlayer != null && mediaPlayer.isPlaying()){
			mediaPlayer.stop();
		}
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	
}