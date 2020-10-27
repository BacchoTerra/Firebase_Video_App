package com.bacchoterra.videoview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class AddVideoActivity extends AppCompatActivity {

    //Layout components
    private VideoView videoView;
    private TextView txtLength;
    private TextInputEditText editTitle;
    private Button btnUpload;

    //Extras
    private Uri videoUri;

    //Firebase
    private StorageReference rootStorage;
    private StorageReference ref;

    //Constants
    private static final int VIDEO_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);
        init();


    }

    private void init() {
        initViews();
        initVideoView();
    }

    private void initViews() {

        videoView = findViewById(R.id.activity_add_video_videoView);
        txtLength = findViewById(R.id.activity_add_video_txtLength);
        editTitle = findViewById(R.id.activity_add_video_editTitle);
        btnUpload = findViewById(R.id.activity_add_video_btnUpload);

    }

    private void initVideoView(){

        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == VIDEO_REQUEST_CODE && resultCode == RESULT_OK && data != null){

            videoUri = data.getData();
            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    txtLength.setText(String.valueOf(mediaPlayer.getDuration()));
                    videoView.start();
                }
            });

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_add_video,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {


        if (item.getItemId() == R.id.menu_add_video_choose_video){
            Intent videoIntent = new Intent();
            videoIntent.setType("video/*");
            videoIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(videoIntent,VIDEO_REQUEST_CODE);

        }

        return true;
    }
}