package to.epac.factorycraft.transitapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TermsActivity extends AppCompatActivity {

    private MaterialButton btnBack;
    private MaterialButton btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
        });
        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });
    }
}