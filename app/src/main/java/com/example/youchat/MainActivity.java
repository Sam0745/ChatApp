package com.example.youchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.youchat.Adapter.UserAdapter;
import com.example.youchat.Adapter.UserListAdapter;
import com.example.youchat.Model.GroupModel;
import com.example.youchat.Model.UserModel;
import com.example.youchat.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding mainXml;
    DatabaseReference userReference, groupReference;
    UserAdapter userAdapter;
    Toolbar toolbar;
    private List<GroupModel> groupList;
    private List<UserModel> userList;
    private AlertDialog createGroupDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainXml = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainXml.getRoot());

        toolbar = findViewById(R.id.mainToolbar);
        toolbar.setBackgroundColor(Color.parseColor("#F3EFF5"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        userReference = FirebaseDatabase.getInstance().getReference("users");
        groupReference = FirebaseDatabase.getInstance().getReference("groups");
        groupList=new ArrayList<>();
        userList=new ArrayList<>();

        userAdapter=new UserAdapter(this);
        mainXml.userRecycler.setAdapter(userAdapter);
        mainXml.userRecycler.setLayoutManager(new LinearLayoutManager(this));


        retrieveUsers();
        retrieveGroups();



        mainXml.createGroupFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieveUsersForGroupCreation();
            }
        });

        mainXml.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
                finish();
            }
        });
    }

    private void retrieveUsersForGroupCreation() {
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    String uid=dataSnapshot.getKey();
                    if (!uid.equals(FirebaseAuth.getInstance().getUid())){
                        UserModel userModel=dataSnapshot.getValue(UserModel.class);
                        userList.add(userModel);
                    }
                }
                showCreateGroupDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void retrieveUsers() {
        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (userAdapter != null) {
                    userAdapter.clearUsers();
                }
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String uid = dataSnapshot.getKey();

                    if (!uid.equals(FirebaseAuth.getInstance().getUid())) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        userAdapter.addUser(userModel);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void retrieveGroups() {
        groupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    GroupModel groupModel = dataSnapshot.getValue(GroupModel.class);
                    String groupId = groupModel.getGroupId();

                    DatabaseReference groupMembersRef = groupReference.child(groupId).child("members");
                    groupMembersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(FirebaseAuth.getInstance().getUid())) {
                                groupList.add(groupModel);
                                userAdapter.addGroup(groupModel);
                            } else {
                                userAdapter.removeGroup(groupId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(MainActivity.this, "Failed to retrieve group members", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to retrieve groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Group");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        builder.setView(dialogView);

        EditText groupNameEditText = dialogView.findViewById(R.id.groupNameEditText);
        ListView membersListView = dialogView.findViewById(R.id.membersListView);

        UserListAdapter userListAdapter = new UserListAdapter(userList);
        membersListView.setAdapter(userListAdapter);

        builder.setPositiveButton("Create", null);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        createGroupDialog = builder.create();


        createGroupDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button createButton = createGroupDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                createButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String groupName = groupNameEditText.getText().toString().trim();

                        if (groupName.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Enter Group Name", Toast.LENGTH_SHORT).show();
                        } else if (groupName.length() < 3) {
                            Toast.makeText(MainActivity.this, "Group Name must be at least 3 characters long", Toast.LENGTH_SHORT).show();
                        } else {
                            List<String> selectedUserIds = userListAdapter.getSelectedUserIds();
                            if (selectedUserIds.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Select at least one member for the group", Toast.LENGTH_SHORT).show();
                            } else {
                                createGroup(groupName, selectedUserIds);
                                createGroupDialog.dismiss();
                            }
                        }
                    }
                });
            }
        });

        createGroupDialog.show();
    }

    private void createGroup(String groupName, List<String> selectedUserIds) {

        DatabaseReference groupRef =groupReference.push();
        String groupID=groupRef.getKey();

        GroupModel groupModel=new GroupModel(groupID,groupName);
        groupRef.setValue(groupModel);

       selectedUserIds.add(FirebaseAuth.getInstance().getUid());

       for (String userId : selectedUserIds){
           DatabaseReference userGroupRef=userReference.child(userId).child("groups").child(groupID);
           userGroupRef.setValue(true);
       }

       DatabaseReference groupMemberRef=groupRef.child("members");
       for (String userId : selectedUserIds){
           groupMemberRef.child(userId).setValue(true);
       }
        groupList.add(groupModel);
        userAdapter.clearGroups();
        userAdapter.addAllGroups(groupList);
        Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
    }
}