package cn.sky.musicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;


public class MusicActivity extends Activity {

    private ImageView MusicPlay;
    private ImageView MusicNext;
    private ImageView MusicPrevious;

    Boolean mBound = false;

    //记录鼠标点击了几次
    boolean flag =false;

    MusicService mService;

    SeekBar seekBar;

    //多线程，后台更新UI
    Thread myThread;

    //控制后台线程退出
    boolean playStatus = true;

    //处理进度条更新
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //从bundle中获取进度，是double类型，播放的百分比
                    double progress = msg.getData().getDouble("progress");

                    //根据播放百分比，计算seekbar的实际位置
                    int max = seekBar.getMax();
                    int position = (int) (max * progress);
                    //设置seekbar的实际位置
                    seekBar.setProgress(position);
                    break;
                default:
                    break;
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musicplay);

        MusicPlay = (ImageView) findViewById(R.id.Musicplay);
        MusicNext = (ImageView) findViewById(R.id.musicnext);
        MusicPrevious = (ImageView) findViewById(R.id.musicprevious);
        //定义一个新线程，用来发送消息，通知更新UI
        myThread = new Thread(new UpdateProgress());
        //绑定service;
        Intent serviceIntent = new Intent(MusicActivity.this, MusicService.class);

        //如果未绑定，则进行绑定,第三个参数是一个标志，它表明绑定中的操作．它一般应是BIND_AUTO_CREATE，这样就会在service不存在时创建一个
        if (!mBound) {
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }

        seekBar = (SeekBar) findViewById(R.id.MusicProgress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //手动调节进度
                // TODO Auto-generated method stub
                //seekbar的拖动位置
                int dest = seekBar.getProgress();
                //seekbar的最大值
                int max = seekBar.getMax();
                //调用service调节播放进度
                mService.setProgress(max, dest);
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

        });


        MusicPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mBound&&flag) {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause));
                    mService.pause();
                    flag =false;
                }else{
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay));
                    mService.play();
                    flag =true;
                }
            }
        });
    }

    //实现runnable接口，多线程实时更新进度条
    public class UpdateProgress implements Runnable {
        //通知UI更新的消息
        //用来向UI线程传递进度的值
        Bundle data = new Bundle();
        //更新UI间隔时间
        int milliseconds = 100;
        double progress;
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //用来标识是否还在播放状态，用来控制线程退出
            while (playStatus) {
                try {
                    //绑定成功才能开始更新UI
                    if (mBound) {
                        //发送消息，要求更新UI
                        Message msg = new Message();
                        data.clear();
                        progress = mService.getProgress();
                        msg.what = 0;
                        data.putDouble("progress", progress);
                        msg.setData(data);
                        mHandler.sendMessage(msg);
                    }
                    Thread.sleep(milliseconds);
                    //Thread.currentThread().sleep(milliseconds);
                    //每隔100ms更新一次UI
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.MyBinder myBinder = (MusicService.MyBinder) binder;
            //获取service
            mService = (MusicService) myBinder.getService();
            //绑定成功
            mBound = true;
            //开启线程，更新UI
            myThread.start();
            MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay));
            mService.play();
            flag =true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        //      getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    public void onDestroy() {
        //销毁activity时，要记得销毁线程
        playStatus = false;
        super.onDestroy();
    }
}