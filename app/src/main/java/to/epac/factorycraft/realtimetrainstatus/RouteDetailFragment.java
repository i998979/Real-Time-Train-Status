package to.epac.factorycraft.realtimetrainstatus;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RouteDetailFragment extends Fragment {

    private HRConfig hrConf;
    private SharedPreferences prefs;

    private ScrollView svRoute;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_route_detail, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(getContext());

        MaterialButton btnReturn = root.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        svRoute = root.findViewById(R.id.sv_route);

        LinearLayout routeContainer = root.findViewById(R.id.route_container);

        TextView tvStartTime = root.findViewById(R.id.tv_start_time);
        TextView tvEndTime = root.findViewById(R.id.tv_end_time);
        TextView journeyStart = root.findViewById(R.id.tv_journey_start);

        TextView tvJourneyTime = root.findViewById(R.id.tv_journey_time);
        TextView tvInterchangeCount = root.findViewById(R.id.tv_interchange_count);
        TextView tvFare = root.findViewById(R.id.tv_fare);

        MaterialButton btnShare = root.findViewById(R.id.btn_share);
        btnShare.setOnClickListener(v -> {
            Bitmap routeBitmap = bitmapFromView(routeContainer);
            shareBitmap(routeBitmap);
        });

        MaterialButton btnRoutePicture = root.findViewById(R.id.btn_route_picture);
        btnRoutePicture.setOnClickListener(v -> {
            Bitmap routeBitmap = bitmapFromView(routeContainer);
            saveBitmapToDCIM(routeBitmap);
        });
        MaterialButton btnRouteSave = root.findViewById(R.id.btn_route_save);
        MaterialButton btnCheckValue = root.findViewById(R.id.btn_check_value);
        btnCheckValue.setOnClickListener(v -> {
            openExternalApp(MainActivity.OCTOPUS_PACKAGE);
        });


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

            // Footer Data
            tvInterchangeCount.setText(selectedRoute.optString("interchangeStationsNo"));
            tvFare.setText(getFare(selectedRoute) + "");


            // Apply path segments
            List<VisualSegment> segments = parsePathToSegments(selectedRoute.getJSONArray("path"), startH, startM);
            if (!segments.isEmpty()) {
                VisualSegment lastSeg = segments.get(segments.size() - 1);
                tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", lastSeg.endH, lastSeg.endM));

                int totalJourneyMinutes = (lastSeg.endH * 60 + lastSeg.endM) - (startH * 60 + startM);
                if (totalJourneyMinutes < 0) totalJourneyMinutes += 1440;
                tvJourneyTime.setText(totalJourneyMinutes + "");
            }
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
        tvLine.setText(hrConf.getLineByAlias(seg.lineCode).name);

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


    private Bitmap bitmapFromView(View targetView) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        int backgroundColor = typedValue.data;

        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        targetView.measure(widthSpec, heightSpec);

        int measuredWidth = targetView.getMeasuredWidth();
        int measuredHeight = targetView.getMeasuredHeight();

        targetView.layout(0, 0, measuredWidth, measuredHeight);

        Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(backgroundColor);
        targetView.draw(canvas);

        targetView.requestLayout();

        return bitmap;
    }

    private void shareBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        try {
            File cachePath = new File(requireContext().getExternalCacheDir(), "route_detail");
            cachePath.mkdirs();
            File file = new File(cachePath, "screenshot_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file);

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/png");

                startActivity(Intent.createChooser(shareIntent, "分享路綫截圖"));
            }

        } catch (IOException e) {
            e.printStackTrace();
            showSnackBar(svRoute, Color.parseColor("#E14158"), "路綫截圖分享失敗。");
        }
    }

    private void saveBitmapToDCIM(Bitmap bitmap) {
        if (bitmap == null) return;

        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
        String subFolderName = Environment.DIRECTORY_DCIM + File.separator + getString(R.string.app_name);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

        values.put(MediaStore.MediaColumns.RELATIVE_PATH, subFolderName);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        try {
            ContentResolver resolver = requireContext().getContentResolver();
            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = resolver.insert(contentUri, values);

            if (uri != null) {
                OutputStream outputStream = resolver.openOutputStream(uri);
                if (outputStream != null) {
                    boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();

                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);

                    if (success) {
                        showSnackBar(svRoute, Color.parseColor("#58A473"), "路綫截圖已成功保存。");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showSnackBar(svRoute, Color.parseColor("#E14158"), "路綫截圖保存失敗。");
        }
    }


    private void showSnackBar(View anchor, int color, String message) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        snackbarView.setBackgroundTintList(ColorStateList.valueOf(color));
        // snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);

        View navigationBar = getActivity().findViewById(R.id.bottom_navigation);
        snackbar.setAnchorView(navigationBar);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }


    private void openExternalApp(String packageName) {
        try {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                goToPlayStore(packageName);
            }

        } catch (Exception e) {
            goToPlayStore(packageName);
        }
    }

    private void goToPlayStore(String appPackage) {
        Uri uri = Uri.parse("market://details?id=" + appPackage);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackage)));
        }
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
}