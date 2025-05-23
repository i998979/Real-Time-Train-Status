package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.res.Resources;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.widget.EditText;

public class NumberPicker extends android.widget.NumberPicker {

    public NumberPicker(Context context) {
        super(context);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            EditText input = findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
            if (input != null) {
                input.setFilters(new InputFilter[]{});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
