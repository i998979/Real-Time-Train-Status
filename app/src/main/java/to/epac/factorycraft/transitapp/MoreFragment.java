package to.epac.factorycraft.transitapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.card.MaterialCardView;

public class MoreFragment extends Fragment {

    private PackageManager pm;

    private MaterialCardView btnOctopus;
    private MaterialCardView btnMtrMobile;
    private LinearLayout mediaContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        pm = requireContext().getPackageManager();

        btnOctopus = view.findViewById(R.id.btn_octopus);
        btnOctopus.setOnClickListener(v -> openExternalApp(MainActivity.OCTOPUS_PACKAGE));
        btnMtrMobile = view.findViewById(R.id.btn_mtrmobile);
        btnMtrMobile.setOnClickListener(v -> openExternalApp(MainActivity.MTRMOBILE_PACKAGE));
        mediaContainer = view.findViewById(R.id.media_container);


        int[] images = {R.drawable.a, R.drawable.b, R.drawable.c, R.drawable.d, R.drawable.e};
        String[] titles = {"尖東路軌改道工程", "現代化列車退役", "CHiiKAWA DAYS", "站見鐵路展", "市區綫復古列車"};

        for (int i = 0; i < images.length; i++) {
            View card = inflater.inflate(R.layout.item_jr_media_card, mediaContainer, false);

            ImageView ivMedia = card.findViewById(R.id.iv_media_image);
            TextView tvRank = card.findViewById(R.id.tv_media_rank);
            TextView tvTitle = card.findViewById(R.id.tv_media_title);

            ivMedia.setImageResource(images[i]);
            tvRank.setText(String.valueOf(i + 1));
            tvTitle.setText(titles[i]);

            card.setOnClickListener(v -> {
                // Card click listener
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Utils.dpToPx(requireContext(), 140),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (i < images.length - 1) {
                params.rightMargin = Utils.dpToPx(requireContext(), 12);
            } else {
                params.rightMargin = 0;
            }

            card.setLayoutParams(params);

            mediaContainer.addView(card);
        }


        GridLayout gridContainer = view.findViewById(R.id.grid_menu_container);

        String[] labels = {"鐵流 Railic HK", "鐵流 Railic HK", "footsteps_33.6km", "footsteps_33.6km"};
        int[] icons = {R.drawable.railic, R.drawable.youtube, R.drawable.footsteps_33_6km, R.drawable.youtube};
        String[] urls = {"https://www.instagram.com/railichk/", "https://www.youtube.com/@RailicHongKong",
                "https://www.instagram.com/footsteps_33.6km", "https://www.youtube.com/@i998979"};

        for (int i = 0; i < labels.length; i++) {
            View itemView = inflater.inflate(R.layout.item_grid_menu, gridContainer, false);

            ((ImageView) itemView.findViewById(R.id.iv_grid_icon)).setImageResource(icons[i]);
            ((TextView) itemView.findViewById(R.id.tv_grid_label)).setText(labels[i]);

            final String url = urls[i];
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            itemView.setLayoutParams(params);

            gridContainer.addView(itemView);
        }

        return view;
    }

    private void openExternalApp(String packageName) {
        try {
            Intent intent = pm.getLaunchIntentForPackage(packageName);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                goToPlayStore(packageName);
            }

        } catch (Exception e) {
            goToPlayStore(packageName);
        }
    }

    private void goToPlayStore(String appPackage) {
        Uri uri = Uri.parse("market://details?id=" + appPackage);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackage)));
        }
    }
}