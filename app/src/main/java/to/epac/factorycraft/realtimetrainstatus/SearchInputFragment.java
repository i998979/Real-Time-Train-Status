package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

public class SearchInputFragment extends Fragment {
    private static final String PREF_NAME = "route_prefs";
    private static final String KEY_ORIGIN_ID = "origin_id";
    private static final String KEY_DEST_ID = "dest_id";

    private static final String KEY_RT = "is_rt_enabled";
    private static final String KEY_WALK_SPEED = "walk_speed";
    private static final String KEY_FARE_TYPE = "fare_type";
    private SharedPreferences prefs;

    private View layoutOrigin;
    private TextView tvOrigin;

    private MaterialButton btnSwap;

    private View layoutDest;
    private TextView tvDest;

    private Button btnGo;

    private String selectedOriginID = null;
    private String selectedDestID = null;
    private boolean isSelectingOrigin = true;

    private ActivityResultLauncher<Intent> searchLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_input, container, false);

        prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        selectedOriginID = prefs.getString(KEY_ORIGIN_ID, null);
        selectedDestID = prefs.getString(KEY_DEST_ID, null);

        layoutOrigin = view.findViewById(R.id.layout_origin);
        tvOrigin = view.findViewById(R.id.tv_origin_name);
        layoutDest = view.findViewById(R.id.layout_dest);
        tvDest = view.findViewById(R.id.tv_dest_name);

        btnSwap = view.findViewById(R.id.btn_swap);
        btnSwap.setOnClickListener(v -> {
            String tempID = selectedOriginID;
            selectedOriginID = selectedDestID;
            selectedDestID = tempID;

            updateStationDisplay(tvOrigin, selectedOriginID, "出發地");
            updateStationDisplay(tvDest, selectedDestID, "目的地");
            updateButtonStates();
        });

        searchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        int id = result.getData().getIntExtra("selected_station_id", 1);
                        String name = result.getData().getStringExtra("selected_station_name");

                        if (isSelectingOrigin) {
                            selectedOriginID = String.valueOf(id);
                            tvOrigin.setText(name);
                        } else {
                            selectedDestID = String.valueOf(id);
                            tvDest.setText(name);
                        }
                        updateButtonStates();
                    }
                }
        );

        View.OnClickListener searchClick = v -> {
            isSelectingOrigin = (v.getId() == R.id.layout_origin);
            searchLauncher.launch(new Intent(requireContext(), StationSearchActivity.class));
        };
        layoutOrigin.setOnClickListener(searchClick);
        layoutDest.setOnClickListener(searchClick);

        btnGo = view.findViewById(R.id.btn_go);
        btnGo.setOnClickListener(v -> {
            HistoryManager.getInstance(requireContext()).saveRouteSearch(selectedOriginID, selectedDestID, tvOrigin.getText().toString(), tvDest.getText().toString());

            RouteListFragment listFragment = new RouteListFragment();
            Bundle bundle = new Bundle();
            bundle.putString("o", selectedOriginID);
            bundle.putString("d", selectedDestID);
            listFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.main_container, listFragment)
                    .addToBackStack("LIST_PAGE")
                    .commit();
        });

        updateStationDisplay(tvOrigin, selectedOriginID, "出發地");
        updateStationDisplay(tvDest, selectedDestID, "目的地");
        updateButtonStates();

        int[] btnIds = {R.id.btn_option_rt, R.id.btn_option_walk, R.id.btn_option_fare};
        int[] iconRes = {R.drawable.baseline_browse_gallery_24, R.drawable.baseline_directions_walk_24, R.drawable.baseline_credit_card_24};
        int[] colors = {Color.parseColor("#6ec08d"), Color.parseColor("#6ec08d"), Color.parseColor("#6ec08d")};
        int[] layoutIds = {R.layout.layout_rt_settings, R.layout.layout_walk_settings, R.layout.layout_fare_settings};

        for (int i = 0; i < btnIds.length; i++) {
            final int index = i;
            MaterialButton settingsBtn = view.findViewById(btnIds[i]);

            String currentStatus = "";
            switch (index) {
                case 0:
                    currentStatus = prefs.getBoolean(KEY_RT, false) ? "開啟" : "關閉";
                    break;
                case 1:
                    currentStatus = prefs.getString(KEY_WALK_SPEED, "普通");
                    break;
                case 2:
                    currentStatus = "車票設定";
                    break;
            }
            renderOptionButton(settingsBtn, index, currentStatus, colors[index], iconRes[index]);

            BottomSheetDialog bottomSheet = new BottomSheetDialog(getContext());
            bottomSheet.setContentView(layoutIds[index]);

            settingsBtn.setOnClickListener(v -> {
                if (index == 1) {
                    MaterialButton closeBtn = bottomSheet.findViewById(R.id.btn_close);
                    closeBtn.setOnClickListener(v1 -> {
                        bottomSheet.dismiss();
                    });

                    RadioGroup rg = bottomSheet.findViewById(R.id.rg_walk_speed);

                    String saved = prefs.getString(KEY_WALK_SPEED, "普通");


                    for (int j = 0; j < rg.getChildCount(); j++) {
                        View child = rg.getChildAt(j);

                        if (child instanceof MaterialRadioButton) {
                            MaterialRadioButton rb = (MaterialRadioButton) child;

                            // Reformat text with smaller and gray 2nd line
                            String rawText = rb.getText().toString();
                            String title = rawText.split("\n")[0];

                            int lineBreak = rawText.indexOf("\n");
                            if (lineBreak != -1) {
                                SpannableString ss = new SpannableString(rawText);
                                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, lineBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                ss.setSpan(new ForegroundColorSpan(Color.GRAY), lineBreak + 1, rawText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                ss.setSpan(new RelativeSizeSpan(0.8f), lineBreak + 1, rawText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                rb.setText(ss);
                            }

                            if (title.equals(saved)) rg.check(rb.getId());
                        }
                    }

                    rg.setOnCheckedChangeListener((group, checkedId) -> {
                        MaterialRadioButton rb = group.findViewById(checkedId);

                        String selected = rb.getText().toString().split("\n")[0];
                        prefs.edit()
                                .putString(KEY_WALK_SPEED, selected)
                                .apply();
                        renderOptionButton(settingsBtn, index, selected, colors[index], iconRes[index]);
                    });
                }

                bottomSheet.show();
            });
        }

        return view;
    }

    private void updateStationDisplay(TextView tv, String id, String hint) {
        if (id == null) {
            tv.setText(hint);
            return;
        }
        HRConfig.Station s = HRConfig.getInstance(requireContext()).getStationById(Integer.parseInt(id));
        tv.setText(s != null ? s.name : hint);
    }

    private void updateButtonStates() {
        boolean canSwap = selectedOriginID != null || selectedDestID != null;
        btnSwap.setEnabled(canSwap);
        btnSwap.setAlpha(canSwap ? 1.0f : 0.5f);

        boolean canGo = selectedOriginID != null && selectedDestID != null;
        btnGo.setAlpha(canGo ? 1.0f : 0.5f);
        btnGo.setEnabled(canGo);

        int activeColor = Color.parseColor("#6EC08D");
        int greyColor = Color.parseColor("#BDBDBD");
        btnGo.setBackgroundTintList(ColorStateList.valueOf(canGo ? activeColor : greyColor));

        prefs.edit()
                .putString(KEY_ORIGIN_ID, selectedOriginID)
                .putString(KEY_DEST_ID, selectedDestID)
                .apply();
    }

    private void renderOptionButton(MaterialButton btn, int index, String status, int color, int iconRes) {
        String[] currentTitles = {" RT", "", ""};
        String title = currentTitles[index];
        boolean hasTitle = !title.isEmpty();

        String firstLine = hasTitle ? " " + title : " ";
        String fullText = firstLine + "\n" + status;

        SpannableString spannable = new SpannableString(fullText);

        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon != null) {
            icon.setBounds(0, 0, dpToPx(18), dpToPx(18));
            icon.setTint(color);
            spannable.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_CENTER), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(hasTitle ? 0.4f : 1.0f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (hasTitle) {
            spannable.setSpan(new ForegroundColorSpan(color), 1, firstLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 1, firstLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int startStatus = fullText.indexOf(status);
        if (startStatus != -1) {
            spannable.setSpan(new ForegroundColorSpan(Color.GRAY), startStatus, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.8f), startStatus, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        btn.setText(spannable);
    }

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }
}