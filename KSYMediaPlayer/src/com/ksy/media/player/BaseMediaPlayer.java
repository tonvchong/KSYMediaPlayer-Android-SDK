package com.ksy.media.player;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.ksy.media.player.exception.Ks3ClientException;
import com.ksy.media.player.log.LogClient;
import com.ksy.media.player.log.LogRecord;
import com.ksy.media.player.util.Constants;

/**
 * 
 *   Common IMediaPlayer implement
 */
public abstract class BaseMediaPlayer implements IMediaPlayer {
    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnDRMRequiredListener mOnDRMRequiredListener;
    
    public LogRecord logRecord = LogRecord.getInstance();
    private Timer seekTimer;
    
    
    public final void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public final void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public final void setOnBufferingUpdateListener(
            OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public final void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public final void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public final void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public final void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }
    
    public final void setOnDRMRequiredListener(OnDRMRequiredListener listener){
    	mOnDRMRequiredListener = listener;
    }

    
    public void resetListeners() {
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnDRMRequiredListener = null;
    }

    public void attachListeners(IMediaPlayer mp) {
        mp.setOnPreparedListener(mOnPreparedListener);
        mp.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mp.setOnCompletionListener(mOnCompletionListener);
        mp.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mp.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mp.setOnErrorListener(mOnErrorListener);
        mp.setOnInfoListener(mOnInfoListener);
        mp.setOnDRMRequiredListener(mOnDRMRequiredListener);
    }

    protected final void notifyOnPrepared() {
        if (mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
    }

    protected final void notifyOnCompletion() {
        if (mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }

    //TODO
    protected final void notifyOnSeekComplete() {
        if (mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
        
        //不需要这个了，合并seekstatus和seekMessage
//        logRecord.setSeekEnd(System.currentTimeMillis());
        logRecord.setSeekStatus("ok");
        logRecord.setSeekMessage("SeekComplete"); //TODO 需要底层对接
        
        Log.d(Constants.LOG_TAG, "seekend =" + logRecord.getSeekEndJson());
        
        seekTimer = new Timer();
        seekTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					LogClient.getInstance().put(logRecord.getSeekEndJson());
					
				} catch (Ks3ClientException e) {
					e.printStackTrace();
					Log.e(Constants.LOG_TAG, "saveUsageData e = " + e);
				}
			}
		}, 3000, 4000);
    
        try {
			LogClient.getInstance().put(logRecord.getSeekEndJson());
		} catch (Ks3ClientException e) {
			e.printStackTrace();
			Log.e(Constants.LOG_TAG, "BaseMediaPlayer e =" + e);
		}
        
    }

    protected final void notifyOnVideoSizeChanged(int width, int height,
            int sarNum, int sarDen) {
        if (mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height,
                    sarNum, sarDen);
    }

    //TODO
    protected final boolean notifyOnError(int what, int extra) {
        if (mOnErrorListener != null)
            return mOnErrorListener.onError(this, what, extra);
        logRecord.setSeekStatus("fail");
        
        String playFail = String.valueOf(what) + "_" + String.valueOf(extra);
        //都返回错误码就行
        logRecord.setPlayStatus(playFail);
        
        logRecord.setSeekMessage(playFail);
        
        return false;
    }

    protected final boolean notifyOnInfo(int what, int extra) {
        if (mOnInfoListener != null)
            return mOnInfoListener.onInfo(this, what, extra);
        return false;
    }
    
    protected final void notifyOnDRMRequired(int what, int extra,String version) {
        if (mOnDRMRequiredListener != null)
           mOnDRMRequiredListener.OnDRMRequired(this, what, extra,version);
    }
}
