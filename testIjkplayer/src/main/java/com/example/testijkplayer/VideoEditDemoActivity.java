package com.example.testijkplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.exceptions.Pragma;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.example.videoeditordemo.R;

public class VideoEditDemoActivity extends Activity{

	String videoPath=null;
	VideoEditor mVideoEditor = new VideoEditor();
	ProgressDialog  mProgressDialog;
	int videoDuration;
	  @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
	        
			 setContentView(R.layout.video_edit_demo_layout);
	        
			 videoPath=getIntent().getStringExtra("videopath");
			 
	        findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(videoPath==null)
						return ;
					if(videoDuration>60*1000){//大于60秒
						showHintDialog();
					}else{
						new SubAsyncTask().execute();
					}
				}
			});
	        
	        MediaPlayer  player=new MediaPlayer();
	        try {
				player.setDataSource(videoPath);
				   player.prepareAsync();
			        player.setOnPreparedListener(new OnPreparedListener() {
						
						@Override
						public void onPrepared(MediaPlayer mp) {
							// TODO Auto-generated method stub
							videoDuration=mp.getDuration();
							Log.i("sno","MediaPlayer mPreparedListener:"+videoDuration);
							mp.release();
						}
					});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  } 
		private void showHintDialog()
		{
			new AlertDialog.Builder(this)
			.setTitle("提示")
			.setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
	        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					new SubAsyncTask().execute();
				}
			})
			.setNegativeButton("取消", null)
	        .show();
		}
	  public class SubAsyncTask extends AsyncTask<Object, Object, Boolean>{
			  @Override
			protected void onPreExecute() {
			// TODO Auto-generated method stub
				  mProgressDialog = new ProgressDialog(VideoEditDemoActivity.this);
		          mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		          mProgressDialog.setMessage("正在处理中...");
		        
		          mProgressDialog.setCanceledOnTouchOutside(false);
		          mProgressDialog.show();
		          
				  	super.onPreExecute();
			}
      	    @Override
      	    protected synchronized Boolean doInBackground(Object... params) {
      	    	// TODO Auto-generated method stub
      	    	demoVideoGray();
      	    	return null;
      	    }
    	@Override
    	protected void onPostExecute(Boolean result) { 
    		// TODO Auto-generated method stub
    		super.onPostExecute(result);
    		if( mProgressDialog!=null){
	       		 mProgressDialog.cancel();
	       		 mProgressDialog=null;
    		}
    	}
    }
	  private void demoVideoGray()
	  {
		  List<String> cmdList=new ArrayList<String>();
	    	cmdList.add("-vcodec");
	    	cmdList.add("lansoh264_dec");  //使用我们的硬解码加速
			cmdList.add("-i");
			cmdList.add(videoPath);
			cmdList.add("-vf");
			cmdList.add("format=gray");
			cmdList.add("-vcodec");
			cmdList.add("lansoh264_enc"); //使用我们的硬编码加速
			cmdList.add("-strict");
			cmdList.add("-2");
			cmdList.add("-y");
			cmdList.add("/sdcard/video_demo_gray.mp4");
			String[] command=new String[cmdList.size()];  
		     for(int i=0;i<cmdList.size();i++){  
		    	 command[i]=(String)cmdList.get(i);  
		     }  
		     mVideoEditor.executeVideoEditor(command);
	  }
}

