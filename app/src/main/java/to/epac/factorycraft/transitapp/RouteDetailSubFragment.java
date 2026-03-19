package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RouteDetailSubFragment extends Fragment {

    private SavedRouteManager savedRouteManager;

    private HRConfig hrConf;
    private SharedPreferences prefs;

    private MaterialButton btnClose;
    private MaterialButton btnReturn;

    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvJourneyStart;

    private LinearLayout routeContainer;

    private TextView tvJourneyTime;
    private TextView tvInterchangeCount;
    private TextView tvFare;

    private MaterialButton btnAdd;

    private String currentOriginID, currentDestID, currentOriginName, currentDestName, routeData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_detail_bottomsheet, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(getContext());
        savedRouteManager = new SavedRouteManager(requireContext());

        btnReturn = view.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            if (getParentFragment() instanceof RouteHostBottomSheet) {
                ((RouteHostBottomSheet) getParentFragment()).dismiss();
            }
        });
        tvStartTime = view.findViewById(R.id.tv_start_time);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        tvJourneyStart = view.findViewById(R.id.tv_journey_start);

        routeContainer = view.findViewById(R.id.route_container);

        tvJourneyTime = view.findViewById(R.id.tv_journey_time);
        tvInterchangeCount = view.findViewById(R.id.tv_interchange_count);
        tvFare = view.findViewById(R.id.tv_fare);

        btnAdd = view.findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            savedRouteManager.saveRoute(currentOriginID, currentDestID, currentOriginName, currentDestName, routeData);

            if (getParentFragment() instanceof RouteHostBottomSheet) {
                RouteHostBottomSheet host = (RouteHostBottomSheet) getParentFragment();

                host.notifyRouteAdded();
                host.dismiss();
            }
        });

        try {
            JSONObject data = new JSONObject(getArguments().getString("route_data"));
            JSONObject selectedRoute = data.getJSONArray("routes").getJSONObject(getArguments().getInt("selected_route"));

            routeData = selectedRoute.toString();

            // Start and end time calculation
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E)", Locale.TRADITIONAL_CHINESE);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

            int nowMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            try {
                String firstTimeStr = data.getJSONObject("firstTrain").getString("time");
                String lastTimeStr = data.getJSONObject("lastTrain").getString("time");
                int firstMins = Integer.parseInt(firstTimeStr.split(":")[0]) * 60 + Integer.parseInt(firstTimeStr.split(":")[1]);
                int lastMins = Integer.parseInt(lastTimeStr.split(":")[0]) * 60 + Integer.parseInt(lastTimeStr.split(":")[1]);

                if (lastMins < firstMins) lastMins += 24 * 60;

                boolean isTomorrow = (nowMins > lastMins % 1440 && nowMins < firstMins);
                if (isTomorrow)
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            tvJourneyStart.setText(sdf.format(calendar.getTime()));

            String startTime = getArguments().getString("start_time");
            int startH = Integer.parseInt(startTime.split(":")[0]);
            int startM = Integer.parseInt(startTime.split(":")[1]);
            tvStartTime.setText(startTime);

            // Footer Data
            tvInterchangeCount.setText(selectedRoute.optString("interchangeStationsNo"));
            tvFare.setText(getFare(selectedRoute) + "");

            // Apply path segments
            List<VisualSegment> segments = parsePathToSegments(selectedRoute.getJSONArray("path"), startH, startM);
            if (!segments.isEmpty()) {
                VisualSegment firstSeg = segments.get(0);
                int oID = firstSeg.startNode.optInt("ID");
                currentOriginID = String.valueOf(oID);
                currentOriginName = hrConf.getStationName(oID);

                VisualSegment lastSeg = segments.get(segments.size() - 1);
                int dID = lastSeg.endNode.optInt("ID");
                currentDestID = String.valueOf(dID);
                currentDestName = hrConf.getStationName(dID);

                tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", lastSeg.endH, lastSeg.endM));

                int totalJourneyMinutes = (lastSeg.endH * 60 + lastSeg.endM) - (startH * 60 + startM);
                if (totalJourneyMinutes < 0) totalJourneyMinutes += 1440;
                tvJourneyTime.setText(totalJourneyMinutes + "");
            }
            buildRouteUI(inflater, routeContainer, segments, startH, startM);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
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
            if (i == segments.size() - 1) {
                View lastStationView = inflater.inflate(R.layout.item_route_step, container, false);
                MaterialTextView tvStation = lastStationView.findViewById(R.id.tv_station);
                tvStation.setText(hrConf.getStationName(seg.endNode.optInt("ID")));
                tvStation.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), StationActivity.class);
                    intent.putExtra("station_code", hrConf.getStationAlias(seg.endNode.optInt("ID")));
                    v.getContext().startActivity(intent);
                });

                container.addView(lastStationView);
            }
        }
    }


    private void addStationSection(LayoutInflater inflater, LinearLayout container, JSONObject node) {
        View view = inflater.inflate(R.layout.item_route_step, container, false);

        TextView tvStation = view.findViewById(R.id.tv_station);
        tvStation.setText(hrConf.getStationName(node.optInt("ID")));
        tvStation.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), StationActivity.class);
            intent.putExtra("station_code", hrConf.getStationAlias(node.optInt("ID")));
            v.getContext().startActivity(intent);
        });

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
        tvLine.setText(hrConf.getLineByAlias(seg.lineCode).name);

        TextView tvBadge = view.findViewById(R.id.tv_line_code_badge);
        View lineColorBadge = view.findViewById(R.id.line_color_badge);
        tvBadge.setText(hrConf.getLineById(seg.lineID).alias);
        lineColorBadge.setBackgroundColor(color);

        TextView tvDirection = view.findViewById(R.id.tv_direction);
        String towards = seg.startNode.optString("towards");
        if (!towards.isEmpty()) {
            String[] ids = towards.split("/");
            String direction = hrConf.getStationName(Integer.parseInt(ids[0].trim()));
            if (ids.length > 1)
                direction += "/" + hrConf.getStationName(Integer.parseInt(ids[1].trim()));

            tvDirection.setText(direction + " 方面");
        }

        TextView tvCarsCount = view.findViewById(R.id.tv_car_count);
        String alias = hrConf.getLineById(seg.lineID).alias;
        tvCarsCount.setText(alias.equals("EAL") ? "9両" : alias.equals("SIL") ? "3両" : alias.equals("DRL") ? "4両" : "8両");


        MaterialCardView trafficCard = view.findViewById(R.id.v_traffic_news);
        String currentLineCode = hrConf.getLineById(seg.lineID).alias;

        String tNews = getArguments().getString("traffic_news", "");
        if (!tNews.isEmpty()) {
            try {
                JSONObject tJson = new JSONObject(tNews);
                JSONArray trafficNews = tJson.getJSONObject("ryg_status").getJSONArray("line");

                for (int i = 0; i < trafficNews.length(); i++) {
                    JSONObject lineObj = trafficNews.getJSONObject(i);
                    String lineCode = lineObj.getString("line_code").toUpperCase();

                    if (lineCode.equals(currentLineCode)) {
                        String status = lineObj.getString("status").toLowerCase();

                        if (!status.equals("green") && !status.equals("grey")) {
                            trafficCard.setVisibility(View.VISIBLE);

                            TextView tvStatus = view.findViewById(R.id.tv_status);
                            TextView tvMessage = view.findViewById(R.id.tv_message);
                            TextView tvLineSection = view.findViewById(R.id.tv_line_section);
                            ImageView ivIcon = view.findViewById(R.id.iv_status_icon);

                            updateStatusUI(status, tvStatus, ivIcon);

                            String displayMessage = "服務受阻";
                            String lineSectionText = "全綫";
                            Object messagesObj = lineObj.opt("messages");

                            if (messagesObj instanceof JSONObject) {
                                JSONObject msgObj = ((JSONObject) messagesObj).optJSONObject("message");
                                if (msgObj != null) {
                                    displayMessage = msgObj.optString("title_tc", msgObj.optString("cause_tc", ""));
                                    JSONObject affectedArea = msgObj.optJSONObject("affected_areas");
                                    if (affectedArea != null) {
                                        JSONObject area = affectedArea.optJSONObject("affected_area");
                                        if (area != null) {
                                            lineSectionText = hrConf.getStationName(area.optInt("station_code_fr")) + "~"
                                                    + hrConf.getStationName(area.optInt("station_code_to"));
                                        }
                                    }
                                }
                            } else if (messagesObj instanceof String) {
                                displayMessage = (String) messagesObj;
                            }

                            tvMessage.setText(displayMessage);
                            tvLineSection.setText(lineSectionText);

                            trafficCard.setOnClickListener(v -> {
                                android.content.Intent intent = new android.content.Intent(getActivity(), TrafficNewsActivity.class);
                                intent.putExtra("line_code", lineCode);
                                intent.putExtra("line_name_tc", hrConf.getLineByAlias(lineCode).name);
                                intent.putExtra("line_color", lineObj.optString("line_color", seg.lineColor));
                                intent.putExtra("status", status);
                                intent.putExtra("messages", messagesObj != null ? messagesObj.toString() : "");
                                startActivity(intent);
                            });
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


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
        MaterialButton btnMap = walkView.findViewById(R.id.btn_map);

        tvWalkIntTime.setText(seg.duration + " 分");
        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.startH, seg.startM));
        tvArriveTime.setText(String.format(Locale.getDefault(), "%02d:%02d", seg.endH, seg.endM));
        btnMap.setOnClickListener(v -> {
            String coords = hrConf.getStationCoord(seg.endNode.optInt("ID"));

            if (!coords.isEmpty()) {
                Uri uri = Uri.parse("geo:" + coords + "?z=17");

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.google.android.apps.maps");

                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + coords);
                    startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                }
            }
        });

        container.addView(walkView);
    }


    private List<VisualSegment> parsePathToSegments(JSONArray path, int startH, int startM) {
        List<VisualSegment> segments = new ArrayList<>();
        if (path == null || path.length() < 2) return segments;

        // Walk interchange multiplier
        float multiplier = 1.0F;
        String speed = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");
        switch (speed) {
            case "很慢":
                multiplier = 1.2F;
                break;
            case "慢速":
                multiplier = 1.1F;
                break;
            case "普通":
                multiplier = 1.0F;
                break;
            case "快速":
                multiplier = 0.9F;
                break;
        }

        int accumulatedDelay = 0;

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
            int rawStartTime = startNode.optInt("time");
            int rawEndTime = endNode.optInt("time");
            int rawDuration = rawEndTime - rawStartTime;

            int startTotalM = (startH * 60) + startM + rawStartTime + accumulatedDelay;
            seg.startH = (startTotalM / 60) % 24;
            seg.startM = startTotalM % 60;

            seg.duration = (int) (rawDuration * (isWalk ? multiplier : 1.0F));

            int endTotalM = startTotalM + seg.duration;
            seg.endH = (endTotalM / 60) % 24;
            seg.endM = endTotalM % 60;

            accumulatedDelay += (seg.duration - rawDuration);

            // Apply line name and color
            seg.lineCode = hrConf.getLineById(startLineID).alias;
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


    private void updateStatusUI(String status, TextView tvStatus, ImageView ivIcon) {
        switch (status.toLowerCase()) {
            case "yellow":
                tvStatus.setText("服務延誤");
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告生效");
                ivIcon.setImageResource(R.drawable.baseline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
        }
    }
}