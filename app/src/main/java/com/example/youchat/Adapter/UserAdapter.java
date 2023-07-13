package com.example.youchat.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.youchat.ChatActivity;
import com.example.youchat.GroupChatActivity;
import com.example.youchat.Model.GroupModel;
import com.example.youchat.Model.UserModel;
import com.example.youchat.R;

import java.util.ArrayList;
import java.util.List;
public class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_GROUP = 2;

    private Context context;
    private List<UserModel> userList;
    private List<GroupModel> groupList;

    public UserAdapter(Context context) {
        this.context = context;
        userList = new ArrayList<>();
        groupList = new ArrayList<>();
    }

    public void addUser(UserModel userModel) {
        userList.add(userModel);
        notifyDataSetChanged();
    }

    public void clearUsers() {
        userList.clear();
        notifyDataSetChanged();
    }

    public void addAllGroups(List<GroupModel> groups) {
        groupList.addAll(groups);
        notifyDataSetChanged();
    }

    public void clearGroups() {
        groupList.clear();
        notifyDataSetChanged();
    }

    public void addGroup(GroupModel groupModel) {
        groupList.add(groupModel);
        notifyDataSetChanged();
    }

    public void removeGroup(String groupId) {
        for (int i = 0; i < groupList.size(); i++) {
            GroupModel groupModel = groupList.get(i);
            if (groupModel.getGroupId().equals(groupId)) {
                groupList.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < userList.size()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_GROUP;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View userView = inflater.inflate(R.layout.user_row, parent, false);
            return new UserViewHolder(userView);
        } else if (viewType == VIEW_TYPE_GROUP) {
            View groupView = inflater.inflate(R.layout.group_row, parent, false);
            return new GroupViewHolder(groupView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            UserModel userModel = userList.get(position);
            userViewHolder.nameShow.setText(userModel.getUsername());
            userViewHolder.emailShow.setText(userModel.getUserEmail());

            userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("id", userModel.getUserId());
                    intent.putExtra("name", userModel.getUsername());
                    context.startActivity(intent);
                }
            });
        } else if (holder instanceof GroupViewHolder) {
            GroupViewHolder groupViewHolder = (GroupViewHolder) holder;
            int groupPosition = position - userList.size();
            GroupModel groupModel = groupList.get(groupPosition);
            groupViewHolder.groupName.setText(groupModel.getGroupName());

            groupViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, GroupChatActivity.class);
                    intent.putExtra("id", groupModel.getGroupId());
                    intent.putExtra("name", groupModel.getGroupName());
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size() + groupList.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameShow;
        TextView emailShow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameShow = itemView.findViewById(R.id.userName);
            emailShow = itemView.findViewById(R.id.userEmail);
        }
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupName;


        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.groupName);

        }
    }
}