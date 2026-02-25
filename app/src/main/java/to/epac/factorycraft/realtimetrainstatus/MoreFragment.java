package to.epac.factorycraft.realtimetrainstatus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class MoreFragment extends Fragment {

    private PackageManager pm;

    private MaterialCardView btnOctopus;
    private MaterialCardView btnMtrMobile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        pm = requireContext().getPackageManager();

        btnOctopus = view.findViewById(R.id.btn_octopus);
        btnOctopus.setOnClickListener(v -> openExternalApp(MainActivity.OCTOPUS_PACKAGE));
        btnMtrMobile = view.findViewById(R.id.btn_mtrmobile);
        btnMtrMobile.setOnClickListener(v -> openExternalApp(MainActivity.MTRMOBILE_PACKAGE));

        return view;
    }

    private void openExternalApp(String packageName) {
        try {
            Intent intent = pm.getLaunchIntentForPackage(packageName);

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
}