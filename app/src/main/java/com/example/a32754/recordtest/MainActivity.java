package com.example.a32754.recordtest;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {
    //录音、播放
    private ToggleButton RecordingButton, PlayingButton;
    File recordingFile;//储存AudioRecord录下来的文件
    File recordingWave;
    boolean isRecording = false; //true表示正在录音
    boolean isPlaying = false;
    AudioRecord audioRecord=null;
    PCMPlayer pcmPlayer = null;
    File parent=null;//文件目录
    int bufferSize=0;//最小缓冲区大小
    int sampleRateInHz = 44100;//采样率
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    AudioTrack trackplayer = null;
    String TAG="AudioRecord";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        RecordingButton = (ToggleButton)findViewById(R.id.RecordingButton);
        PlayingButton = (ToggleButton) findViewById(R.id.PlayingButton);

        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig, audioFormat);//计算最小缓冲区
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRateInHz,channelConfig, audioFormat, bufferSize);//创建AudioRecorder对象

        parent = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/AudiioRecordTest");
        if(!parent.exists())
            parent.mkdirs();//创建文件夹

        RecordingButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startRecording();
                    PlayingButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "正在录音", Toast.LENGTH_SHORT).show();
                }
                else {
                    stopRecording();
                    PlayingButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "录音结束", Toast.LENGTH_SHORT).show();
                }
            }
        });

        PlayingButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    RecordingButton.setEnabled(false);
                    recordingFile = new File(parent,"audiotest.pcm");
                    int audioLength = (int)recordingFile.length();
                    Toast.makeText(MainActivity.this, "正在播放", Toast.LENGTH_SHORT).show();
                    trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRateInHz,channelConfig,audioFormat,audioLength,AudioTrack.MODE_STREAM);//创建AudioTrack对象
                    trackplayer.setNotificationMarkerPosition(audioLength/2);
                    trackplayer.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                        @Override
                        public void onMarkerReached(AudioTrack track) {
                            PlayingButton.setChecked(false);
                        }
                        @Override
                        public void onPeriodicNotification(AudioTrack track) {
                        }
                    });

                    try{
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));
                        byte[] buffer = new byte[bufferSize];
                        trackplayer.play();
                        int res;
                        while(true) {
                            res = dis.read(buffer);
                            if(res == -1)
                                break;
                            trackplayer.write(buffer,0,res);
                        }
                        dis.close();
                    }
                    catch (Throwable t) {
                        Log.e(TAG, "Playing Failed");
                    }
                }
                else {
                    RecordingButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                    if(trackplayer != null) {
                        trackplayer.stop();
                        trackplayer.release();
                        trackplayer = null;
                    }
                }
            }
        });
    }

    //开始录音
    public void startRecording() {
        isRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRecording = true;

                recordingFile = new File(parent,"audiotest.pcm");
                if(recordingFile.exists()){
                    recordingFile.delete();
                }

                try {
                    recordingFile.createNewFile();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"创建储存音频文件出错");
                }
                try {
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                    byte[] buffer = new byte[bufferSize];
                    audioRecord.startRecording();//开始录音
                    int r = 0;
                    while (isRecording) {
                        int bufferReadResult = audioRecord.read(buffer,0,bufferSize);
                        for (int i = 0; i < bufferReadResult; i++)
                        {
                            dos.write(buffer[i]);
                        }
                        r++;
                    }
                    audioRecord.stop();//停止录音
                    dos.close();
                    recordingWave = new File(parent,"audiotest.wav");
                    Tools.rawToWave(recordingFile,recordingWave);
                } catch (Throwable t) {
                    Log.e(TAG, "Recording Failed");
                }
            }
        }).start();
    }

    //停止录音
    public void stopRecording() {
        isRecording = false;
        isPlaying = false;
    }

    public void startPlaying() {
        isPlaying = true;

    }

    public void stopPlaying() {
        isRecording = false;
        isPlaying = false;
    }
}

