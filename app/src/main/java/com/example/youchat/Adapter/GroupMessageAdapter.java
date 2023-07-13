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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        holder.leftConstraint.setVisibility(isCurrentUser ? View.GONE : View.VISIBLE);
        holder.rightConstraint.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);

        if (isCurrentUser) {
            holder.rightMessage.setVisibility(View.VISIBLE);
            holder.rightMessage.setText(message.getMessage());
            holder.rightImageView.setVisibility(View.GONE);
            holder.rightConstraint.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDeletePopup(message);
                    return true;
                }
            });
        } else {
            holder.leftMessage.setVisibility(View.VISIBLE);
            holder.leftMessage.setText(message.getMessage());
            holder.leftImageView.setVisibility(View.GONE);
            holder.leftConstraint.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showDeletePopup(message);
                    return true;
                }
            });
        }

        String timestamp = formatTimestamp(message.getTime());
        holder.leftTimeTextView.setText(timestamp);
        holder.rightTimeTextView.setText(timestamp);

        String imageUrl = message.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            if (isCurrentUser) {
                holder.rightMessage.setVisibility(View.GONE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.rightImageView);
            } else {
                holder.leftMessage.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.leftImageView);
            }
        } else {
            holder.leftImageView.setVisibility(View.GONE);
            holder.rightImageView.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (isCurrentUser) {
                    showDeletePopup(message);
                }
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
        String imageUrl = message.getImageUrl();

        messagesRef.child(messageId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageModelList.remove(message);
                notifyDataSetChanged();

                if (!TextUtils.isEmpty(imageUrl)) {
                    // Delete the image from storage if it exists
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Message deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete the message", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(context, "Message deleted successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to delete the message", Toast.LENGTH_SHORT).show();
            }
        });
    }
}