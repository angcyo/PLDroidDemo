/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.testijkplayer;

import com.example.videoeditordemo.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.sample.widget.media.AndroidMediaController;
import tv.danmaku.ijk.media.sample.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.sample.widget.media.MeasureHelper;

public class VideoActivity extends Activity  {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private AndroidMediaController mMediaController;
    private IjkVideoView mVideoView;

    private boolean mBackPressed;

    public static Intent newIntent(Context context, String videoPath, String videoTitle) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        return intent;
    }
    public static void intentTo(Context context, String videoPath, String videoTitle) {
        context.startActivity(newIntent(context, videoPath, videoTitle));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        mVideoPath = getIntent().getStringExtra("videoPath");
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        
        
        startPlayVideo();
    }
    private void startPlayVideo()
    {
    	  mMediaController = new AndroidMediaController(this, false);
          IjkMediaPlayer.loadLibrariesOnce(null);
          IjkMediaPlayer.native_profileBegin("libijkplayer.so");

          mVideoView.setMediaController(mMediaController);
          if (mVideoPath != null)
              mVideoView.setVideoPath(mVideoPath);
          else {
              Log.e(TAG, "Null Data Source\n");
              finish();
              return;
          }
          mVideoView.start();
    }
    @Override
    public void onBackPressed() {
        mBackPressed = true;
        super.onBackPressed();
    }
    //看如何实现设置为硬解模式.

    @Override
    protected void onStop() {
        super.onStop();
        if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }
}
