package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public LinearLayout mainLayout;
    public TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout);
        tableLayout = findViewById(R.id.tableLayout);
    }
}