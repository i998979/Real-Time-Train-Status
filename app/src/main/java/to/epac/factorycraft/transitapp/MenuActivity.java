package to.epac.factorycraft.transitapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class MenuActivity extends AppCompatActivity {

    private LinearLayout menuLayout;
    private MaterialButton btnClose;
    private LinearLayout btnFeedback;
    private LinearLayout btnTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        menuLayout = findViewById(R.id.layout_menu);
        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });
        btnFeedback = findViewById(R.id.btn_feedback);
        btnTerms = findViewById(R.id.btn_terms);
        btnTerms.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TermsActivity.class);
            v.getContext().startActivity(intent);
        });

        btnFeedback.setOnClickListener(v -> {
            showSnackBar(menuLayout, Color.parseColor("#58A473"), "感謝花時間告訴我們你的想法。");
        });
    }


    private void showSnackBar(View anchor, int color, String message) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        snackbarView.setBackgroundTintList(ColorStateList.valueOf(color));
        // snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }
}