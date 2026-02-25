package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "route_prefs";
    public static final String KEY_LAST_NAV_ID = "last_nav_id";
    public static final String KEY_ORIGIN_ID = "origin_id";
    public static final String KEY_DEST_ID = "dest_id";

    public static final String KEY_ROUTESEARCH_LAST_TAB = "routesearch_last_selected_tab";
    public static final String KEY_OPERATIONINFO_LAST_TAB = "operationinfo_last_selected_tab";
    public static final String KEY_STATIONINFO_LAST_TAB = "stationinfo_last_selected_tab";

    public static final String KEY_LAST_STATION_CODE = "last_station_code";

    public static final String KEY_WALK_SPEED = "walk_speed";
    public static final String KEY_TICKET_TYPE = "ticket_type";
    public static final String KEY_FARE_TYPE = "fare_type";

    public static final String OCTOPUS_PACKAGE = "com.octopuscards.nfc_reader";
    public static final String MTRMOBILE_PACKAGE = "com.mtr.mtrmobile";


    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            int lastNavId = prefs.getInt(KEY_LAST_NAV_ID, R.id.nav_search);

            Fragment initial;
            if (lastNavId == R.id.nav_operation) {
                initial = new OperationInfoFragment();
            } else if (lastNavId == R.id.nav_station) {
                initial = new StationInfoFragment();
            } else if (lastNavId == R.id.nav_more) {
                initial = new MoreFragment();
            } else {
                initial = new RouteSearchFragment();
                lastNavId = R.id.nav_search;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, initial)
                    .commit();

            bottomNavigationView.setSelectedItemId(lastNavId);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putInt(KEY_LAST_NAV_ID, itemId)
                    .apply();

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);

            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            Fragment nextFragment = null;

            if (itemId == R.id.nav_search) {
                if (currentFragment instanceof RouteSearchFragment) {
                    ((RouteSearchFragment) currentFragment).viewPager.setCurrentItem(1, true);
                    return true;
                }
                nextFragment = new RouteSearchFragment();
            } else if (itemId == R.id.nav_operation) {
                if (currentFragment instanceof OperationInfoFragment) return true;
                nextFragment = new OperationInfoFragment();
            } else if (itemId == R.id.nav_station) {
                if (currentFragment instanceof StationInfoFragment) return true;
                nextFragment = new StationInfoFragment();
            } else if (itemId == R.id.nav_more) {
                if (currentFragment instanceof MoreFragment) return true;
                nextFragment = new MoreFragment();
            }

            if (nextFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, nextFragment)
                        .commit();
                return true;
            }

            return false;
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        handleWidgetIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && "SAVED_ROUTE".equals(intent.getStringExtra("TARGET_FRAGMENT"))) {
            // 1. 切換 BottomNavigationView 到「運行情報」分頁
            bottomNavigationView.setSelectedItemId(R.id.nav_operation);

            // 2. 嘗試尋找當前 Fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);

            // 如果 Fragment 已經存在（例如 App 原本就在後台打開著）
            if (currentFragment instanceof OperationInfoFragment) {
                ViewPager2 pager = currentFragment.getView().findViewById(R.id.pager_content);
                if (pager != null) {
                    pager.setCurrentItem(0, true);
                    intent.removeExtra("TARGET_FRAGMENT");
                }
            }
        }
    }
}