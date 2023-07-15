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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatAtcivityBinding groupXml;

    private DatabaseReference groupChatRef;
    private GroupMessageAdapter groupMessageAdapter;
    private String groupId, groupName ;
    private static final int REQUEST_PICK_IMAGE = 1;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    Toolbar toolbar;
    String receiverName,receiversId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupXml=ActivityGroupChatAtcivityBinding.inflate(getLayoutInflater());
        setContentView(groupXml.getRoot());

        groupId=getIntent().getStringExtra("id");
        groupName=getIntent().getStringExtra("name");

        toolbar = findViewById(R.id.groupToolbar);
        toolbar.setBackgroundColor(Color.parseColor("#F9CD4A"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        groupXml.groupNameTextView.setText(groupName);

        auth=FirebaseAuth.getInstance();
        currentUser=auth.getCurrentUser();

        groupChatRef= FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId)
                .child("message");

        groupMessageAdapter=new GroupMessageAdapter(this,FirebaseAuth.getInstance().getCurrentUser().getUid(),groupChatRef);
        groupXml.groupChatRecycler.setAdapter(groupMessageAdapter);

        groupXml.groupChatRecycler.setLayoutManager(new LinearLayoutManager(this));

        fetchGroupMessages();

        groupXml.sendGroupMessageImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message=groupXml.etGroupMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message,false,receiversId,receiverName);
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
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                progressDialog.setProgress((int) progress);
            }
        });
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                return imageRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                progressDialog.dismiss();
                if (task.isSuccessful() && task.getResult() != null) {
                    Uri downloadUri = task.getResult();
                    sendMessage(downloadUri.toString(), true,receiversId,receiverName);
                } else {
                    Toast.makeText(GroupChatActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    /*private void saveImageToDataBase(String imageUrl ,String receiversId,String receiverName) {
        String messageId = UUID.randomUUID().toString();
        long timeStamp = System.currentTimeMillis();

        MessageModel  messageModel = new MessageModel(messageId,FirebaseAuth.getInstance().getUid(),receiversId,receiverName,"",timeStamp,);
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

    }*/

    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);

    }

    private void sendMessage(String message,boolean isImage,String receiversId,String receiverName) {
        DatabaseReference messageRef = groupChatRef.push();
        String messageId = messageRef.getKey();
        long currentTime = System.currentTimeMillis();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String senderName = snapshot.child("username").getValue(String.class);

                    // Create a MessageModel object with the sender's name
                    MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(), "", senderName, message, currentTime, isImage);

                    // Add the message to the adapter and database
                    groupMessageAdapter.add(messageModel);
                    messageRef.setValue(messageModel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        MessageModel messageModel = new MessageModel(messageId, FirebaseAuth.getInstance().getUid(),receiversId, receiverName, message, currentTime, isImage);
        groupMessageAdapter.add(messageModel);
        messageRef.setValue(messageModel);
    }

    private void fetchGroupMessages() {

        groupChatRef.addValueEventListener(new ValueEventListener() {
            private List<MessageModel> messageModelList=new ArrayList<>();
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
                for (MessageModel messageModel : messageModelList){
                    String receiverId = messageModel.getReceiverId();
                    if(receiverId==null)
                    {
                        receiverId="";
                    }

                    DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
                    receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String receiverName = snapshot.child("username").getValue(String.class);
                                String name=receiverName;


                                if (receiverName != null) {
                                    messageModel.setReceiverName(receiverName);
                                    messageModelList.add(messageModel);
                                }
                                //groupMessageAdapter.notifyDataSetChanged();
                            }
                        }


                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    }
                }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}