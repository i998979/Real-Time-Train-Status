package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RouteDetailFragment extends Fragment {

    private HRConfig hrConf;
    private int startHour, startMin;

    private static class VisualSegment {
        String lineName;
        String colorHex;
        int duration;
        int startH, startM;
        int endH, endM;
        boolean isWalk = false;
        JSONObject startNode;
        JSONObject endNode;
        List<JSONObject> intermediates = new ArrayList<>();
        int lineID;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_route_detail, container, false);

        hrConf = HRConfig.getInstance(getContext());

        LinearLayout routeContainer = root.findViewById(R.id.route_container);
        TextView tvJourneyTime = root.findViewById(R.id.tv_journey_time);
        TextView tvInterhcangeCount = root.findViewById(R.id.tv_interchange_count);
        TextView tvFare = root.findViewById(R.id.tv_fare);

        try {
            JSONObject routeData = new JSONObject(getArguments().getString("route_data"));

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            startHour = calendar.get(Calendar.HOUR_OF_DAY);
            startMin = calendar.get(Calendar.MINUTE);

            // Footer Data
            tvJourneyTime.setText(routeData.optString("time"));
            tvInterhcangeCount.setText(routeData.optString("interchangeStationsNo"));

            JSONArray fares = routeData.optJSONArray("fares");
            if (fares != null && fares.length() > 0) {
                String price = fares.getJSONObject(0).optJSONObject("fareInfo").optJSONObject("adult").optString("octopus", "-");
                tvFare.setText(price);
            }

            List<VisualSegment> segments = parsePathToSegments(routeData.getJSONArray("path"), startHour, startMin);
            buildRouteUI(inflater, routeContainer, segments);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private void buildRouteUI(LayoutInflater inflater, LinearLayout container, List<VisualSegment> segments) {
        container.removeAllViews();

        for (int i = 0; i < segments.size(); i++) {
            VisualSegment seg = segments.get(i);

            // Show Station Section
            addStationSection(inflater, container, seg.startNode);

            // Show Ride/Walk Section
            if (!seg.isWalk)
                addRideSection(inflater, container, seg);
            else
                addWalkSection(inflater, container, seg);

            // Show Destination Station Section
            if (i == segments.size() - 1)
                addStationSection(inflater, container, seg.endNode);
        }
    }

    private void addStationSection(LayoutInflater inflater, LinearLayout container, JSONObject node) {
        View view = inflater.inflate(R.layout.item_route_step, container, false);

        TextView tvStation = view.findViewById(R.id.tv_station);
        tvStation.setText(hrConf.getStationName(node.optInt("ID")));

        container.addView(view);
    }

    private void addRideSection(LayoutInflater inflater, LinearLayout container, VisualSegment seg) {
        View view = inflater.inflate(R.layout.item_segment_ride, container, false);

        // Segment arrival
        TextView tvArvPlat = view.findViewById(R.id.tv_arv_platform);
        String arvPlat = seg.endNode.optString("platform");
        if (!arvPlat.isEmpty() && !arvPlat.equals("null"))
            tvArvPlat.setText(arvPlat + " 號月台");
        else
            tvArvPlat.setVisibility(View.GONE);

        TextView tvArvTime = view.findViewById(R.id.tv_arv_time);
        if (tvArvTime != null)
            tvArvTime.setText(formatTime(seg.endNode.optInt("time")));


        // Segment departure
        TextView tvDepPlat = view.findViewById(R.id.tv_dep_platform);
        String depPlat = seg.startNode.optString("platform");
        if (!depPlat.isEmpty() && !depPlat.equals("null"))
            tvDepPlat.setText(depPlat + " 號月台");
        else
            tvDepPlat.setVisibility(View.GONE);

        TextView tvDepTime = view.findViewById(R.id.tv_dep_time);
        tvDepTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.startH, seg.startM));
        tvDepTime.setVisibility(View.VISIBLE);


        // Segment line color
        View vLineTop = view.findViewById(R.id.v_line_middle);
        View vLineBottom = view.findViewById(R.id.v_line_bottom);
        int color = Color.parseColor("#" + seg.colorHex);

        vLineTop.setBackgroundColor(color);
        GradientDrawable lineBtmDrawable = new GradientDrawable();
        lineBtmDrawable.setShape(GradientDrawable.RECTANGLE);
        float cornerRadiusPx = 5 * getResources().getDisplayMetrics().density;
        lineBtmDrawable.setCornerRadii(new float[]{
                0f, 0f, 0f, 0f,
                cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx
        });
        lineBtmDrawable.setColor(color);
        vLineBottom.setBackground(lineBtmDrawable);

        TextView tvLine = view.findViewById(R.id.tv_line);
        tvLine.setText(seg.lineName);

        TextView tvBadge = view.findViewById(R.id.tv_line_code_badge);
        View lineColorBadge = view.findViewById(R.id.line_color_badge);
        tvBadge.setText(hrConf.getLineById(seg.lineID).alias);
        lineColorBadge.setBackgroundColor(color);

        TextView tvDirection = view.findViewById(R.id.tv_direction);
        String towards = seg.startNode.optString("towards");
        if (!towards.isEmpty()) {
            String[] stationIds = towards.split("/");
            StringBuilder combinedNames = new StringBuilder();

            for (int i = 0; i < stationIds.length; i++) {
                try {
                    int stationId = Integer.parseInt(stationIds[i].trim());
                    String stationName = hrConf.getStationName(stationId);

                    combinedNames.append(stationName);

                    if (i < stationIds.length - 1) combinedNames.append("/");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            tvDirection.setText(combinedNames + " 方面");
        }

        TextView tvCarsCount = view.findViewById(R.id.tv_car_count);
        String alias = hrConf.getLineById(seg.lineID).alias;
        tvCarsCount.setText(alias.equals("EAL") ? "9両" : alias.equals("SIL") ? "3両" : alias.equals("DRL") ? "4両" : "8両");


        // Intermediate station
        MaterialButton btnExpand = view.findViewById(R.id.btn_expand);
        LinearLayout intermediateContainer = view.findViewById(R.id.container_intermediate);

        btnExpand.setText("乘搭 " + (seg.intermediates.size() + 1) + " 個站");

        for (JSONObject stop : seg.intermediates) {
            View intView = inflater.inflate(R.layout.item_intermediate, intermediateContainer, false);
            TextView tvStartTime = intView.findViewById(R.id.tv_start_time);
            TextView tvMidStation = intView.findViewById(R.id.tv_mid_station);
            View vLineMiddle = intView.findViewById(R.id.v_line_middle);

            tvStartTime.setText(formatTime(stop.optInt("time")));
            tvMidStation.setText(hrConf.getStationName(stop.optInt("ID")));
            vLineMiddle.setBackgroundColor(color);

            intermediateContainer.addView(intView);
        }

        if (!seg.intermediates.isEmpty()) {
            btnExpand.setOnClickListener(v -> {
                boolean isVisible = intermediateContainer.getVisibility() == View.VISIBLE;

                intermediateContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (isVisible)
                    btnExpand.setIconResource(R.drawable.baseline_keyboard_arrow_down_24);
                else
                    btnExpand.setIconResource(R.drawable.baseline_keyboard_arrow_up_24);
            });
        } else {
            btnExpand.setIcon(null);
            btnExpand.setClickable(false);
        }

        container.addView(view);
    }

    private void addWalkSection(LayoutInflater inflater, LinearLayout container, VisualSegment seg) {
        View walkView = inflater.inflate(R.layout.item_walkinterchange, container, false);

        TextView tvWalkIntTime = walkView.findViewById(R.id.tv_walkinterchange_time);
        TextView tvStartTime = walkView.findViewById(R.id.tv_start_time);
        TextView tvArriveTime = walkView.findViewById(R.id.tv_end_time);

        tvWalkIntTime.setText(seg.duration + " 分");
        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.startH, seg.startM));
        tvArriveTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.endH, seg.endM));

        container.addView(walkView);
    }


    private List<VisualSegment> parsePathToSegments(JSONArray path, int startH, int startM) {
        List<VisualSegment> segments = new ArrayList<>();
        if (path == null || path.length() < 2) return segments;

        int i = 0;
        while (i < path.length() - 1) {
            JSONObject startNode = path.optJSONObject(i);
            String startCat = startNode.optString("linkType").equals("WALKINTERCHANGE") ? "WALK" : "RIDE";
            int startLineID = startNode.optInt("lineID");

            // 尋找這一段的終點
            int j = i;
            while (j < path.length() - 1) {
                JSONObject curr = path.optJSONObject(j);
                JSONObject next = path.optJSONObject(j + 1);

                String currCat = curr.optString("linkType").equals("WALKINTERCHANGE") ? "WALK" : "RIDE";
                int currLineID = curr.optInt("lineID");

                if (!startCat.equals(currCat) || (startCat.equals("RIDE") && currLineID != startLineID))
                    break;

                j++;
            }

            segments.add(createSegmentFromRange(path, i, j, startH, startM));

            i = j;

            if (i < path.length() - 1) {
                JSONObject currentEnd = path.optJSONObject(j);
                JSONObject nextStart = path.optJSONObject(j + 1);
                if (currentEnd.optInt("ID") == nextStart.optInt("ID")) {
                    i = j + 1;
                }
            }
        }

        return segments;
    }

    private VisualSegment createSegmentFromRange(JSONArray path, int startIdx, int endIdx, int baseH, int baseM) {
        JSONObject startNode = path.optJSONObject(startIdx);
        JSONObject endNode = path.optJSONObject(endIdx);

        String cat = startNode.optString("linkType").equals("WALKINTERCHANGE") ? "WALK" : "RIDE";
        int lineID = startNode.optInt("lineID");
        int duration = endNode.optInt("time") - startNode.optInt("time");

        List<JSONObject> inters = new ArrayList<>();
        for (int k = startIdx + 1; k < endIdx; k++) {
            inters.add(path.optJSONObject(k));
        }

        VisualSegment seg = createSeg(cat, lineID, duration, baseH, baseM + startNode.optInt("time"), startNode, endNode, inters);

        int endTotalM = (baseH * 60) + baseM + endNode.optInt("time");
        seg.endH = (endTotalM / 60) % 24;
        seg.endM = endTotalM % 60;

        return seg;
    }

    private VisualSegment createSeg(String cat, int id, int dur, int h, int m, JSONObject start, JSONObject end, List<JSONObject> inter) {
        VisualSegment seg = new VisualSegment();
        seg.lineID = id;
        seg.duration = dur;
        int totalM = (h * 60) + m;
        seg.startH = (totalM / 60) % 24;
        seg.startM = totalM % 60;
        seg.isWalk = "WALK".equals(cat);
        seg.startNode = start;
        seg.endNode = end;
        seg.intermediates = new ArrayList<>(inter);
        HRConfig.Line line = hrConf.getLineById(id);
        seg.lineName = (line != null) ? line.name : (seg.isWalk ? "步行" : "MTR");
        seg.colorHex = (line != null) ? line.color : "888888";
        return seg;
    }

    private String formatTime(int minutesOffset) {
        int totalMin = startMin + minutesOffset;
        return String.format(Locale.getDefault(), "%02d:%02d", (startHour + totalMin / 60) % 24, totalMin % 60);
    }
}