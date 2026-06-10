package com.tradeanalyst.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ConversationEntity> mMessages = new ArrayList<>();

    public void setMessages(List<ConversationEntity> messages) {
        mMessages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ConversationEntity message = mMessages.get(position);
        holder.senderText.setText(message.sender);
        holder.messageText.setText(message.message);

        // Styling based on sender (Geometric Balance aesthetic)
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
        if ("User".equalsIgnoreCase(message.sender)) {
            holder.senderText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorAccent, null));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.bg_dark_emerald, null));
            holder.cardView.setStrokeWidth(2);
            holder.cardView.setStrokeColor(holder.itemView.getContext().getResources().getColor(R.color.colorPrimary, null));
            params.gravity = android.view.Gravity.END;
        } else {
            holder.senderText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorPrimary, null));
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.surface_dark_emerald, null));
            holder.cardView.setStrokeWidth(0);
            params.gravity = android.view.Gravity.START;
        }
        holder.cardView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderText;
        TextView messageText;
        MaterialCardView cardView;

        ChatViewHolder(View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.chat_sender);
            messageText = itemView.findViewById(R.id.chat_message_text);
            cardView = itemView.findViewById(R.id.chat_bubble_card);
        }
    }
}
