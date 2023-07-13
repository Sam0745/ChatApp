package com.example.youchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.youchat.Adapter.GroupMessageAdapter;
import com.example.youchat.Model.GroupModel;
import com.example.youchat.Model.MessageModel;
import com.example.youchat.databinding.ActivityGroupChatAtcivityBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatAtcivityBinding groupXml;
    private List<MessageModel> messageModelList;
    private DatabaseReference groupChatRef;
    private GroupMessageAdapter groupMessageAdapter;
    private String groupId, groupName ;
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 100;

    Toolbar toolbar;
    private GroupModel groupModel;
    private String receiverName,receiversId;


    private static final int REQUEST_IMAGE_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupXml=ActivityGroupChatAtcivityBinding.inflate(getLayoutInflater());
        setContentView(groupXml.getRoot());

        groupId=getIntent().getStringExtra("id");
        groupName=getIntent().getStringExtra("name");

        groupChatRef= FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId)
                .child("message");

        toolbar = findViewById(R.id.groupToolbar);
        toolbar.setBackgroundColor(Color.parseColor("#F9CD4A"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        groupXml.groupNameTextView.setText(groupName);

        groupXml.groupChatRecycler.setLayoutManager(new LinearLayoutManager(this));
        groupXml.groupChatRecycler.setHasFixedSize(true);

        messageModelList=new ArrayList<>();
        groupMessageAdapter=new GroupMessageAdapter(this,FirebaseAuth.getInstance().getCurrentUser().getUid(),groupChatRef);
        groupXml.groupChatRecycler.setAdapter(groupMessageAdapter);

        fetchGroupMessages();



        groupXml.sendGroupMessageImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message=groupXml.etGroupMessage.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    sendMessage(message,receiversId,receiverName);
                    groupXml.etGroupMessage.setText("");
                }
            }
        });

        groupXml.shareGroupImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();


            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_PICK_IMAGE && resultCode == RESULT_OK ){
            if (data != null && data.getData() != null){
                Uri imageUri = data.getData();
                uploadImageToStorage(imageUri);
            }
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

        ProgressBar progressBar=findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        UploadTask uploadTask= imageRef.putFile(imageUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setProgress((int) progress);
        });

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl=uri.toString();
                saveImageToDataBase(imageUrl);
                progressDialog.dismiss();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveImageToDataBase(String imageUrl) {
        String messageId = UUID.randomUUID().toString();
        long timeStamp = System.currentTimeMillis();

        MessageModel  messageModel = new MessageModel(messageId,FirebaseAuth.getInstance().getUid(),receiversId,receiverName,"",timeStamp,imageUrl);
        groupMessageAdapter.add(messageModel);

        groupChatRef
                .child(messageId)
                .setValue(messageModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }

    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);

    }

    private void sendMessage(String message,String receiversId,String receiverName) {
        DatabaseReference messageRef = groupChatRef.push();
        String messageId = messageRef.getKey();
        long currentTime = System.currentTimeMillis();

        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(),receiversId,receiverName, message, currentTime, null);
        groupMessageAdapter.add(messageModel);
        messageRef.setValue(messageModel);
    }

    private void fetchGroupMessages() {

        groupChatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModelList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    messageModelList.add(messageModel);
                }
                groupMessageAdapter.clear();
                groupMessageAdapter.addAll(messageModelList);
                groupXml.groupChatRecycler.scrollToPosition(messageModelList.size() - 1);

                if (messageModelList.isEmpty()) {
                    groupXml.lottieAnimationView.setVisibility(View.VISIBLE);
                    groupXml.groupChatRecycler.setVisibility(View.GONE);
                } else {
                    groupXml.lottieAnimationView.setVisibility(View.GONE);
                    groupXml.groupChatRecycler.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}