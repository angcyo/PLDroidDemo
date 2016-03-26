package com.example.testijkplayer;


import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Video;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import com.example.videoeditordemo.R;


public class MainActivity extends Activity {

	EditText etVideoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.activity_main);
        
        
        etVideoPath=(EditText)findViewById(R.id.id_main_etvideo);
        etVideoPath.setText("/sdcard/2x.mp4");
       
        findViewById(R.id.id_main_demoplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(checkPath())
					startVideoPlayDemo();
			}
		});
        findViewById(R.id.id_main_demoedit).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(checkPath())
					startVideoEditDemo();
			}
		});
    }
    
    private boolean checkPath(){
    	if(etVideoPath.getText()!=null && etVideoPath.getText().toString().isEmpty()){
    		Toast.makeText(MainActivity.this, "请输入视频地址", Toast.LENGTH_SHORT).show();
    		return false;
    	}	
    	else{
    		String path=etVideoPath.getText().toString();
    		if((new File(path)).exists()==false){
    			Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
    			return false;
    		}else{
    			return true;
    		}
    	}
    }
    private void startVideoPlayDemo()
    {
    			String path=etVideoPath.getText().toString();
    			VideoActivity.intentTo(MainActivity.this, path, path);
    }
    private void startVideoEditDemo()
    {
    	String path=etVideoPath.getText().toString();
    	Intent intent=new Intent(MainActivity.this,VideoEditDemoActivity.class);
    	intent.putExtra("videopath", path);
    	startActivity(intent);
    }
}
