package com.example.youchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.youchat.Adapter.GroupMessageAdapter;
import com.example.youchat.Adapter.MessageAdapter;
import com.example.youchat.Model.MessageModel;
import com.example.youchat.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding chatXml;
    String receiverId, receiverName, receiverEmail;
    DatabaseReference databaseReferenceSender, databaseReferenceReceiver;
    String senderRoom, receiverRoom;
    MessageAdapter messageAdapter;
    Toolbar toolbar;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_PICK_AUDIO = 2;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 100;
    private boolean isReadExternalStoragePermissionGranted = false;
    boolean isAudio = false;
    boolean isImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatXml = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(chatXml.getRoot());

        receiverId = getIntent().getStringExtra("id");
        receiverName = getIntent().getStringExtra("name");
        receiverEmail = getIntent().getStringExtra("email");


        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        senderRoom = FirebaseAuth.getInstance().getUid() + receiverId;
        receiverRoom = receiverId + FirebaseAuth.getInstance().getUid();

        messageAdapter = new MessageAdapter(this, senderRoom, receiverRoom);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        chatXml.chatRecycler.setLayoutManager(manager);
        chatXml.chatRecycler.setAdapter(messageAdapter);
        messageAdapter.setRecyclerView(chatXml.chatRecycler);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.parseColor("#F9CD4A"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        chatXml.receiverNameTextView.setText(receiverName);
        chatXml.receiverEmailTextView.setText(receiverEmail);


        databaseReferenceSender = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom);
        databaseReferenceReceiver = FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom);

        databaseReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messageList = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    messageList.add(messageModel);
                }

                // Sort the message list based on the timestamp
                Collections.sort(messageList, new Comparator<MessageModel>() {
                    @Override
                    public int compare(MessageModel message1, MessageModel message2) {
                        return Long.compare(message1.getTime(), message2.getTime());
                    }
                });

                messageAdapter.clear();
                messageAdapter.addAll(messageList);
                chatXml.chatRecycler.scrollToPosition(messageAdapter.getItemCount() - 1);

                if (messageList.isEmpty()) {
                    chatXml.lottieAnimationView.setVisibility(View.VISIBLE);
                    chatXml.chatRecycler.setVisibility(View.GONE);
                } else {
                    chatXml.lottieAnimationView.setVisibility(View.GONE);
                    chatXml.chatRecycler.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        chatXml.sendMessageImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = chatXml.etMessage.getText().toString();
                if (message.trim().length() > 0) {
                    sendMessage(message, receiverId, receiverName, isImage, isAudio);
                }
            }
        });

        chatXml.AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatXml.ItemsCardView.setVisibility(View.VISIBLE);
                chatXml.AddButton.setVisibility(View.GONE);
                chatXml.closeAddButton.setVisibility(View.VISIBLE);
                chatXml.constraintGallery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openGallery();
                        chatXml.AddButton.setVisibility(View.VISIBLE);
                        chatXml.ItemsCardView.setVisibility(View.GONE);
                        chatXml.closeAddButton.setVisibility(View.GONE);


                    }

                });
                chatXml.constraintAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openAudioPicker();
                        chatXml.AddButton.setVisibility(View.VISIBLE);
                        chatXml.ItemsCardView.setVisibility(View.GONE);
                        chatXml.closeAddButton.setVisibility(View.GONE);

                    }
                });
                chatXml.closeAddButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        chatXml.AddButton.setVisibility(View.VISIBLE);
                        chatXml.ItemsCardView.setVisibility(View.GONE);
                        chatXml.closeAddButton.setVisibility(View.GONE);
                    }
                });
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        // Release the ExoPlayer resources when they are no longer needed
        if (messageAdapter != null) {
            messageAdapter.releasePlayer();
        }
    }


    private void checkReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            isReadExternalStoragePermissionGranted = true;
            openGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isReadExternalStoragePermissionGranted = true;
                openGallery();
            } else {

                Toast.makeText(this, "Permission denied. Unable to access external storage.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);

    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQUEST_PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                uploadImageToStorage(imageUri);
            }
        }
        if (requestCode == REQUEST_PICK_AUDIO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri audioUri = data.getData();
            uploadAudioToStorage(audioUri);
        }
    }

    private void uploadImageToStorage(Uri imageUri) {
        String fileName = UUID.randomUUID().toString();
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("images/" + fileName);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Please wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.show();


        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setProgress((int) progress);
        });

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                saveImageToDataBase(imageUrl, receiverId, receiverName);
                progressDialog.dismiss();

                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();

            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });

    }

    private void uploadAudioToStorage(Uri audioUri) {
        String fileName = UUID.randomUUID().toString();
        StorageReference audioRef = FirebaseStorage.getInstance().getReference().child("audio/" + fileName);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Please wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.show();

        UploadTask uploadTask = audioRef.putFile(audioUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setProgress((int) progress);
        });

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String audioUrl = uri.toString();
                saveAudioToDatabase(audioUrl, receiverId, receiverName);
                progressDialog.dismiss();

                Toast.makeText(this, "Audio uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to upload audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void saveImageToDataBase(String imageUrl, String receiverId, String receiverName) {
        String messageId = UUID.randomUUID().toString();
        long timeStamp = System.currentTimeMillis();

        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), receiverId, receiverName, imageUrl, timeStamp, true);
        messageAdapter.add(messageModel);

        databaseReferenceSender
                .child(messageId)
                .setValue(messageModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
//                        sendMessage(imageUrl, receiverId, receiverName, true,false);
                        Toast.makeText(ChatActivity.this, "Image Url is saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Image url is not Saved", Toast.LENGTH_SHORT).show();
                    }
                });
        databaseReferenceReceiver
                .child(messageId)
                .setValue(messageModel);
    }

    private void saveAudioToDatabase(String audioUrl, String receiverId, String receiverName) {
        String messageId = UUID.randomUUID().toString();
        long timeStamp = System.currentTimeMillis();

        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), receiverId, receiverName, audioUrl, timeStamp, false,true);
        messageAdapter.add(messageModel);

        databaseReferenceSender
                .child(messageId)
                .setValue(messageModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
//,                        sendMessage(audioUrl, receiverId, receiverName, false, true);
                        Toast.makeText(ChatActivity.this, "Audio URL is saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Failed to save audio URL", Toast.LENGTH_SHORT).show();
                    }
                });

        databaseReferenceReceiver
                .child(messageId)
                .setValue(messageModel);
    }

    private void sendMessage(String message, String receiverId, String receiverName, boolean isImage, boolean isAudio) {

        String messageId = UUID.randomUUID().toString();
        long timeStamp = System.currentTimeMillis();


        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), receiverId, receiverName, message, timeStamp, isImage, isAudio);
        messageAdapter.add(messageModel);
        databaseReferenceSender
                .child(messageId)
                .setValue(messageModel);
        databaseReferenceReceiver
                .child(messageId)
                .setValue(messageModel);

        chatXml.chatRecycler.scrollToPosition(messageAdapter.getItemCount() - 1);

        chatXml.etMessage.setText("");
    }


}