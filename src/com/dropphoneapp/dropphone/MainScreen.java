package com.dropphoneapp.dropphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

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
    private static final String BASE_URL = "https://dropphoneapp.appspot.com";
//    private static final String BASE_URL = "http://192.168.1.2:8080";
    private static final String SHARED_SECRET = "Phasuca6a4eCRU84b7eVUnA55asWaspazEDuw4maDRUS8uhE5AsuwE7aPUTe6ru9";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        new AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage("Gravity is a cruel mistress.  You are solely responsible for anything bad that happens while using this app.")
                .setNegativeButton("I\'m afraid.",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                        System.exit(0);
                    }
                })
                .setPositiveButton("Okay, sounds good", null)
                .show();



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

    public void wimpOut(View whatever) {
        super.onDestroy();
    }

    public void submitScore(View whatever) {
        //submit the score
        EditText username_input = ((EditText) findViewById(R.id.edtInput));
        String username = username_input.getText().toString();
        ProgressDialog progress = ProgressDialog.show(CONTEXT,"Please wait...", "Submitting your score");
        int rank = submitscore(username, time_fallen_seconds);
        progress.dismiss();

        new AlertDialog.Builder(this)
                .setTitle("Your rank")
                .setMessage("Your time of " + String.valueOf(new DecimalFormat("#####.##").format(time_fallen_seconds))+ " ms was ranked #" + String.valueOf(rank) + " in the world.")
                .setNeutralButton("Ok", null)
                .show();


        setContentView(R.layout.main);
        AccelerometerManager.startListening(this);
    }

    private int submitscore(String username, double score)  {

        String string_score = "";
        try{
            username = URLEncoder.encode(username, "utf-8");
            string_score = URLEncoder.encode(String.valueOf(score), "utf-8");
        } catch (Exception e){
            //
        }
        MessageDigest algorithm;
        try {
          algorithm = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
          return -1;
        }
        algorithm.reset();
//        algorithm.update(username.getBytes());
        algorithm.update(string_score.getBytes());
        algorithm.update(SHARED_SECRET.getBytes());
        byte messageDigest[] = algorithm.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<messageDigest.length;i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                // could use a for loop, but we're only dealing with a single byte
                hexString.append('0');
            }
            hexString.append(hex);
        }


        String address = BASE_URL+"/app/newscore?username="+username+"&score="+string_score+"&hash="+hexString;
        int rank = 1000;
        try{
            JSONObject json = RestJsonClient.connect_post(address);
            rank = json.getInt("rank");
        } catch (JSONException e) {
            // handle it or something
        }

        return rank;
    }

    public void onAccelerationChanged(float x, float y, float z, long timestamp) {
        //ignore timestamps older thant .5 seconds
        if(System.nanoTime() - timestamp > 500000000) {
            return;
        }
        double sum = x*x + y*y + z*z;
        double magnitude = Math.sqrt(sum);
        boolean falling = magnitude < FALLING_THRESHOLD;

        if(current_state == STATE_FALLING){
            if(!falling){
                //if we stop falling... calculate the time we fell
                time_fallen_seconds = (timestamp - start_time_nanoseconds)/(1000000f);
                if(time_fallen_seconds > FALL_DURATION_THRESHOLD ){
                    ((TextView) findViewById(R.id.main_text)).setText("New high score! " + String.valueOf(time_fallen_seconds) + " milliseconds");
                    AccelerometerManager.stopListening();
                    setContentView(R.layout.input);

                    try{
                        ((TextView) findViewById(R.id.yourscore)).setText("Your score: " + String.valueOf(new DecimalFormat("#####.##").format(time_fallen_seconds)) + "ms");
                    }catch (Exception e){
                        //
                    }

                    current_state = STATE_NOT_FALLING;
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
