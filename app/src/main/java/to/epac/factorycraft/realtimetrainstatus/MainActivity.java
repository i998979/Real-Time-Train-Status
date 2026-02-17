package to.epac.factorycraft.realtimetrainstatus;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LAST_NAV_ID = "last_nav_id";

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
    }
}