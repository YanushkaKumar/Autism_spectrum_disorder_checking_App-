package com.example.bottomnavactivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.IOException;
import org.json.JSONObject;
import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;


public class MainActivity extends AppCompatActivity {

    //Variable Declarations:
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 101;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private ImageView imageView;
    private ImageView imageView3;
    private TextView tv_recording_path;
    private TextView tv_time;
    private ImageView imageView4;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private int seconds = 0;
    private String path = null;

    private Handler handler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            seconds++;
            updateTimerText();
            handler.postDelayed(this, 1000); // Update every 1 second
        }
    };

    private EditText ageEditText;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Initializing UI Elements

        imageView = findViewById(R.id.imageView);
        imageView3 = findViewById(R.id.imageView3);
        tv_recording_path = findViewById(R.id.tv_recording_path);
        tv_time = findViewById(R.id.tv_time);
        imageView4 = findViewById(R.id.imageView4);
        ageEditText = findViewById(R.id.age_text);


        // Check and request audio recording permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                    imageView.setVisibility(View.INVISIBLE); // Hide imageView when recording starts
                } else {
                    stopRecording();
                    imageView.setVisibility(View.VISIBLE); // Show imageView when recording stops
                    clearTextView(); // Clear the textView
                }
            }
        });




        imageView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startPlaying();
                } else {
                    stopPlaying();
                }
            }
        });

        imageView4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopRecording();
            }
        });

        

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                    imageView.setVisibility(View.INVISIBLE); // Hide imageView when recording starts
                } else {
                    stopRecording();
                    imageView.setVisibility(View.VISIBLE); // Show imageView when recording stops
                }
            }
        });

        imageView4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                imageView.setVisibility(View.VISIBLE); // Show imageView when recording stops
            }
        });
    }
    private void handleIOError(String errorMessage) {
        // Handle IO error and display the error message
        runOnUiThread(() -> {
            TextView textView = findViewById(R.id.textView);
            textView.setText("Error: " + errorMessage);
        });
    }

    private void uploadFileToFlaskAPI() {
        if (path == null) {
            // Path is not available, cannot upload
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS.SECONDS) // Example: Increase timeout to 30 seconds
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("audio/mp3"); // Change this to the appropriate media type

        File file = new File(path);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", file.getName(), RequestBody.create(mediaType, file))
                .addFormDataPart("age", ageEditText.getText().toString()) // Add age as a form field
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.185.94:5000/process_audio") // Replace with your Flask API URL
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the failure and pass an error message
                handleIOError("Failed to make the request: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // The file has been successfully uploaded
                    runOnUiThread(() -> {
                        TextView textView = findViewById(R.id.textView);
                        textView.setText("File uploaded successfully");
                    });

                    try {
                        // Read and parse the JSON response
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String result = json.getString("result");

                        // Update your UI with the result
                        runOnUiThread(() -> {
                            TextView textView = findViewById(R.id.textView);
                            textView.setText(result);
                        });
                    } catch (JSONException e) {
                        // Handle JSON parsing error
                        handleIOError("Error parsing JSON response: " + e.getMessage());
                    }
                } else {
                    // Handle the response if it's not successful
                    handleIOError("HTTP response code: " + response.code());
                }
            }
        });
    }


    private void clearTextView() {
        TextView textView = findViewById(R.id.textView);
        textView.setText("");
    }
    private void handleRecordingError() {
        // Handle recording error, e.g., display an error message
        runOnUiThread(() -> {
            TextView textView = findViewById(R.id.textView);
            textView.setText("Error during recording");
        });
    }
    private void handlePlaybackError() {
        // Handle playback error, e.g., display an error message
        runOnUiThread(() -> {
            TextView textView = findViewById(R.id.textView);
            textView.setText("Error during playback");
        });
    }
    private void handleIOError(TextView textView) {
        // Handle IO error, e.g., display an error message
        runOnUiThread(() -> {
            textView.setText("Error reading response");
        });
    }


    private int recordingCount = 0; // Counter for naming the recordings

    private void startRecording() {
        if (isRecording) return;

        String timestamp = Long.toString(System.currentTimeMillis()); // Generate a timestamp
        path = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/recording_" + timestamp + ".mp3"; // Change to .wav format

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT); // Use WAV format
        mediaRecorder.setOutputFile(path);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); // Use default audio encoder

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            handleRecordingError();
        }

        mediaRecorder.start();
        isRecording = true;
        startTimer(); // Start the countdown timer
        tv_recording_path.setText("Recording: " + path);
        recordingCount++; // Increment the recording count

        // Hide the textView

        imageView.setVisibility(View.INVISIBLE);
        tv_recording_path.setVisibility(View.INVISIBLE);
        clearTextView();

    }

    private void stopRecording() {
        if (!isRecording) return;

        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        isRecording = false;
        stopTimer(); // Stop the countdown timer
        tv_recording_path.setText("Recording stopped: " + path);

        // Show the textView

        imageView.setVisibility(View.VISIBLE);


        uploadFileToFlaskAPI();
        clearEditText();
    }
    private void clearEditText() {
        ageEditText.setText(""); // Clear the EditText
    }
    private void startPlaying() {
        if (isPlaying || path == null) return;

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            tv_time.setText("Playing");
        } catch (IOException e) {
            handlePlaybackError();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaying();
            }
        });
    }

    private void stopPlaying() {
        if (!isPlaying) return;

        mediaPlayer.release();
        mediaPlayer = null;
        isPlaying = false;
        tv_time.setText("00:00");
    }

    private void startTimer() {
        seconds = 0;
        handler.postDelayed(timerRunnable, 1000); // Start the countdown timer
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable); // Stop the countdown timer
    }

    private void updateTimerText() {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        String timeString = String.format("%02d:%02d", minutes, remainingSeconds);
        tv_time.setText(timeString);
    }




}
