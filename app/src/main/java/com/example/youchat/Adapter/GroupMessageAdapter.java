package com.example.youchat.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
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
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;


public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MyViewHolder> {

    private Context context;
    private List<MessageModel> messageModelList;
    private String currentUserId;
    private DatabaseReference messagesRef;
    private SimpleExoPlayer player;
    private Handler handler;
    private boolean isPlayerPlaying;
    private int currentPlayingPosition;
    private RecyclerView recyclerView;
    private long progressBeforeSeeking;


    public GroupMessageAdapter(Context context, String currentUserId, DatabaseReference messagesRef) {
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.messagesRef = messagesRef;
        this.isPlayerPlaying = false;
        this.currentPlayingPosition = -1;
        this.player = new SimpleExoPlayer.Builder(context).build();
        this.handler =new Handler();
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
                holder.rightAudioConstraint.setVisibility(View.GONE);
                holder.leftConstraint.setVisibility(View.GONE);
                holder.rightConstraint.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                holder.rightMessage.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.rightImageView);


            } else {
                holder.leftAudioConstraint.setVisibility(View.GONE);
                holder.leftConstraint.setVisibility(View.VISIBLE);
                holder.rightConstraint.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.VISIBLE);
                holder.leftMessage.setVisibility(View.GONE);

                Glide.with(context)
                        .load(message.getMessage())
                        .into(holder.leftImageView);
            }
        }else if (message.isAudio()){
            if (isCurrentUser) {
                holder.rightConstraint.setVisibility(View.VISIBLE);
                holder.rightAudioConstraint.setVisibility(View.VISIBLE);
                holder.rightMessage.setVisibility(View.GONE);
                holder.rightImageView.setVisibility(View.GONE);
                setAudioPlayer(holder.rightBtnPlayPause, holder.rightSeekBar, holder.rightAudioCurrentTime, holder.rightAudioTotalDuration, position);
                updateAudioPlaybackUI(holder.rightBtnPlayPause, holder.rightSeekBar, holder.rightAudioCurrentTime, holder.rightAudioTotalDuration, position);
            } else {
                holder.leftConstraint.setVisibility(View.VISIBLE);
                holder.leftAudioConstraint.setVisibility(View.VISIBLE);
                holder.leftMessage.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.GONE);
                setAudioPlayer(holder.leftBtnPlayPause, holder.leftSeekBar, holder.leftAudioCurrentTime, holder.leftAudioTotalDuration, position);
                updateAudioPlaybackUI(holder.leftBtnPlayPause, holder.leftSeekBar, holder.leftAudioCurrentTime, holder.leftAudioTotalDuration, position);
            }
        }
        else {
            // Display text message
            if (message.getSenderId().equals(currentUserId)) {
                holder.rightAudioConstraint.setVisibility(View.GONE);
                holder.leftAudioConstraint.setVisibility(View.GONE);
                holder.leftConstraint.setVisibility(View.GONE);
                holder.rightConstraint.setVisibility(View.VISIBLE);
                holder.rightMessage.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.GONE);

                holder.rightMessage.setText(message.getMessage());
            } else {
                holder.rightAudioConstraint.setVisibility(View.GONE);
                holder.leftAudioConstraint.setVisibility(View.GONE);
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

    public void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
            handler.removeCallbacks(progressRunnable);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout leftConstraint, rightConstraint;
        TextView leftMessage, rightMessage;
        TextView leftTimeTextView, rightTimeTextView;
        ImageView leftImageView, rightImageView;
        TextView receiverNameTextView;

        ConstraintLayout leftAudioConstraint, rightAudioConstraint;
        ImageButton leftBtnPlayPause, rightBtnPlayPause;
        SeekBar leftSeekBar, rightSeekBar;
        TextView leftAudioCurrentTime, rightAudioCurrentTime, leftAudioTotalDuration, rightAudioTotalDuration;


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

            leftAudioConstraint = itemView.findViewById(R.id.leftAudioConstraint);
            rightAudioConstraint = itemView.findViewById(R.id.rightAudioConstraint);
            leftBtnPlayPause = itemView.findViewById(R.id.leftBtnPlayPause);
            rightBtnPlayPause = itemView.findViewById(R.id.rightBtnPlayPause);
            leftSeekBar = itemView.findViewById(R.id.leftSeekBar);
            rightSeekBar = itemView.findViewById(R.id.rightSeekBar);
            leftAudioCurrentTime = itemView.findViewById(R.id.leftAudioCurrentTime);
            rightAudioCurrentTime = itemView.findViewById(R.id.rightAudioCurrentTime);
            leftAudioTotalDuration = itemView.findViewById(R.id.leftAudioTotalDuration);
            rightAudioTotalDuration = itemView.findViewById(R.id.rightAudioTotalDuration);




        }

        public boolean isAudioMessageFromCurrentUser() {
                MessageModel message = messageModelList.get(getAdapterPosition());
                String senderId = message.getSenderId();
                return senderId.equals(currentUserId);
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
    private void updateAudioPlaybackUI(ImageButton btnPlayPause, SeekBar seekBar, TextView audioCurrentTime, TextView audioTotalDuration, int position) {
        if (position == currentPlayingPosition) {
            btnPlayPause.setImageResource(isPlayerPlaying ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24);
            seekBar.setProgress((int) (player.getCurrentPosition() / 1000L));
            audioCurrentTime.setText(formatTime(player.getCurrentPosition()));
            audioTotalDuration.setText(formatTime(player.getDuration()));
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            seekBar.setProgress(0);
            audioCurrentTime.setText("00:00");
            audioTotalDuration.setText("00:00");
        }
    }


    private void setAudioPlayer(ImageButton btnPlayPause, SeekBar seekBar, TextView audioCurrentTime, TextView audioTotalDuration, int position) {


        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAudioPlayback(position);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long seekPosition = progress * 1000L;
                    audioCurrentTime.setText(formatTime(seekPosition));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressBeforeSeeking = player.getCurrentPosition();
                pauseAudioPlayback();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                long seekPosition = seekBar.getProgress() * 1000L;
                player.seekTo(seekPosition);
                audioCurrentTime.setText(formatTime(seekPosition));
                if (isPlayerPlaying) {
                    resumeAudioPlayback();
                }
            }
        });


        player.addListener(new Player.EventListener(){
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY){
                    long duration = player.getDuration();
                    audioTotalDuration.setText(formatTime(duration));
                    seekBar.setMax((int) (duration / 1000L)); // Convert duration to seconds
                    seekBar.setEnabled(true);
                    updatePlaybackProgress(position);

                    updatePlayPauseButton();

                    notifyDataSetChanged();

                }else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE || state == Player.STATE_BUFFERING) {
                    btnPlayPause.setImageResource(R.drawable.ic_baseline_pause_24);
                    isPlayerPlaying = false;
                    currentPlayingPosition = -1;
                    seekBar.setProgress(0);
                    seekBar.setEnabled(false);
                    handler.removeCallbacks(progressRunnable);

                    updatePlayPauseButton();

                    notifyDataSetChanged();

                }
            }
        });
    }
    private void toggleAudioPlayback(int position) {
        if (position == currentPlayingPosition) {
            if (isPlayerPlaying) {
                pauseAudioPlayback();
            } else {
                resumeAudioPlayback();
            }
        } else {
            startNewAudioPlayback(position);
        }
    }

    private void startNewAudioPlayback(int position) {
        if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
            stopAudioPlayback();
        }
        MessageModel message = messageModelList.get(position);
        String audioUrl = message.getMessage();

        // Prepare media item and set it to the player
        MediaItem mediaItem = MediaItem.fromUri(audioUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        // Update playback state
        currentPlayingPosition = position;
        isPlayerPlaying = true;
        handler.post(progressRunnable);
    }
    private void pauseAudioPlayback() {
        player.pause();
        isPlayerPlaying = false;
        handler.removeCallbacks(progressRunnable);
        updatePlayPauseButton();
    }

    private void resumeAudioPlayback() {
        player.play();
        isPlayerPlaying = true;
        handler.post(progressRunnable);
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        MyViewHolder viewHolder = (MyViewHolder) recyclerView.findViewHolderForAdapterPosition(currentPlayingPosition);
        if (viewHolder != null) {
            if (viewHolder.isAudioMessageFromCurrentUser()) {
                viewHolder.rightBtnPlayPause.setImageResource(isPlayerPlaying ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24);
            } else {
                viewHolder.leftBtnPlayPause.setImageResource(isPlayerPlaying ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24);
            }
        }
    }

    private void stopAudioPlayback() {
        player.stop();
        player.clearMediaItems();
        isPlayerPlaying = false;
        currentPlayingPosition = -1;
        handler.removeCallbacks(progressRunnable);
    }

    private void updatePlaybackProgress(int position) {
        long currentPosition = player.getCurrentPosition();
        long totalDuration = player.getDuration();
        long progress = (currentPosition * 100) / totalDuration;

        MyViewHolder viewHolder = (MyViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        if (viewHolder != null) {
            SeekBar seekBar;
            TextView currentTimeTextView;
            TextView totalDurationTextView;

            if (viewHolder.isAudioMessageFromCurrentUser()) {
                seekBar = viewHolder.rightSeekBar;
                currentTimeTextView = viewHolder.rightAudioCurrentTime;
                totalDurationTextView = viewHolder.rightAudioTotalDuration;
            } else {
                seekBar = viewHolder.leftSeekBar;
                currentTimeTextView = viewHolder.leftAudioCurrentTime;
                totalDurationTextView = viewHolder.leftAudioTotalDuration;
            }

            seekBar.setProgress((int) (currentPosition / 1000L)); // Convert milliseconds to seconds
            currentTimeTextView.setText(formatTime(currentPosition));
            totalDurationTextView.setText(formatTime(totalDuration));
        }

        handler.postDelayed(progressRunnable, 1000);
    }


    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlaybackProgress(currentPlayingPosition);
        }
    };

    private String formatTime(long milliseconds) {
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
}