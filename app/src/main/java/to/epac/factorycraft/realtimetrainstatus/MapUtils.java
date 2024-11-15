package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapUtils {

    Context ctx;

    public MapUtils(Context ctx) {
        this.ctx = ctx;
    }

    public void drawPolylines(GoogleMap map, List<LatLng> points, String color) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(points);
        polylineOptions.width(12);
        polylineOptions.color(Color.parseColor(color));
        map.addPolyline(polylineOptions);
    }

    /**
     * Get the starting point and ending point of the sector which is closest to the specified location
     *
     * @param latLng Location to check
     * @return Starting point and ending point of the closest sector
     */
    public LatLng[] getClosestSector(LatLng latLng, String line, boolean isSpur) {
        // Retrieve all sector points
        List<LatLng> sectorPoints = new ArrayList<>();
        if (line.equals("EAL")) {
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(isSpur ? R.string.eal_lmc : R.string.eal_low)));
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(R.string.eal_main)));
        } else
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(R.string.tml_main)));


        // Closes sector
        LatLng[] closestSector = new LatLng[2];
        // How close it is
        double closestDistance = Double.MAX_VALUE;

        // Loop through all sectors, omit the last one as it must be the ending point
        for (int i = 0; i < sectorPoints.size() - 1; i++) {
            LatLng from = sectorPoints.get(i);
            LatLng to = sectorPoints.get(i + 1);

            // Split sectors into 10 sub-points
            for (int j = 0; j <= 100; j++) {
                LatLng temp = SphericalUtil.interpolate(from, to, j * 0.01);
                double dist = SphericalUtil.computeDistanceBetween(temp, latLng);

                // If the distance between sub-point and location is closer than the saved one
                if (dist < closestDistance) {
                    // It is now the closest one
                    closestDistance = dist;
                    closestSector[0] = from;
                    closestSector[1] = to;
                }
            }
        }
        return closestSector;
    }

    /**
     * Get all sector points between from and to, including trimmed starting point and ending point
     *
     * @param from   From where
     * @param to     To where
     * @param isSpur Checking spur line instead of main line
     * @return List of the sector points
     */
    public List<LatLng> getAllSectorPointsBetween(LatLng from, LatLng to, String line, boolean isSpur) {
        List<LatLng> latLngs = new ArrayList<>();

        List<LatLng> sectorPoints = new ArrayList<>();
        if (line.equals("EAL")) {
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(isSpur ? R.string.eal_lmc : R.string.eal_low)));
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(R.string.eal_main)));
        } else
            sectorPoints.addAll(Utils.getLatLngs(ctx.getResources().getString(R.string.tml_main)));


        LatLng[] fromSector = getClosestSector(from, line, isSpur);
        LatLng[] toSector = getClosestSector(to, line, isSpur);

        boolean swapped = false;

        // If both "from" and "to" is in the same sector
        if (Arrays.asList(fromSector).containsAll(Arrays.asList(toSector))) {
            latLngs.add(from);
            latLngs.add(to);
        }
        // If they are in different sector
        else {
            // Get the starting point of from's sector, then retrieve the index of it
            int start = sectorPoints.indexOf(fromSector[1]);
            // Get the ending point of to's sector, then retrieve the index of it
            int end = sectorPoints.indexOf(toSector[0]);

            if (start > end) {
                swapped = true;
                start = sectorPoints.indexOf(toSector[1]);
                end = sectorPoints.indexOf(fromSector[0]);
            }

            latLngs.add(swapped ? to : from);

            // Loop through all sector point between start and end
            for (int i = start; i <= end; i++) {
                latLngs.add(sectorPoints.get(i));
            }

            latLngs.add(swapped ? from : to);

            if (swapped) Collections.reverse(latLngs);
        }

        return latLngs;
    }

    /**
     * Get LatLng from sector list by length
     *
     * @param latLngs Sectors to get
     * @param length  Length to extend
     * @return LatLng between sub-sector which the length is within
     */
    public LatLng getLatLngFromSectorsByLength(List<LatLng> latLngs, double length) {
        LatLng[] sector = new LatLng[]{latLngs.get(0), latLngs.get(1)};
        double elapsedDistance = 0;

        // Loop through the sectors list
        for (int i = 0; i < latLngs.size() - 1; i++) {
            // Get distance of the looping sector
            double distance = SphericalUtil.computeDistanceBetween(latLngs.get(i), latLngs.get(i + 1));

            // If the distance does not exceed, add to count and continue
            if (elapsedDistance + distance < length) {
                elapsedDistance += distance;
            } else {
                // If the distance exceed, the current sector is where the length is in
                sector[0] = latLngs.get(i);
                sector[1] = latLngs.get(i + 1);
                break;
            }
        }

        return SphericalUtil.interpolate(sector[0], sector[1], (length - elapsedDistance) / SphericalUtil.computeDistanceBetween(sector[0], sector[1]));
    }

    /**
     * Get the train's LatLng
     *
     * @param trip Trip of the train
     * @return LatLng of the train
     */
    public LatLng getTrainAt(Trip trip, String line) {
        // if (!trip.trainId.contains("340")) return null;

        int currentStationCode = trip.currentStationCode;
        int nextStationCode = trip.nextStationCode;
        int destinationStationCode = trip.destinationStationCode;
        int targetDistance = trip.targetDistance;
        int startDistance = trip.startDistance;

        int curr = ctx.getResources().getIdentifier(Utils.mapStation(currentStationCode, line).toLowerCase(), "string", ctx.getPackageName());
        int next = ctx.getResources().getIdentifier(Utils.mapStation(nextStationCode, line).toLowerCase(), "string", ctx.getPackageName());
        int targ = ctx.getResources().getIdentifier(Utils.mapStation(destinationStationCode, line).toLowerCase(), "string", ctx.getPackageName());

        LatLng currLatLng = null;
        if (curr > 0) {
            currLatLng = Utils.getLatLng(ctx.getResources().getString(curr));
        }

        LatLng nextLatLng = null;
        if (next > 0) {
            nextLatLng = Utils.getLatLng(ctx.getResources().getString(next));
        }


        LatLng targLatLng = null;
        if (targ > 0) {
            targLatLng = Utils.getLatLng(ctx.getResources().getString(targ));
        }


        // FOT->SHT->ADM will show FOT->SHT when arrived SHT
        if (currLatLng != null && nextLatLng != null && trip.targetDistance == 0 && Utils.isPassengerTrain(trip.td))
            return null;

        // Handle non-passenger train
        if (targetDistance == 0 && startDistance == 0)
            return currLatLng;

        if (startDistance > 10000)
            return currLatLng;

        if ((trip.currentStationCode == 0 || trip.currentStationCode == 701) && (trip.nextStationCode == 0 || trip.nextStationCode == 701)) {
            if (line.equals("TML"))
                return Utils.getLatLng(ctx.getResources().getString(R.string.phd));
            else
                return Utils.getLatLng(ctx.getResources().getString(R.string.htd));
        }


        List<LatLng> sectorsBetweenCurrAndNext = getAllSectorPointsBetween(currLatLng, nextLatLng, line,
                trip.currentStationCode == 14 || trip.destinationStationCode == 14 || trip.nextStationCode == 14);

        double lengthBetweenCurrAndNext = SphericalUtil.computeLength(sectorsBetweenCurrAndNext);

        // Percentage of how far is away from the previous station
        double originalDistancePercent = (double) startDistance / ((double) targetDistance + (double) startDistance);

        // The train has moved x% / meters away from the previous station
        double lengthElapsed = lengthBetweenCurrAndNext * originalDistancePercent;

        return getLatLngFromSectorsByLength(sectorsBetweenCurrAndNext, lengthElapsed);
    }

    public int getDistance(int curr, int next, int distanceFromCurrentStation) {
        String upDist = "1300 1080 1110 2870 750 1140 960 800 1290 4460 1260 1010 1140 1040 780 1130 1710 2770 2410 4390 8930 3540 1040 2340 4950 2100 0";
        String dnDist = "0 1300 1080 1110 2870 750 1140 960 800 1290 4460 1260 1010 1140 1040 780 1130 1710 2770 2410 4390 8930 3540 1040 2340 4950 2100";

        boolean isUp = Utils.covertStationOrder(curr) < Utils.covertStationOrder(next);

        int currIndex = -1;
        int nextIndex = -1;

        String[] stations = ctx.getResources().getString(R.string.tml_stations).split(" ");
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].equals(Utils.mapStation(curr, "TML").toLowerCase())) currIndex = i;
            if (stations[i].equals(Utils.mapStation(next, "TML").toLowerCase())) nextIndex = i;

            if (currIndex != -1 && nextIndex != -1) break;
        }

        if (currIndex == -1 || nextIndex == -1) return 0;

        int distance;
        if (isUp) distance = Integer.parseInt(upDist.split(" ")[currIndex]);
        else distance = Integer.parseInt(dnDist.split(" ")[currIndex]);

        return Math.max(distance - distanceFromCurrentStation, 0);
    }
}
