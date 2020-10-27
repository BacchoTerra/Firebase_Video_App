package com.bacchoterra.videoview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import pub.devrel.easypermissions.EasyPermissions;

public class AddVideoActivity extends AppCompatActivity {

    //Layout components
    private VideoView videoView;
    private TextView txtLength;
    private TextInputEditText editTitle;
    private Button btnUpload;
    private ProgressBar progressBar;

    //Extras
    private Uri videoUri;

    //Firebase
    private StorageReference rootStorage;
    private StorageReference ref;

    //Constants
    private static final int EXTERNAL_STORAGE_PERM_REQ = 45;
    private static final int CAMERA_PERM_REQ = 52;


    private static final int STORAGE_REQUEST_CODE = 100;
    private static final int CAPTURE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);
        init();

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoUri != null) {

                    uploadVideo();


                } else {
                    Toast.makeText(AddVideoActivity.this, "No video Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });


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
        progressBar = findViewById(R.id.activity_add_video_progressBar);

    }

    private void initVideoView() {

        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == STORAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {

            videoUri = data.getData();
            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    txtLength.setText(String.valueOf(mediaPlayer.getDuration()));
                    videoView.start();
                }
            });

        } else if (requestCode == CAPTURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
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

        getMenuInflater().inflate(R.menu.menu_add_video, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {


        if (item.getItemId() == R.id.menu_add_video_choose_video) {

            String[] itens = {"Camera", "Storage"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("New Video From:");
            builder.setItems(itens, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            cameraPerm();
                            break;
                        case 1:
                            storagePerm();
                            break;
                    }
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void storagePerm() {

        String[] perm = {Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!EasyPermissions.hasPermissions(this, perm)) {

            EasyPermissions.requestPermissions(this, getString(R.string.rationale_ask), EXTERNAL_STORAGE_PERM_REQ, perm);
        } else {
            Intent storageIntent = new Intent();
            storageIntent.setType("video/*");
            storageIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(storageIntent, STORAGE_REQUEST_CODE);
        }

    }

    private void cameraPerm() {

        String[] perm = {Manifest.permission.CAMERA};

        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_ask), CAMERA_PERM_REQ, perm);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            startActivityForResult(cameraIntent, CAPTURE_REQUEST_CODE);

        }


    }

    private void uploadVideo() {
        btnUpload.setVisibility(View.GONE);

        rootStorage = FirebaseStorage.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        String videoFinalPath = "video_" + timestamp;

        ref = rootStorage.child("videos").child(videoFinalPath);


        ref.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                Uri uri = uriTask.getResult();

                if (uriTask.isSuccessful()){

                    saveInDatabase(videoFinalPath,uri.toString(),timestamp);

                }

                btnUpload.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(AddVideoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax((int) snapshot.getTotalByteCount());
                progressBar.setProgress((int) snapshot.getBytesTransferred());
            }
        });

    }

    private void saveInDatabase(String videoName,String url,String timestamp){

        VideoModel videoModel = new VideoModel();

        videoModel.setVideoName(videoName);
        if (editTitle.getText().toString().isEmpty()){
            videoModel.setTitle("Untitled");
        }else {
            videoModel.setTitle(editTitle.getText().toString());
        }
        videoModel.setUrl(url);
        videoModel.setTimestamp(timestamp);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference ref = rootRef.child("videos").child(videoName);
        ref.setValue(videoModel);
    }
}