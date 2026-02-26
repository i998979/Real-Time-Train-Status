package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchLineAdapter extends RecyclerView.Adapter<SearchLineAdapter.ViewHolder> {

    private List<Integer> ids = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<String> codes = new ArrayList<>();

    private final OnLineClickListener listener;

    public interface OnLineClickListener {
        void onLineClick(int id, String name, String code);
    }

    public SearchLineAdapter(OnLineClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Integer> newIds, List<String> newNames, List<String> newCodes) {
        this.ids = newIds;
        this.names = newNames;
        this.codes = newCodes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_line, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int id = ids.get(position);
        String name = names.get(position);
        String code = codes.get(position);

        holder.tvLineName.setText(name);
        holder.tvLineCodeBadge.setText(code.toUpperCase());

        HRConfig config = HRConfig.getInstance(holder.itemView.getContext());
        HRConfig.Line line = config.getLineMap().get(id);

        holder.lineColorBadge.setBackgroundColor(Color.parseColor("#" + line.color));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLineClick(id, name, code);
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLineName;
        TextView tvLineCodeBadge;
        FrameLayout lineColorBadge;

        public ViewHolder(View itemView) {
            super(itemView);
            tvLineName = itemView.findViewById(R.id.tv_line);
            tvLineCodeBadge = itemView.findViewById(R.id.tv_line_code_badge);
            lineColorBadge = itemView.findViewById(R.id.line_color_badge);
        }
    }
}