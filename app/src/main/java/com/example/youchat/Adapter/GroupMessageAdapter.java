package com.example.youchat.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.youchat.Model.GroupMessageModel;
import com.example.youchat.Model.MessageModel;
import com.example.youchat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MyViewHolder> {

    private Context context;
    private List<MessageModel> messageModelList;
    private String currentUserId;
    private DatabaseReference messagesRef;

    public GroupMessageAdapter(Context context, String currentUserId, DatabaseReference messagesRef) {
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.messagesRef = messagesRef;
    }

    public void addAll(List<MessageModel> messages) {
        int previousSize = messageModelList.size();
        messageModelList.addAll(messages);
        notifyItemRangeInserted(previousSize, messages.size());
    }

    public void add(MessageModel message) {
        messageModelList.add(message);
        notifyDataSetChanged();
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_message_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {



        MessageModel message = messageModelList.get(position);

        boolean isCurrentUser = message.getSenderId().equals(currentUserId);


        holder.leftConstraint.setVisibility(View.GONE);
        holder.rightConstraint.setVisibility(View.GONE);

        if (message.isImage()) {
            if (message.getSenderId().equals(currentUserId)) {
                holder.leftConstraint.setVisibility(View.GONE);
                holder.rightConstraint.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                holder.rightMessage.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.rightImageView);


            } else {
                holder.leftConstraint.setVisibility(View.VISIBLE);
                holder.rightConstraint.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.VISIBLE);
                holder.leftMessage.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.leftImageView);
            }
        } else {
            // Display text message
            if (message.getSenderId().equals(currentUserId)) {
                holder.leftConstraint.setVisibility(View.GONE);
                holder.rightConstraint.setVisibility(View.VISIBLE);
                holder.rightMessage.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.GONE);

                holder.rightMessage.setText(message.getMessage());
            } else {
                holder.leftConstraint.setVisibility(View.VISIBLE);
                holder.rightConstraint.setVisibility(View.GONE);
                holder.leftMessage.setVisibility(View.VISIBLE);
                holder.leftImageView.setVisibility(View.GONE);

                holder.leftMessage.setText(message.getMessage());
            }
        }

        // Set timestamp
        if (message.getSenderId().equals(currentUserId)) {
            holder.rightTimeTextView.setText(formatTimestamp(message.getTime()));

        } else {
            holder.leftTimeTextView.setText(formatTimestamp(message.getTime()));
            holder.receiverNameTextView.setText(message.getReceiverName());
        }


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                    showDeletePopup(message);

                return true;
            }
        });



    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout leftConstraint, rightConstraint;
        TextView leftMessage, rightMessage;
        TextView leftTimeTextView, rightTimeTextView;
        ImageView leftImageView, rightImageView;
        TextView receiverNameTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            leftConstraint = itemView.findViewById(R.id.leftChatLayout);
            rightConstraint = itemView.findViewById(R.id.rightChatLayout);

            leftMessage = itemView.findViewById(R.id.leftMessage);
            rightMessage = itemView.findViewById(R.id.RightMessage);

            leftTimeTextView = itemView.findViewById(R.id.leftTimeStampTextView);
            rightTimeTextView = itemView.findViewById(R.id.rightTimeStampTextView);

            leftImageView = itemView.findViewById(R.id.leftImageView);
            rightImageView = itemView.findViewById(R.id.rightImageView);

            receiverNameTextView = itemView.findViewById(R.id.groupReceiverNameTextView);
        }
    }

    private String formatTimestamp(long timestamp) {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat.format(new Date(timestamp));
    }

    private void showDeletePopup(MessageModel message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete this message?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteMessage(message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteMessage(MessageModel message) {
        String messageId = message.getMsgId();
        if (messageId != null) {
            DatabaseReference messageReference = messagesRef.child(messageId);

            messageReference.removeValue()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(context, "Message ID is null", Toast.LENGTH_SHORT).show();
        }
    }
}