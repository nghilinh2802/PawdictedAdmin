package com.group7.pawdictedadmin.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.group7.pawdictedadmin.R;
import com.group7.pawdictedadmin.models.FinancialSummaryItem;
import java.util.List;

public class FinancialSummaryAdapter extends RecyclerView.Adapter<FinancialSummaryAdapter.ViewHolder> {

    private Context context;
    private List<FinancialSummaryItem> items;

    public FinancialSummaryAdapter(Context context, List<FinancialSummaryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_financial_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FinancialSummaryItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());
        holder.tvValue.setText(item.getValue());
        holder.tvValue.setTextColor(context.getResources().getColor(item.getColorResource()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvValue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
