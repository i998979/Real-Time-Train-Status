package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LineSelectorFragment extends Fragment {

    public static final String ARG_ALLOWED_LINES = "allowed_lines";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_line_selector, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_line_selector);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        Switch openData = view.findViewById(R.id.open_data);

        List<LineItem> allLines = new ArrayList<>();
        allLines.add(new LineItem("EAL", "東鐵綫", "#5eb7e8"));
        allLines.add(new LineItem("TML", "屯馬綫", "#9c2e00"));
        allLines.add(new LineItem("KTL", "觀塘綫", "#00a040"));
        allLines.add(new LineItem("AEL", "機場快綫", "#00888e"));
        allLines.add(new LineItem("DRL", "迪士尼綫", "#eb6ea5"));
        allLines.add(new LineItem("ISL", "港島綫", "#0075c2"));
        allLines.add(new LineItem("TCL", "東涌綫", "#f3982d"));
        allLines.add(new LineItem("TKL", "將軍澳綫", "#7e3c93"));
        allLines.add(new LineItem("TWL", "荃灣綫", "#e60012"));
        allLines.add(new LineItem("SIL", "南港島綫", "#cbd300"));

        List<String> allowed = getArguments() != null ? getArguments().getStringArrayList(ARG_ALLOWED_LINES) : null;

        List<LineItem> display = new ArrayList<>();
        if (allowed == null || allowed.isEmpty()) {
            display.addAll(allLines);
        } else {
            for (LineItem item : allLines) {
                for (String allowedCode : allowed) {
                    if (item.code.equalsIgnoreCase(allowedCode)) {
                        display.add(item);
                        break;
                    }
                }
            }
        }

        LineSelectorAdapter adapter = new LineSelectorAdapter(display, line -> {
            Intent intent = new Intent(getContext(), TrainLocationActivity.class);
            intent.putExtra("LINE_CODE", line.code.toLowerCase());
            intent.putExtra("DATA_SOURCE", openData.isChecked() ? "OPENDATA" : "ROCTEC");
            startActivity(intent);
        });

        rv.setAdapter(adapter);
        return view;
    }


    public static class LineItem {
        String code, name, color;

        LineItem(String code, String name, String color) {
            this.code = code;
            this.name = name;
            this.color = color;
        }
    }
}