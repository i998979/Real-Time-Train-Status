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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_line_selector, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_line_selector);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        Switch openData = view.findViewById(R.id.openData);

        List<LineItem> lines = new ArrayList<>();
        lines.add(new LineItem("EAL", "東鐵綫", "#5eb7e8"));
        lines.add(new LineItem("TML", "屯馬綫", "#9c2e00"));
        lines.add(new LineItem("KTL", "觀塘綫", "#00a040"));
        lines.add(new LineItem("AEL", "機場快綫", "#00888e"));
        lines.add(new LineItem("DRL", "迪士尼綫", "#eb6ea5"));
        lines.add(new LineItem("ISL", "港島綫", "#0075c2"));
        lines.add(new LineItem("TCL", "東涌綫", "#f3982d"));
        lines.add(new LineItem("TKL", "將軍澳綫", "#7e3c93"));
        lines.add(new LineItem("TWL", "荃灣綫", "#e60012"));
        lines.add(new LineItem("SIL", "南港島綫", "#cbd300"));

        LineSelectorAdapter adapter = new LineSelectorAdapter(lines, line -> {
            Intent intent = new Intent(getContext(), EastRailJRActivity.class);
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