package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LineSelectorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_selector);

        RecyclerView rv = findViewById(R.id.rv_line_selector);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Switch openData = findViewById(R.id.openData);

        List<LineItem> lines = new ArrayList<>();
        // 添加東鐵綫
        lines.add(new LineItem("EAL", "東鐵綫", "#5DE2FF"));
        // 添加屯馬綫
        lines.add(new LineItem("TML", "屯馬綫", "#9A3848"));

        LineSelectorAdapter adapter = new LineSelectorAdapter(lines, line -> {
            Intent intent = new Intent(this, EastRailJRActivity.class);
            intent.putExtra("LINE_CODE", line.code.toLowerCase());
            intent.putExtra("DATA_SOURCE", openData.isChecked() ? "OPENDATA" : "ROCTEC");
            startActivity(intent);
        });

        rv.setAdapter(adapter);
    }

    // 簡單的資料模型
    public static class LineItem {
        String code, name, color;
        LineItem(String code, String name, String color) {
            this.code = code; this.name = name; this.color = color;
        }
    }
}