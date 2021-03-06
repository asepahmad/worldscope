package com.litmus.worldscope;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.litmus.worldscope.model.WorldScopeCreatedStream;

import fragment.CommentFragment;
import fragment.StickerFragment;
import fragment.StreamCreateFragment;
import fragment.StreamVideoControlFragment;
import fragment.StreamVideoFragment;
import fragment.TitleFragment;
import fragment.TutorialFragment;

public class StreamActivity extends AppCompatActivity implements StreamVideoFragment.OnStreamVideoFragmentListener,
        StreamCreateFragment.OnStreamCreateFragmentListener,
        StreamVideoControlFragment.OnStreamVideoControlFragmentListener,
        CommentFragment.OnCommentFragmentInteractionListener,
        TitleFragment.OnTitleFragmentButtonsListener,
        TutorialFragment.TutorialFragmentCompletionListener {

    private static final String TAG = "StreamActivity";
    private static final boolean IS_STREAMER = true;
    private String streamWhenReadyTag = "streamWhenReady";
    private String isRecordingTag = "isRecordingTag";
    private String rtmpLink;
    private StreamVideoFragment.StreamVideoControls control;
    private StreamCreateFragment streamCreateFragment;
    private StreamVideoControlFragment streamVideoControlFragment;
    private CommentFragment commentFragment;
    private TitleFragment titleFragment;
    private TutorialFragment tutorialFragment;
    private StickerFragment stickerFragment;
    private android.support.v4.app.FragmentManager sfm;
    private boolean streamWhenReady = false;
    private boolean isRecording = false;
    private boolean isTutorial = true;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            savedInstanceState.getBoolean(streamWhenReadyTag);
            savedInstanceState.getBoolean(isRecordingTag);
        }

        setContentView(R.layout.activity_stream);
        sfm = getSupportFragmentManager();

        // Create GestureDetector
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Get streamCreateFragment
        streamCreateFragment = (StreamCreateFragment) sfm.findFragmentById(R.id.streamCreateFragment);
        // Get streamVideoControlFragment
        streamVideoControlFragment = (StreamVideoControlFragment) sfm.findFragmentById(R.id.streamVideoControlFragment);
        // Get commentFragment
        commentFragment = (CommentFragment) sfm.findFragmentById(R.id.commentFragment);
        // Get titleFragment
        titleFragment = (TitleFragment) sfm.findFragmentById(R.id.titleFragment);
        // Get tutorialFragment
        tutorialFragment = (TutorialFragment) sfm.findFragmentById(R.id.tutorialFragment);
        // Get starFragment
        stickerFragment = (StickerFragment) sfm.findFragmentById(R.id.stickerFragment);

        Log.d(TAG, "Streamer activity created!");
    }

    // Get streamId and recording
    @Override
    public void onRestart() {
        super.onRestart();

        Log.d(TAG, "onRestart");
        isRecording = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("isRecording", false);
        rtmpLink = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("rtmpLink", "");
        // If control is ready, start streaming, else stream when ready
        if(control != null && isRecording == true) {
            // Find streamVideoFragment and set the rtmp link from streamCreateFragment
            StreamVideoFragment streamVideoFragment = (StreamVideoFragment) sfm.findFragmentById(R.id.streamVideoFragment);
            streamVideoFragment.setRTMPLink(rtmpLink);
            control.startStreaming();
            Log.d(TAG, "Stream resumed");
        } else {
            streamWhenReady = true;
        }
    }

    // Save streamId and recording
    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("isRecording", isRecording)
                .putString("rtmpLink", rtmpLink)
                .apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(rtmpLink != null && !rtmpLink.isEmpty()) {
            Log.d(TAG, "Ending stream");
            streamCreateFragment.endStream();
        }

        // Remove recording state
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("isRecording", false)
                .putString("rtmpLink", "")
                .apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    // Override to intercept all touch events, required as listView was consuming touch events
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }


    /**
     * Implementing StreamVideoFragment
     */
    @Override
    public void streamVideoFragmentReady(StreamVideoFragment.StreamVideoControls control) {
        this.control = control;
        Log.d(TAG, "Streamer ready!");
        if(streamWhenReady) {
            control.startStreaming();
        }
    }

    /**
     * Implementing StreamCreateFragment
     */

    @Override
    public void onStreamCreationSuccess(WorldScopeCreatedStream stream) {
        this.rtmpLink = stream.getStreamLink();

        // Find streamVideoFragment and set the rtmp link from streamCreateFragment
        StreamVideoFragment streamVideoFragment = (StreamVideoFragment) sfm.findFragmentById(R.id.streamVideoFragment);
        streamVideoFragment.setRTMPLink(rtmpLink);

        // If control is ready, start streaming, else stream when ready
        if(control != null) {
            isRecording = true;
            control.startStreaming();
        } else {
            streamWhenReady = true;
        }

        streamVideoControlFragment.tutorialForceShow();

        //Listen for completion of tutorial
        tutorialFragment.setListener(this);

        // Open tutorial
        tutorialFragment.initialize();

        // Start the streamVideoControls

        // Join room and show comment UI
        commentFragment.setupRoom(stream.getAppInstance(), stream.getStreamId(), stream.getStreamer().getAlias());

        // Show titlebar
        titleFragment.loadStreamDetails(IS_STREAMER, stream.getStreamer().getIsSubscribed(), stream.getStreamer().getPlatformId(),
                stream.getStreamer().getAlias(), stream.getTitle());

    }

    @Override
    public void onCancelStreamButtonClicked() {
        control.destroyStreamer();

        redirectToMainActivity();
    }


    @Override
    public void onStreamTerminationResolved(boolean isTerminated) {
        if(isTerminated) {
            redirectToMainActivity();
        } else {
            // Release the controls back
            streamVideoControlFragment.unBlockControls();
        }
    }

    /**
     * Implementing StreamVideoControlFragment
     */

    @Override
    public void onStreamRecordButtonShortPress() {
        // Signal to StreamVideoFragment to pause the stream
        if(isRecording) {
            control.stopStreaming();
        } else {
            control.startStreaming();
        }

        // Toggle the isRecording boolean
        isRecording = !isRecording;
    }

    @Override
    public void onStreamRecordButtonLongPress() {
        // Signal to StreamCreateFragment to do a confirmation and stop stream
        streamCreateFragment.confirmStreamTermination();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && isRecording) {
            streamCreateFragment.confirmStreamTermination();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // For CommentFragment
    // TODO: Update
    @Override
    public void onFragmentInteraction() {

    }

    // Gesture Listener to listen for double taps
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // On Double Tap, calls the video control fragment back into view if hidden
            Log.d(TAG, "Double Tap detected, showing controls");
            if(!isTutorial) {
                streamVideoControlFragment.toggleControlVisibility();
            }
            return true;
        }
    }

    @Override
    public void onToggleButtonClicked() {
        //control.toggleCamera();
    }

    @Override
    public void onStickerButtonClicked() {
        stickerFragment.sendStickers();
    }

    @Override
    public void onCompletedTutorial() {
        Log.d(TAG, "Completed tutorial");
        isTutorial = false;
        streamVideoControlFragment.tutorialCompleted();
        commentFragment.initialize();
        titleFragment.showTitleUI();
    }
}
