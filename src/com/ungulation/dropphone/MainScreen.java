package com.ungulation.dropphone;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainScreen extends Activity implements AccelerometerListener {
    private static Context CONTEXT;
    private double min_magnitude = 9999;
    private long start_time_nanoseconds = 0;
    private double time_fallen_seconds = 0;
    private double max_time_fallen_seconds = 0;


    private static final int STATE_FALLING = 0;
    private static final int STATE_NOT_FALLING = 1;
    private static final float FALLING_THRESHOLD = 2.0f;
    private static final double FALL_DURATION_THRESHOLD = 100; //milliseconds
    private int current_state = STATE_NOT_FALLING;
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        CONTEXT = this;
    }

    protected void onResume() {
        super.onResume();
        if (AccelerometerManager.isSupported()) {
            AccelerometerManager.startListening(this);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (AccelerometerManager.isListening()) {
            AccelerometerManager.stopListening();
        }

    }

    public void submitScore(View whatever) {
        setContentView(R.layout.main);
        AccelerometerManager.startListening(this);
    }


    public void onAccelerationChanged(float x, float y, float z, long timestamp) {
        //ignore timestamps older thant .5 seconds
        if(System.nanoTime() - timestamp > 500000000) {
            return;
        }
        double sum = x*x + y*y + z*z;
        double magnitude = Math.sqrt(sum);
        boolean falling = magnitude < FALLING_THRESHOLD;
        try{
            ((TextView) findViewById(R.id.y)).setText(String.valueOf(y));
            ((TextView) findViewById(R.id.z)).setText(String.valueOf(z));
            ((TextView) findViewById(R.id.min_mag)).setText(String.valueOf(magnitude));
            ((TextView) findViewById(R.id.timefallen)).setText(String.valueOf(max_time_fallen_seconds));
        } catch (Exception e) {
            return;
        }

        if(current_state == STATE_FALLING){
            if(!falling){
                //if we stop falling... calculate the time we fell
                time_fallen_seconds = (timestamp - start_time_nanoseconds)/(1000000f);
                if(time_fallen_seconds > FALL_DURATION_THRESHOLD ){
                    if(time_fallen_seconds > max_time_fallen_seconds){
                        max_time_fallen_seconds = time_fallen_seconds;
                        ((TextView) findViewById(R.id.main_text)).setText("New high score! " + String.valueOf(time_fallen_seconds) + " milliseconds");
                        //ask the server if this is a high score...

                        //if it is...
                        AccelerometerManager.stopListening();
                        setContentView(R.layout.input);
                        try{
                            ((TextView) findViewById(R.id.yourscore)).setText("Your score: " + String.valueOf(time_fallen_seconds));
                        }catch (Exception e){
                            //
                        }
                        start_time_nanoseconds = System.nanoTime();
                        current_state = STATE_NOT_FALLING;
                        return;
                    } else {
                        ((TextView) findViewById(R.id.main_text)).setText("You fell for " + String.valueOf(time_fallen_seconds)  + " milliseconds");
                    }

                    return;
                }
                current_state = STATE_NOT_FALLING;
            }
        } else { // we are not falling
            if(falling){
                // and we start falling... record thes tart time
                start_time_nanoseconds = timestamp;
                current_state = STATE_FALLING;
            }
        }
        if(magnitude < min_magnitude) {
            min_magnitude = magnitude;
        }
    }

    public static Context getContext() {
        return CONTEXT;
    }

}
