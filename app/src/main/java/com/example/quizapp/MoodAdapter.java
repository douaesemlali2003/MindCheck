package com.example.quizapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {

    private List<MoodEntry> moodList;

    public MoodAdapter(List<MoodEntry> moodList) {
        this.moodList = moodList;
    }

    @NonNull
    @Override
    public MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_history, parent, false);
        return new MoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
        MoodEntry mood = moodList.get(position);
        holder.tvEmoji.setText(mood.getEmoji());
        holder.tvNote.setText(mood.getNote());

        if (mood.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(mood.getDate().toDate()));
        }

        holder.tvNote.setVisibility(
                (mood.getNote() == null || mood.getNote().isEmpty()) ? View.GONE : View.VISIBLE);

        // Photo
        String photo = mood.getPhoto();
        if (photo != null && !photo.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(photo, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivItemPhoto.setImageBitmap(bmp);
                holder.ivItemPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.ivItemPhoto.setVisibility(View.GONE);
            }
        } else {
            holder.ivItemPhoto.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return moodList.size();
    }

    static class MoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvDate, tvNote;
        ImageView ivItemPhoto;

        public MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji     = itemView.findViewById(R.id.tvItemEmoji);
            tvDate      = itemView.findViewById(R.id.tvItemDate);
            tvNote      = itemView.findViewById(R.id.tvItemNote);
            ivItemPhoto = itemView.findViewById(R.id.ivItemPhoto);
        }
    }
}
