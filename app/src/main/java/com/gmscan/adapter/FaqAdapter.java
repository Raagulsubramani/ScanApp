package com.gmscan.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gmscan.R;
import com.gmscan.model.faq.FaqModel;
import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {
    private List<FaqModel> faqList;

    public FaqAdapter(List<FaqModel> faqList) {
        this.faqList = faqList;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_faq, parent, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqModel item = faqList.get(position);

        // Set question
        holder.questionTextView.setText(item.question());

        // Initially hide answer
        holder.answerTextView.setVisibility(View.GONE);
        holder.expandIcon.setImageResource(R.drawable.arrow_down); // Set default icon

        // Toggle answer visibility on click
        holder.itemView.setOnClickListener(v -> {
            if (holder.answerTextView.getVisibility() == View.GONE) {
                holder.answerTextView.setVisibility(View.VISIBLE);
                holder.answerTextView.setText(item.answer());
                holder.expandIcon.setImageResource(R.drawable.arrow_up); // Change to up icon
            } else {
                holder.answerTextView.setVisibility(View.GONE);
                holder.expandIcon.setImageResource(R.drawable.arrow_down); // Change to down icon
            }
        });
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<FaqModel> newList) {
        faqList = newList;
        notifyDataSetChanged();
    }

    public static class FaqViewHolder extends RecyclerView.ViewHolder {
        TextView questionTextView;
        TextView answerTextView;
        ImageView expandIcon;

        public FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.faq_question);
            answerTextView = itemView.findViewById(R.id.faq_answer);
            expandIcon = itemView.findViewById(R.id.expand_icon);
        }
    }
}