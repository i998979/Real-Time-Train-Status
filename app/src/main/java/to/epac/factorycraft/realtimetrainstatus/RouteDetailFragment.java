package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RouteDetailFragment extends Fragment {

    private HRConfig hrConf;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_route_detail, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.KEY_FARE_TYPE, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(getContext());

        MaterialButton btnReturn = root.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        TextView tvStartTime = root.findViewById(R.id.tv_start_time);
        TextView tvEndTime = root.findViewById(R.id.tv_end_time);
        TextView journeyStart = root.findViewById(R.id.tv_journey_start);

        LinearLayout routeContainer = root.findViewById(R.id.route_container);
        TextView tvJourneyTime = root.findViewById(R.id.tv_journey_time);
        TextView tvInterchangeCount = root.findViewById(R.id.tv_interchange_count);
        TextView tvFare = root.findViewById(R.id.tv_fare);

        try {
            JSONObject data = new JSONObject(getArguments().getString("route_data"));
            JSONObject selectedRoute = data.getJSONArray("routes").getJSONObject(getArguments().getInt("selected_route"));


            // Start and end time calculation
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            int nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

            String firstTimeStr = data.getJSONObject("firstTrain").getString("time");
            String lastTimeStr = data.getJSONObject("lastTrain").getString("time");
            int firstMins = Integer.parseInt(firstTimeStr.split(":")[0]) * 60 + Integer.parseInt(firstTimeStr.split(":")[1]);
            int lastMins = Integer.parseInt(lastTimeStr.split(":")[0]) * 60 + Integer.parseInt(lastTimeStr.split(":")[1]);

            if (lastMins < firstMins) lastMins += 24 * 60;

            boolean isTomorrow = (nowMins > lastMins % 1440 && nowMins < firstMins);
            if (isTomorrow)
                cal.add(Calendar.DAY_OF_YEAR, 1);

            SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E)", Locale.TRADITIONAL_CHINESE);
            journeyStart.setText(sdf.format(cal.getTime()));

            String startTime = getArguments().getString("start_time");
            int startH = Integer.parseInt(startTime.split(":")[0]);
            int startM = Integer.parseInt(startTime.split(":")[1]);

            tvStartTime.setText(startTime);
            JSONArray path = selectedRoute.getJSONArray("path");
            int endTotalMin = startM + path.getJSONObject(path.length() - 1).optInt("time");
            int endH = (startH + endTotalMin / 60) % 24;
            int endM = endTotalMin % 60;
            tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endH, endM));


            // Footer Data
            tvJourneyTime.setText(selectedRoute.optString("time"));
            tvInterchangeCount.setText(selectedRoute.optString("interchangeStationsNo"));
            tvFare.setText(getFare(selectedRoute) + "");


            // Apply path segments
            List<VisualSegment> segments = parsePathToSegments(selectedRoute.getJSONArray("path"), startH, startM);
            buildRouteUI(inflater, routeContainer, segments, startH, startM);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private void buildRouteUI(LayoutInflater inflater, LinearLayout container, List<VisualSegment> segments, int startH, int startM) {
        container.removeAllViews();

        for (int i = 0; i < segments.size(); i++) {
            VisualSegment seg = segments.get(i);

            // Show Station Section
            addStationSection(inflater, container, seg.startNode);

            // Show Ride/Walk Section
            if (!seg.isWalk)
                addRideSection(inflater, container, seg, startH, startM);
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

    private void addRideSection(LayoutInflater inflater, LinearLayout container, VisualSegment seg, int startH, int startM) {
        View view = inflater.inflate(R.layout.item_segment_ride, container, false);

        // Segment arrival
        TextView tvArvPlat = view.findViewById(R.id.tv_arv_platform);
        String arvPlat = seg.endNode.optString("platform");
        if (!arvPlat.isEmpty() && !arvPlat.equals("null") && !seg.endNode.optString("linkType").equals("INTERCHANGE"))
            tvArvPlat.setText(arvPlat + " 號月台");
        else
            tvArvPlat.setVisibility(View.GONE);

        TextView tvArvTime = view.findViewById(R.id.tv_arv_time);
        int totalArvMin = startM + seg.endNode.optInt("time");
        tvArvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", (startH + totalArvMin / 60) % 24, totalArvMin % 60));


        // Segment departure
        TextView tvDepPlat = view.findViewById(R.id.tv_dep_platform);
        String depPlat = seg.startNode.optString("platform");
        if (!depPlat.isEmpty() && !depPlat.equals("null"))
            tvDepPlat.setText(depPlat + " 號月台");
        else
            tvDepPlat.setVisibility(View.GONE);

        TextView tvDepTime = view.findViewById(R.id.tv_dep_time);
        TextView tvDeparture = view.findViewById(R.id.tv_departure);
        tvDepTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.startH, seg.startM));

        int startStationId = seg.startNode.optInt("ID");
        String lineAlias = hrConf.getLineById(seg.lineID).alias;
        tvDeparture.setVisibility(hrConf.isTerminus(lineAlias, startStationId) ? View.VISIBLE : View.GONE);

        // Segment line color
        View vLineTop = view.findViewById(R.id.v_line_middle);
        View vLineBottom = view.findViewById(R.id.v_line_bottom);
        int color = Color.parseColor("#" + seg.lineColor);

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
            int totalIntMin = startM + stop.optInt("time");
            tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", (startH + totalIntMin / 60) % 24, totalIntMin % 60));
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
            boolean isWalk = startNode.optString("linkType").equals("WALKINTERCHANGE");
            int startLineID = startNode.optInt("lineID");

            // Find segment end
            int j = i;
            while (j < path.length() - 1) {
                JSONObject currNode = path.optJSONObject(j);
                boolean currIsWalk = currNode.optString("linkType").equals("WALKINTERCHANGE");
                int currLineID = currNode.optInt("lineID");

                // If linkType change, segment ends
                if (isWalk != currIsWalk || (!isWalk && currLineID != startLineID)) {
                    break;
                }
                j++;
            }

            // Construct VisualSegment
            VisualSegment seg = new VisualSegment();
            JSONObject endNode = path.optJSONObject(j);

            seg.startNode = startNode;
            seg.endNode = endNode;
            seg.isWalk = isWalk;
            seg.lineID = startLineID;

            // Calculate start end time and duration
            int startTime = startNode.optInt("time");
            int endTime = endNode.optInt("time");
            seg.duration = endTime - startTime;

            int startTotalM = (startH * 60) + startM + startTime;
            seg.startH = (startTotalM / 60) % 24;
            seg.startM = startTotalM % 60;

            int endTotalM = (startH * 60) + startM + endTime;
            seg.endH = (endTotalM / 60) % 24;
            seg.endM = endTotalM % 60;

            // Apply line name and color
            seg.lineName = hrConf.getLineById(startLineID).name;
            seg.lineColor = hrConf.getLineById(startLineID).color;
            seg.stationName = hrConf.getStationName(endNode.optInt("ID"));

            // Add intermediate stations
            for (int k = i + 1; k < j; k++) {
                seg.intermediates.add(path.optJSONObject(k));
            }

            segments.add(seg);

            // Set next segment start
            if (j < path.length() - 1)
                i = j;
            else
                i = j + 1;
        }

        return segments;
    }

    private float getFare(JSONObject route) throws Exception {
        String fareType = prefs.getString(MainActivity.KEY_FARE_TYPE, "adult");
        String ticketType = prefs.getString(MainActivity.KEY_TICKET_TYPE, "octopus");
        JSONArray fares = route.getJSONArray("fares");
        float totalFare = 0.0f;

        for (int i = 0; i < fares.length(); i++) {
            JSONObject fareSection = fares.getJSONObject(i);
            JSONObject fareInfo = fareSection.getJSONObject("fareInfo");
            String fareTitle = fareSection.optString("fareTitle", "");

            if (fareTitle.equals("firstClass")) continue;

            String currentFareType = fareType;

            if (!fareInfo.has(currentFareType)) {
                if (currentFareType.equals("concessionchild") || currentFareType.equals("concessionchild2")) {
                    currentFareType = "concession";
                } else {
                    currentFareType = "adult";
                }
            }


            JSONObject idObj = fareInfo.optJSONObject(currentFareType);
            if (idObj != null) {
                String fStr = idObj.optString(ticketType, "0");

                if (!fStr.equals("-") && !fStr.equals("免費")) {
                    try {
                        totalFare += Float.parseFloat(fStr);
                    } catch (Exception e) {
                    }
                }
            }
        }
        return totalFare;
    }
}