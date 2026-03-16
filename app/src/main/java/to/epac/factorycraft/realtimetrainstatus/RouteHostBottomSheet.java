package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RouteHostBottomSheet extends BottomSheetDialogFragment {

    private static final int CONTAINER_ID = View.generateViewId();

    public interface OnRouteAddedListener {
        void onRouteAdded();
    }

    private OnRouteAddedListener onRouteAddedListener;

    public void setOnRouteAddedListener(OnRouteAddedListener listener) {
        this.onRouteAddedListener = listener;
    }

    public void notifyRouteAdded() {
        if (onRouteAddedListener != null) {
            onRouteAddedListener.onRouteAdded();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int targetHeight = (int) (displayHeight * 0.9);
            behavior.setPeekHeight(targetHeight);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setHideable(false);
            getDialog().setCanceledOnTouchOutside(false);

            ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
            lp.height = targetHeight;
            bottomSheet.setLayoutParams(lp);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setId(CONTAINER_ID);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null && args.containsKey(RouteSearchFragment.ORIGIN_ID) && args.containsKey(RouteSearchFragment.DEST_ID)) {
                RouteListSubFragment listFrag = new RouteListSubFragment();
                listFrag.setArguments(args);
                navigateTo(listFrag, false);
            } else {
                navigateTo(new SearchInputSubFragment(), false);
            }
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() != null) {
            getDialog().setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {

                    if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                        getChildFragmentManager().popBackStack();
                        return true;
                    }
                }
                return false;
            });
        }
    }


    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        transaction.setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
        );

        transaction.replace(CONTAINER_ID, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}