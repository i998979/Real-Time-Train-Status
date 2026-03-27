package to.epac.factorycraft.transitapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MoreFragment extends Fragment {

    private MaterialButton btnMenu;
    private MaterialCardView btnOctopus;
    private MaterialCardView btnMtrMobile;

    private LinearLayout mediaContainer;
    private GridLayout gridContainer;
    private final String JSON_URL = "https://raw.githubusercontent.com/i998979/Real-Time-Train-Status-Private/refs/heads/main/TransitApp.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        btnMenu = view.findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MenuActivity.class);
            v.getContext().startActivity(intent);
        });
        btnOctopus = view.findViewById(R.id.btn_octopus);
        btnOctopus.setOnClickListener(v -> openExternalApp(MainActivity.OCTOPUS_PACKAGE));
        btnMtrMobile = view.findViewById(R.id.btn_mtrmobile);
        btnMtrMobile.setOnClickListener(v -> openExternalApp(MainActivity.MTRMOBILE_PACKAGE));
        mediaContainer = view.findViewById(R.id.media_container);
        gridContainer = view.findViewById(R.id.grid_menu_container);

        setupGridMenu(inflater);
        showPlaceholders(inflater);
        loadMediaData(inflater);

        return view;
    }


    private void setupGridMenu(LayoutInflater inflater) {
        String[] titles = {"鐵流 Railic HK", "鐵流 Railic HK", "footsteps_33.6km", "footsteps_33.6km"};
        int[] icons = {R.drawable.railic, R.drawable.youtube, R.drawable.footsteps_33_6km, R.drawable.youtube};
        String[] urls = {"https://www.instagram.com/railichk/", "https://www.youtube.com/@RailicHongKong",
                "https://www.instagram.com/footsteps_33.6km", "https://www.youtube.com/@i998979"};

        for (int i = 0; i < titles.length; i++) {
            View v = inflater.inflate(R.layout.item_grid_menu, gridContainer, false);
            ((ImageView) v.findViewById(R.id.iv_grid_icon)).setImageResource(icons[i]);
            ((TextView) v.findViewById(R.id.tv_grid_label)).setText(titles[i]);

            String url = urls[i];
            v.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            v.setLayoutParams(p);
            gridContainer.addView(v);
        }
    }

    private void showPlaceholders(LayoutInflater inflater) {
        List<MediaItem> placeholders = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            placeholders.add(new MediaItem(null, null, null, -1));
        }
        renderCards(inflater, placeholders);
    }

    private void loadMediaData(LayoutInflater inflater) {
        new Thread(() -> {
            List<MediaItem> items = new ArrayList<>();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(JSON_URL).openConnection();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String json = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
                    JSONObject root = new JSONObject(json);

                    JSONObject thumbs = root.getJSONObject("thumbnail");
                    JSONObject titles = root.getJSONObject("title");
                    JSONObject urls = root.getJSONObject("url");

                    for (int i = 0; i < thumbs.length(); i++) {
                        String k = String.valueOf(i);
                        items.add(new MediaItem(thumbs.getString(k), titles.getString(k), urls.getString(k), -1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback
            if (items.isEmpty()) {
                int[] images = {R.drawable.a, R.drawable.b, R.drawable.c, R.drawable.d, R.drawable.e};
                String[] titles = {"N-Scale 港鐵 M-Train\n「首發限量版」評價", "尖東路軌改道工程", "CHiiKAWA DAYS", "站見鐵路展", "市區綫復古列車"};
                String[] urls = {"https://www.instagram.com/railichk/p/DWVv7vwlOmB/",
                        "https://www.instagram.com/footsteps_33.6km/p/DVqx5oLEzGx/",
                        "https://www.instagram.com/railichk/p/DG0F40ESgiE/",
                        "https://www.instagram.com/footsteps_33.6km/p/DFHcVvHT_ME/",
                        "https://www.instagram.com/footsteps_33.6km/p/DB0gdbDhSPF/"};

                for (int i = 0; i < images.length; i++) {
                    items.add(new MediaItem(null, titles[i], urls[i], images[i]));
                }
            }

            for (MediaItem item : items) {
                item.bitmap = processBitmap(item, Utils.dpToPx(120));
            }

            new Handler(Looper.getMainLooper()).post(() -> renderCards(inflater, items));
        }).start();
    }

    private void renderCards(LayoutInflater inflater, List<MediaItem> items) {
        if (!isAdded()) return;

        mediaContainer.removeAllViews();

        int width = Utils.dpToPx(120);

        for (int i = 0; i < items.size(); i++) {
            MediaItem item = items.get(i);
            View card = inflater.inflate(R.layout.item_jr_media_card, mediaContainer, false);

            TextView tvRank = card.findViewById(R.id.tv_media_rank);
            TextView tvTitle = card.findViewById(R.id.tv_media_title);
            ImageView iv = card.findViewById(R.id.iv_media_image);

            if (item.title == null) {
                tvRank.setVisibility(View.GONE);
                tvTitle.setVisibility(View.GONE);
                iv.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_placeholder_gradient));
            } else {
                tvRank.setVisibility(View.VISIBLE);
                tvTitle.setVisibility(View.VISIBLE);
                tvRank.setText(String.valueOf(i + 1));
                tvTitle.setText(item.title);

                if (item.bitmap != null) {
                    iv.setImageBitmap(item.bitmap);
                } else if (item.resId != -1) {
                    iv.setImageResource(item.resId);
                }

                card.setOnClickListener(v -> {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.url)));
                });
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, -2);
            params.rightMargin = (i < items.size() - 1) ? Utils.dpToPx(12) : 0;
            card.setLayoutParams(params);

            mediaContainer.addView(card);
        }
    }

    private Bitmap processBitmap(MediaItem item, int targetW) {
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            byte[] data = null;

            if (item.resId != -1) {
                BitmapFactory.decodeResource(getResources(), item.resId, opt);
            } else {
                InputStream is = new URL(item.thumbnail).openStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                data = bos.toByteArray();
                BitmapFactory.decodeByteArray(data, 0, data.length, opt);
            }

            if (opt.outWidth > targetW)
                opt.inSampleSize = Math.round((float) opt.outWidth / targetW);
            else
                opt.inSampleSize = 1;
            opt.inJustDecodeBounds = false;

            if (item.resId != -1)
                return BitmapFactory.decodeResource(getResources(), item.resId, opt);

            return BitmapFactory.decodeByteArray(data, 0, data.length, opt);
        } catch (Exception e) {
            return null;
        }
    }

    private void openExternalApp(String appPackage) {
        PackageManager pm = requireContext().getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(appPackage);

        if (intent != null)
            startActivity(intent);
        else
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackage)));
    }


    private static class MediaItem {
        String thumbnail, title, url;
        int resId;
        Bitmap bitmap;

        MediaItem(String thumbnail, String title, String url, int resId) {
            this.thumbnail = thumbnail;
            this.title = title;
            this.url = url;
            this.resId = resId;
        }
    }
}