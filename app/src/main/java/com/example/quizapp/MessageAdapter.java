package com.example.quizapp;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Message.TYPE_TYPING) {
            View v = inflater.inflate(R.layout.item_message_typing, parent, false);
            return new TypingViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).startAnimation();
            return;
        }
        MessageViewHolder vh  = (MessageViewHolder) holder;
        Message           msg = messages.get(position);
        vh.tvMessage.setText(msg.getText());

        if (msg.getType() == Message.TYPE_USER) {
            vh.container.setGravity(Gravity.END);
            vh.tvMessage.setBackgroundResource(R.drawable.chat_bubble_user);
            vh.tvMessage.setTextColor(Color.WHITE);
        } else {
            vh.container.setGravity(Gravity.START);
            vh.tvMessage.setBackgroundResource(R.drawable.chat_bubble_bot);
            vh.tvMessage.setTextColor(Color.parseColor("#2D3B2D"));
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).stopAnimation();
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ─── ViewHolders ─────────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView     tvMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;
        ObjectAnimator anim1, anim2, anim3;

        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void startAnimation() {
            anim1 = bounce(dot1, 0);
            anim2 = bounce(dot2, 150);
            anim3 = bounce(dot3, 300);
        }

        void stopAnimation() {
            if (anim1 != null) anim1.cancel();
            if (anim2 != null) anim2.cancel();
            if (anim3 != null) anim3.cancel();
            if (dot1 != null) dot1.setTranslationY(0);
            if (dot2 != null) dot2.setTranslationY(0);
            if (dot3 != null) dot3.setTranslationY(0);
        }

        private ObjectAnimator bounce(View v, int startDelay) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(v, "translationY", 0f, -10f, 0f);
            anim.setDuration(600);
            anim.setStartDelay(startDelay);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
            return anim;
        }
    }
}
