// MainActivity.java
package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, new RouteSearchFragment())
                    .commit();

            bottomNavigationView.setSelectedItemId(R.id.nav_search);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selected = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);

            if (itemId == R.id.nav_search && selected instanceof RouteSearchFragment) return true;
            if (itemId == R.id.nav_status && selected instanceof OperationInfoFragment) return true;

            if (itemId == R.id.nav_search) {
                selected = new RouteSearchFragment();
            } else if (itemId == R.id.nav_status) {
                selected = new OperationInfoFragment();
            } else if (itemId == R.id.nav_info) {
                selected = null;
            } else if (itemId == R.id.nav_more) {
                selected = null;
            }

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment_container, selected)
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
                    onBackPressed();
                }
            }
        });
    }
}