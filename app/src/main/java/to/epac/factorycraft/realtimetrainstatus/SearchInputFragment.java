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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

public class SearchInputFragment extends Fragment {
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

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(MainActivity.KEY_WALK_SPEED))
            prefs.edit().putString(MainActivity.KEY_WALK_SPEED, "普通").apply();
        if (!prefs.contains(MainActivity.KEY_FARE_TYPE))
            prefs.edit().putString(MainActivity.KEY_FARE_TYPE, "octopus").apply();
        if (!prefs.contains(MainActivity.KEY_TICKET_TYPE))
            prefs.edit().putString(MainActivity.KEY_TICKET_TYPE, "adult").apply();

        selectedOriginID = prefs.getString(MainActivity.KEY_ORIGIN_ID, null);
        selectedDestID = prefs.getString(MainActivity.KEY_DEST_ID, null);

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

        int[] btnIds = {R.id.btn_option_walk, R.id.btn_option_fare};
        int[] iconRes = {R.drawable.baseline_directions_walk_24, R.drawable.baseline_credit_card_24};
        int[] colors = {Color.parseColor("#6ec08d"), Color.parseColor("#6ec08d")};
        int[] layoutIds = {R.layout.layout_walk_settings, R.layout.layout_fare_settings};

        for (int i = 0; i < btnIds.length; i++) {
            final int index = i;
            MaterialButton settingsBtn = view.findViewById(btnIds[i]);

            String currentStatus = "";
            switch (index) {
                case 0:
                    currentStatus = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");
                    break;
                case 1:
                    currentStatus = "車票設定";
                    break;
            }
            renderOptionButton(settingsBtn, currentStatus, colors[index], iconRes[index]);

            BottomSheetDialog bottomSheet = new BottomSheetDialog(getContext());
            bottomSheet.setContentView(layoutIds[index]);

            settingsBtn.setOnClickListener(v -> {
                if (index == 0) {
                    MaterialButton closeBtn = bottomSheet.findViewById(R.id.btn_close);
                    closeBtn.setOnClickListener(v1 -> {
                        bottomSheet.dismiss();
                    });

                    RadioGroup rg = bottomSheet.findViewById(R.id.rg_walk_speed);

                    String saved = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");


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
                                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, rawText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                                .putString(MainActivity.KEY_WALK_SPEED, selected)
                                .apply();
                        renderOptionButton(settingsBtn, selected, colors[index], iconRes[index]);
                    });
                }
                if (index == 1) {
                    MaterialButton closeBtn = bottomSheet.findViewById(R.id.btn_close);
                    closeBtn.setOnClickListener(v1 -> {
                        bottomSheet.dismiss();
                    });

                    RadioGroup rgTicketType = bottomSheet.findViewById(R.id.rg_ticket_type);
                    RadioGroup rgFareType = bottomSheet.findViewById(R.id.rg_fare_type);

                    String ticketType = prefs.getString(MainActivity.KEY_TICKET_TYPE, "octopus");
                    String fareType = prefs.getString(MainActivity.KEY_FARE_TYPE, "adult");

                    int fareRbId = R.id.rb_adult;
                    if (fareType.equals("adult"))
                        fareRbId = R.id.rb_adult;
                    else if (fareType.equals("concessionchild"))
                        fareRbId = R.id.rb_concessionchild;
                    else if (fareType.equals("concessionchild2"))
                        fareRbId = R.id.rb_concessionchild2;
                    else if (fareType.equals("concessionelderly"))
                        fareRbId = R.id.rb_concessionelderly;
                    else if (fareType.equals("joyyousixty"))
                        fareRbId = R.id.rb_joyyousixty;
                    else if (fareType.equals("concessionpwd"))
                        fareRbId = R.id.rb_concessionpwd;
                    else if (fareType.equals("student"))
                        fareRbId = R.id.rb_student;

                    rgTicketType.check(ticketType.equals("sj") ? R.id.rb_sj : R.id.rb_octopus);
                    rgFareType.check(fareRbId);

                    Runnable updateFareVisibility = () -> {
                        boolean isSJ = rgTicketType.getCheckedRadioButtonId() == R.id.rb_sj;

                        int[] octopusOnly = {R.id.rb_concessionelderly, R.id.rb_joyyousixty, R.id.rb_concessionpwd, R.id.rb_student};
                        for (int id : octopusOnly) {
                            View rb = rgFareType.findViewById(id);
                            rb.setVisibility(isSJ ? View.GONE : View.VISIBLE);

                            if (isSJ && rgFareType.getCheckedRadioButtonId() == id)
                                rgFareType.check(R.id.rb_adult);
                        }
                    };

                    rgTicketType.setOnCheckedChangeListener((group, checkedId) -> {
                        updateFareVisibility.run();
                        saveFareState(rgTicketType, rgFareType);
                    });
                    rgFareType.setOnCheckedChangeListener((group, checkedId) -> {
                        saveFareState(rgTicketType, rgFareType);
                    });

                    updateFareVisibility.run();
                }

                bottomSheet.show();

                View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheetInternal != null) {
                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);

                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            });
        }

        return view;
    }

    private void saveFareState(RadioGroup rgTicketType, RadioGroup rgFareType) {
        int ticketId = rgTicketType.getCheckedRadioButtonId();
        int fareId = rgFareType.getCheckedRadioButtonId();

        String ticketType = (ticketId == R.id.rb_sj) ? "sj" : "octopus";

        String fareType = "adult";
        if (fareId == R.id.rb_adult) fareType = "adult";
        else if (fareId == R.id.rb_concessionchild) fareType = "concessionchild";
        else if (fareId == R.id.rb_concessionchild2) fareType = "concessionchild2";
        else if (fareId == R.id.rb_concessionelderly) fareType = "concessionelderly";
        else if (fareId == R.id.rb_joyyousixty) fareType = "joyyousixty";
        else if (fareId == R.id.rb_concessionpwd) fareType = "concessionpwd";
        else if (fareId == R.id.rb_student) fareType = "student";

        prefs.edit()
                .putString(MainActivity.KEY_TICKET_TYPE, ticketType)
                .putString(MainActivity.KEY_FARE_TYPE, fareType)
                .apply();
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
                .putString(MainActivity.KEY_ORIGIN_ID, selectedOriginID)
                .putString(MainActivity.KEY_DEST_ID, selectedDestID)
                .apply();
    }

    private void renderOptionButton(MaterialButton btn, String status, int color, int iconRes) {
        String firstLine = " ";
        String fullText = firstLine + "\n" + status;

        SpannableString spannable = new SpannableString(fullText);

        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon != null) {
            icon.setBounds(0, 0, dpToPx(18), dpToPx(18));
            icon.setTint(color);
            spannable.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_CENTER), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(1.0f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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