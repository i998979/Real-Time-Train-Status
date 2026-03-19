package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SavedRouteManager {
    private static final String PREFS_NAME = "saved_routes_prefs";
    private static final String KEY_SAVED_ROUTES = "saved_routes";
    private SharedPreferences prefs;

    public SavedRouteManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveRoute(String originID, String destID, String originName, String destName, String routeJson) {
        List<SavedRoute> currentRoutes = getSavedRoutes();
        currentRoutes.add(new SavedRoute(originID, destID, originName, destName, routeJson));
        saveRoutesToPrefs(currentRoutes);
    }

    public List<SavedRoute> getSavedRoutes() {
        List<SavedRoute> routes = new ArrayList<>();
        String json = prefs.getString(KEY_SAVED_ROUTES, "[]");
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                routes.add(new SavedRoute(
                        jsonObject.getString("oID"),
                        jsonObject.getString("dID"),
                        jsonObject.getString("oName"),
                        jsonObject.getString("dName"),
                        jsonObject.optString("rJson", "")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return routes;
    }

    private void saveRoutesToPrefs(List<SavedRoute> routes) {
        JSONArray jsonArray = new JSONArray();
        for (SavedRoute route : routes) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("oID", route.getOriginID());
                jsonObject.put("dID", route.getDestID());
                jsonObject.put("oName", route.getOriginName());
                jsonObject.put("dName", route.getDestName());
                jsonObject.put("rJson", route.getRouteJson());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_SAVED_ROUTES, jsonArray.toString()).apply();
    }

    public void deleteRoute(int position) {
        List<SavedRoute> routes = getSavedRoutes();
        if (position >= 0 && position < routes.size()) {
            routes.remove(position);
            saveRoutesToPrefs(routes);
        }
    }

    public void reorderRoutes(List<SavedRoute> routes) {
        saveRoutesToPrefs(routes);
    }

    public static class SavedRoute {
        private String originID;
        private String destID;
        private String originName;
        private String destName;
        private String routeJson;

        public SavedRoute(String originID, String destID, String originName, String destName, String routeJson) {
            this.originID = originID;
            this.destID = destID;
            this.originName = originName;
            this.destName = destName;
            this.routeJson = routeJson;
        }

        public String getOriginID() {
            return originID;
        }

        public String getDestID() {
            return destID;
        }

        public String getOriginName() {
            return originName;
        }

        public String getDestName() {
            return destName;
        }

        public String getRouteJson() {
            return routeJson;
        }
    }
}