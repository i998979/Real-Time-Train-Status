package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LineSelectorAdapter extends RecyclerView.Adapter<LineSelectorAdapter.ViewHolder> {
    private final List<HRConfig.Line> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HRConfig.Line item);
    }

    public LineSelectorAdapter(List<HRConfig.Line> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_line_select_card, p, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int p) {
        HRConfig.Line item = items.get(p);

        h.tvName.setText(item.name);
        h.tvCode.setText(item.alias);
        h.codeBadge.setBackgroundColor(Color.parseColor("#" + item.color));
        h.itemView.setOnClickListener(v -> {
            listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName;
        View codeBadge;

        ViewHolder(View v) {
            super(v);
            tvCode = v.findViewById(R.id.tv_line_code_badge);
            tvName = v.findViewById(R.id.tv_line);
            codeBadge = v.findViewById(R.id.line_color_badge);
        }
    }
}