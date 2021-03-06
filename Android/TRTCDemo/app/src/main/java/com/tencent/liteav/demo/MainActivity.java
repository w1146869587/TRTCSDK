package com.tencent.liteav.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tencent.liteav.demo.trtc.TRTCNewRoomActivity;
import com.tencent.liteav.demo.trtcvoiceroom.CreateVoiceRoomActivity;
import com.tencent.liteav.trtcaudiocalldemo.demo.CreateAudioCallActivity;
import com.tencent.rtmp.TXLiveBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    private TextView                mMainTitle;
    private TextView                mTvVersion;
    private List<TRTCItemEntity>    mTRTCItemEntityList;
    private RecyclerView            mRvList;
    private TRTCRecyclerViewAdapter mTRTCRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            Log.d(TAG, "brought to front");
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        mTvVersion = (TextView) findViewById(R.id.main_tv_version);
        mTvVersion.setText("腾讯云 TRTC v" + TXLiveBase.getSDKVersionStr());

        mMainTitle = (TextView) findViewById(R.id.main_title);
        mMainTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        File logFile = getLogFile();
                        if (logFile != null) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/octet-stream");
                            //intent.setPackage("com.tencent.mobileqq");
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
                            startActivity(Intent.createChooser(intent, "分享日志"));
                        }
                    }
                });
                return false;
            }
        });

        mRvList = (RecyclerView) findViewById(R.id.main_recycler_view);
        mTRTCItemEntityList = createTRTCItems();

        mTRTCRecyclerViewAdapter = new TRTCRecyclerViewAdapter(this, mTRTCItemEntityList, new TRTCRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                TRTCItemEntity entity = mTRTCItemEntityList.get(position);
                Intent intent = new Intent(MainActivity.this, entity.mTargetClass);
                intent.putExtra("TITLE", entity.mTitle);
                intent.putExtra("TYPE", entity.mType);

                MainActivity.this.startActivity(intent);
            }
        });
        mRvList.setLayoutManager(new LinearLayoutManager(this));
        mRvList.setAdapter(mTRTCRecyclerViewAdapter);
    }

    private List<TRTCItemEntity> createTRTCItems() {
        List<TRTCItemEntity> list = new ArrayList<>();
        list.add(new TRTCItemEntity("视频通话", "支持720P/1080P高清画质，50%丢包率可正常视频通话，自带美颜、挂件、抠图等AI特效。", R.drawable.video_call, TRTCNewRoomActivity.TRTC_VOICECALL, TRTCNewRoomActivity.class));
        list.add(new TRTCItemEntity("视频互动直播", "低延时、十万人高并发的大型互动直播解决方案，观众时延低至800ms，上下麦切换免等待。", R.drawable.live_stream, TRTCNewRoomActivity.TRTC_LIVE, TRTCNewRoomActivity.class));
        list.add(new TRTCItemEntity("语音通话", "48kHz高音质，60%丢包可正常语音通话，领先行业的3A处理，杜绝回声和啸叫。", R.drawable.voice_call, 0, CreateAudioCallActivity.class));
        list.add(new TRTCItemEntity("语音聊天室", "内含变声、音效、混响、背景音乐等声音玩法，适用于闲聊房、K歌房、开黑房等语聊场景。", R.drawable.voice_chatroom, 0, CreateVoiceRoomActivity.class));
        return list;
    }

    private File getLogFile() {
        String       path      = getExternalFilesDir(null).getAbsolutePath() + "/log/tencent/liteav";
        List<String> logs      = new ArrayList<>();
        File         directory = new File(path);
        if (directory != null && directory.exists() && directory.isDirectory()) {
            long lastModify = 0;
            File files[]    = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.getName().endsWith("xlog")) {
                        logs.add(file.getAbsolutePath());
                    }
                }
            }
        }


        String zipPath = path + "/liteavLog.zip";
        return zip(logs, zipPath);
    }

    private File zip(List<String> files, String zipFileName) {
        File zipFile = new File(zipFileName);
        zipFile.deleteOnExit();
        InputStream     is  = null;
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zos.setComment("LiteAV log");
            for (String path : files) {
                File file = new File(path);
                try {
                    if (file.length() == 0 || file.length() > 8 * 1024 * 1024) continue;

                    is = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buffer = new byte[8 * 1024];
                    int    length = 0;
                    while ((length = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "zip log error");
            zipFile = null;
        } finally {
            try {
                zos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return zipFile;
    }
}
