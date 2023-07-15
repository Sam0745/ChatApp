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
import com.example.youchat.Model.MessageModel;
import com.example.youchat.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ktx.Firebase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    private Context context;
    private List<MessageModel> messageModelList;
    private String senderRoom, receiverRoom;


    public MessageAdapter(Context context, String senderRoom, String receiverRoom) {
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
    }

    public void addAll(List<MessageModel> messages) {
        messageModelList.addAll(messages);
        notifyDataSetChanged();
    }
    public void add(MessageModel message) {
        messageModelList.add(message);
        notifyDataSetChanged();
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    public void deleteMessage(String messageId,String imageUrl) {
        DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom).child(messageId);
        DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom).child(messageId);

        senderRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                receiverRef.removeValue();
                notifyDataSetChanged();
                if (imageUrl != null && !imageUrl.isEmpty()){
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            notifyDataSetChanged();
                            Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete the image", Toast.LENGTH_SHORT).show();
                    });
                }

            }
        });
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_message_row,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MessageModel message = messageModelList.get(position);

        holder.leftConstraint.setVisibility(View.GONE);
        holder.rightConstraint.setVisibility(View.GONE);

        if (message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            holder.leftConstraint.setVisibility(View.GONE);
            holder.rightConstraint.setVisibility(View.VISIBLE);
            holder.rightMessage.setVisibility(View.VISIBLE);
            holder.rightMessage.setText(message.getMessage());
            holder.rightImageView.setVisibility(View.GONE);
            holder.rightConstraint.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDeletePopup(message.getMsgId(), message.getImageUrl());
                    return true;
                }
            });

        } else {
            holder.leftConstraint.setVisibility(View.VISIBLE);
            holder.rightConstraint.setVisibility(View.GONE);
            holder.leftMessage.setVisibility(View.VISIBLE);
            holder.leftMessage.setText(message.getMessage());
            holder.receiverNameTextView.setText(message.getReceiverName());
            holder.leftImageView.setVisibility(View.GONE);
            holder.leftConstraint.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showDeletePopup(message.getMsgId(), message.getImageUrl());
                    return true;
                }
            });
        }

        if (message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            holder.rightTimeTextView.setText(formatTimestamp(message.getTime()));
        } else {
            holder.leftTimeTextView.setText(formatTimestamp(message.getTime()));
            holder.receiverNameTextView.setText(message.getReceiverName());
        }

        if (!TextUtils.isEmpty(message.getImageUrl())) {
            if (message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                holder.rightMessage.setVisibility(View.GONE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.rightImageView);
            } else {
                holder.leftMessage.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.leftImageView);
            }
        } else {

            holder.leftImageView.setVisibility(View.GONE);
            holder.rightImageView.setVisibility(View.GONE);
        }
    }

    private void showDeletePopup(String messageId,String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteMessage(messageId, imageUrl);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageModelList.get(position);
        if (message.getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
            return 0; // Sender message
        } else {
            return 1; // Receiver message
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout leftConstraint,rightConstraint;
        TextView leftMessage,rightMessage;
        TextView leftTimeTextView,rightTimeTextView;
        ImageView leftImageView, rightImageView;
        TextView receiverNameTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            leftConstraint=itemView.findViewById(R.id.leftChatLayout);
            rightConstraint=itemView.findViewById(R.id.rightChatLayout);

            leftMessage=itemView.findViewById(R.id.leftMessage);
            rightMessage=itemView.findViewById(R.id.RightMessage);


            leftTimeTextView=itemView.findViewById(R.id.leftTimeStampTextView);
            rightTimeTextView=itemView.findViewById(R.id.rightTimeStampTextView);

            leftImageView=itemView.findViewById(R.id.leftImageView);
            rightImageView=itemView.findViewById(R.id.rightImageView);

            receiverNameTextView = itemView.findViewById(R.id.receiverNameTextView);

        }
    }
    private String formatTimestamp(long timestamp) {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat.format(new Date(timestamp));
    }
}