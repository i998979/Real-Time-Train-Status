package to.epac.factorycraft.transitapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchStationAdapter extends RecyclerView.Adapter<SearchStationAdapter.ViewHolder> {

    private List<Integer> ids = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<String> codes = new ArrayList<>();

    private List<String> favoriteIds = new ArrayList<>();

    private final OnStationClickListener listener;

    public interface OnStationClickListener {
        void onStationClick(int id, String name, String code);
        void onFavoriteClick(int id, String name, String code);
    }

    public SearchStationAdapter(OnStationClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Integer> newIds, List<String> newNames, List<String> newCodes) {
        this.ids = newIds;
        this.names = newNames;
        this.codes = newCodes;
        notifyDataSetChanged();
    }

    public void updateFavorites(List<String> favorites) {
        this.favoriteIds = favorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int id = ids.get(position);
        String name = names.get(position);
        String code = codes.get(position);

        boolean isFavorite = favoriteIds.contains(String.valueOf(id));
        holder.ivFavorite.setColorFilter(Color.parseColor(isFavorite ? "#6EC08D" : "#E0E0E0"));

        holder.tvStation.setText(name);
        holder.itemView.setOnClickListener(v -> {
            listener.onStationClick(id, name, code);
        });
        holder.ivFavorite.setOnClickListener(v -> {
            listener.onFavoriteClick(id, name, code);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStation;
        ImageView ivFavorite;

        private ViewHolder(View itemView) {
            super(itemView);
            tvStation = itemView.findViewById(R.id.tv_station);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
        }
    }
}