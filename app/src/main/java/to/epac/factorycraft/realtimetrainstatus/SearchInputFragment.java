package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.material.card.MaterialCardView;

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
        if (!prefs.contains(MainActivity.KEY_TICKET_TYPE))
            prefs.edit().putString(MainActivity.KEY_TICKET_TYPE, "octopus").apply();
        if (!prefs.contains(MainActivity.KEY_FARE_TYPE))
            prefs.edit().putString(MainActivity.KEY_FARE_TYPE, "adult").apply();

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
                        int id = result.getData().getIntExtra("selected_id", 1);
                        String name = result.getData().getStringExtra("selected_name");
                        String code = result.getData().getStringExtra("selected_code");

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
            searchLauncher.launch(new Intent(requireContext(), SearchActivity.class));
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
        int[] iconRes = {R.drawable.baseline_directions_walk_24, R.drawable.outline_payment_card_24};
        int greenColor = ContextCompat.getColor(requireContext(), R.color.button_green);
        int[] layoutIds = {R.layout.layout_walk_settings, R.layout.layout_fare_settings};

        for (int i = 0; i < btnIds.length; i++) {
            final int index = i;
            MaterialButton settingsBtn = view.findViewById(btnIds[i]);

            String currentStatus = "";
            if (index == 0) {
                currentStatus = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");
            } else {
                currentStatus = "車票設定";
            }
            renderOptionButton(settingsBtn, currentStatus, greenColor, iconRes[index]);

            BottomSheetDialog bottomSheet = new BottomSheetDialog(getContext());
            bottomSheet.setContentView(layoutIds[index]);
            View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);

            settingsBtn.setOnClickListener(v -> {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                MaterialButton closeBtn = bottomSheet.findViewById(R.id.btn_close);
                closeBtn.setOnClickListener(v1 -> {
                    bottomSheet.dismiss();
                });

                Runnable refreshUI = () -> {
                    if (index == 0) {
                        String saved = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");
                        int[] ids = {R.id.rb_fast, R.id.rb_normal, R.id.rb_slow, R.id.rb_veryslow};
                        String[] vals = {"快速", "普通", "慢速", "很慢"};

                        for (int j = 0; j < ids.length; j++) {
                            updateCard(bottomSheet.findViewById(ids[j]), vals[j].equals(saved));
                        }
                    } else if (index == 1) {
                        String curT = prefs.getString(MainActivity.KEY_TICKET_TYPE, "octopus");
                        String curF = prefs.getString(MainActivity.KEY_FARE_TYPE, "adult");

                        if (curT.equals("sj") && (curF.equals("concessionelderly") || curF.equals("joyyousixty") || curF.equals("concessionpwd") || curF.equals("student"))) {
                            curF = "adult";
                            prefs.edit().putString(MainActivity.KEY_FARE_TYPE, curF).apply();
                        }

                        updateCard(bottomSheet.findViewById(R.id.rb_octopus), curT.equals("octopus"));
                        updateCard(bottomSheet.findViewById(R.id.rb_sj), curT.equals("sj"));

                        int[] fIds = {R.id.rb_adult, R.id.rb_concessionchild, R.id.rb_concessionchild2, R.id.rb_concessionelderly, R.id.rb_joyyousixty, R.id.rb_concessionpwd, R.id.rb_student};
                        String[] fVals = {"adult", "concessionchild", "concessionchild2", "concessionelderly", "joyyousixty", "concessionpwd", "student"};
                        for (int j = 0; j < fIds.length; j++) {
                            MaterialCardView card = bottomSheet.findViewById(fIds[j]);

                            card.setVisibility(curT.equals("sj") && j >= 3 ? View.GONE : View.VISIBLE);
                            updateCard(card, fVals[j].equals(curF));
                        }
                    }
                };

                View.OnClickListener listener = cv -> {
                    if (index == 0) {
                        String[] vals = {"快速", "普通", "慢速", "很慢"};
                        int[] ids = {R.id.rb_fast, R.id.rb_normal, R.id.rb_slow, R.id.rb_veryslow};
                        for (int j = 0; j < ids.length; j++) {
                            if (cv.getId() == ids[j]) {
                                prefs.edit().putString(MainActivity.KEY_WALK_SPEED, vals[j]).apply();
                                renderOptionButton(settingsBtn, vals[j], greenColor, iconRes[index]);
                            }
                        }
                    } else if (index == 1) {
                        if (cv.getId() == R.id.rb_octopus)
                            prefs.edit().putString(MainActivity.KEY_TICKET_TYPE, "octopus").apply();
                        else if (cv.getId() == R.id.rb_sj)
                            prefs.edit().putString(MainActivity.KEY_TICKET_TYPE, "sj").apply();
                        else {
                            String[] fVals = {"adult", "concessionchild", "concessionchild2", "concessionelderly", "joyyousixty", "concessionpwd", "student"};
                            int[] fIds = {R.id.rb_adult, R.id.rb_concessionchild, R.id.rb_concessionchild2, R.id.rb_concessionelderly, R.id.rb_joyyousixty, R.id.rb_concessionpwd, R.id.rb_student};
                            for (int j = 0; j < fIds.length; j++)
                                if (cv.getId() == fIds[j])
                                    prefs.edit().putString(MainActivity.KEY_FARE_TYPE, fVals[j]).apply();
                        }
                    }
                    refreshUI.run();
                };

                int[] ids = {R.id.rb_fast, R.id.rb_normal, R.id.rb_slow, R.id.rb_veryslow, R.id.rb_octopus, R.id.rb_sj, R.id.rb_adult, R.id.rb_concessionchild, R.id.rb_concessionchild2, R.id.rb_concessionelderly, R.id.rb_joyyousixty, R.id.rb_concessionpwd, R.id.rb_student};
                for (int id : ids) {
                    View card = bottomSheet.findViewById(id);
                    if (card != null) card.setOnClickListener(listener);
                }

                refreshUI.run();
                bottomSheet.show();
            });
        }

        return view;
    }

    private void updateCard(View card, boolean selected) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
        int colorOnSurface = tv.data;

        ImageView iv = card.findViewWithTag("iv_check_mark");
        iv.setImageResource(selected ? R.drawable.baseline_check_circle_outline_24 : R.drawable.outline_circle_24);
        iv.setImageTintList(ColorStateList.valueOf(selected ? ContextCompat.getColor(requireContext(), R.color.button_green) : colorOnSurface));
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

        int activeColor = ContextCompat.getColor(requireContext(), R.color.button_green);
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

        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes).mutate();
        icon.setBounds(0, 0, Utils.dpToPx(requireContext(), 18), Utils.dpToPx(requireContext(), 18));
        icon.setTint(color);
        spannable.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_CENTER), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.0f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int startStatus = fullText.indexOf(status);
        if (startStatus != -1) {
            spannable.setSpan(new ForegroundColorSpan(Color.GRAY), startStatus, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.8f), startStatus, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        btn.setText(spannable);
    }
}