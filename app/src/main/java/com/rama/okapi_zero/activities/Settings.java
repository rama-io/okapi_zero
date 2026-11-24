package com.rama.okapi_zero.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.rama.okapi_zero.R;
import com.rama.okapi_zero.helpers.SystemBars;
import com.rama.okapi_zero.managers.FontManager;
import com.rama.okapi_zero.managers.PrefsManager;
import com.rama.okapi_zero.managers.ThemeManager;
import com.rama.okapi_zero.managers.ZoomManager;
import com.rama.okapi_zero.objects.PrefTheme;
import com.rama.okapi_zero.objects.Themes;
import com.rama.okapi_zero.widgets.WdCheckbox;
import com.rama.okapi_zero.widgets.WdRadioGroup;

import java.util.List;

public class Settings extends BaseActivity {
    private PrefsManager prefs;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PrefsManager.getInstance(this);
        root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        setupThemes();
        setupSystemSection();
        setupZoomSection();
        FontManager.apply(root, FontManager.getJersey25(this));
        ThemeManager.applyTheme(this, root);
        findViewById(R.id.about_button).setOnClickListener(v -> startActivity(new Intent(Settings.this, About.class)));
        findViewById(R.id.go_back).setOnClickListener(v -> finish());
    }

    private void setupThemes() {
        if (Build.VERSION.SDK_INT < 11) {
            findViewById(R.id.themes_container).setVisibility(View.GONE);
            return;
        }
        setupFlavors(false, R.id.theme_dark_group);
        setupFlavors(true, R.id.theme_light_group);
        setupAccents();
        updateAccentVisibility();
    }

    /** Dark and light each have their own flavor list. */
    private void setupFlavors(final boolean light, int groupId) {
        final List<Themes.Flavor> flavors = Themes.flavors(light);
        WdRadioGroup group = (WdRadioGroup) findViewById(groupId);
        String current = prefs.getFlavor(light);
        int checkedId = -1;
        for (int i = 0; i < flavors.size(); i++) {
            int id = group.addOption(flavors.get(i).label).getId();
            if (flavors.get(i).id.equals(current)) checkedId = id;
        }
        group.check(checkedId);
        group.setOnCheckedChangeListener((g, id) -> {
            prefs.setFlavor(light, flavors.get(g.getCheckedIndex()).id);
            updateAccentVisibility();
            ThemeManager.applyTheme(Settings.this, root);
        });
    }

    /** One accent list shared by both modes. */
    private void setupAccents() {
        final List<Themes.Accent> accents = Themes.accents();
        WdRadioGroup group = (WdRadioGroup) findViewById(R.id.accent_group);
        String current = prefs.getAccent();
        // Option 0 is Random, the accents follow in palette order.
        int checkedId = group.addOption(getString(R.string.label_accent_random)).getId();
        for (int i = 0; i < accents.size(); i++) {
            int id = group.addOption(accents.get(i).label).getId();
            if (accents.get(i).id.equals(current)) checkedId = id;
        }
        group.check(checkedId);
        group.setOnCheckedChangeListener((g, id) -> {
            int index = g.getCheckedIndex();
            if (index <= 0) {
                prefs.setAccent(PrefTheme.ACCENT_RANDOM);
                ThemeManager.roll(Settings.this);
            } else {
                prefs.setAccent(accents.get(index - 1).id);
            }
            ThemeManager.applyTheme(Settings.this, root);
        });
    }

    /** Hidden only when both chosen flavors are fixed palettes (the mono ones), which have no accents. */
    private void updateAccentVisibility() {
        int visibility = ThemeManager.usesAccent(this) ? View.VISIBLE : View.GONE;
        findViewById(R.id.accent_section).setVisibility(visibility);
        findViewById(R.id.accent_separator).setVisibility(visibility);
    }

    private void setupSystemSection() {
        bindCheckbox(R.id.show_status_bar, PrefsManager.KEY_STATUS_BAR, prefs.isStatusBarVisible());
        bindCheckbox(R.id.keep_screen_awake, PrefsManager.KEY_KEEP_AWAKE, prefs.isKeepScreenAwake());
        bindCheckbox(R.id.quick_erase, PrefsManager.KEY_QUICK_ERASE, prefs.isQuickEraseEnabled());
    }

    private void bindCheckbox(int id, final String key, boolean value) {
        WdCheckbox checkbox = findViewById(id);
        checkbox.setChecked(value);
        checkbox.setOnCheckedChangeListener(isChecked -> {
            prefs.setBoolean(key, isChecked);
            SystemBars.setStatusBarVisible(Settings.this, prefs.isStatusBarVisible());
        });
    }

    private void setupZoomSection() {
        final EditText zoom = findViewById(R.id.zoom);
        zoom.setText(String.valueOf(ZoomManager.getPercent(this)));
        zoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyZoom();
            }
            return false;
        });
        zoom.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                applyZoom();
            }
        });
    }

    private void applyZoom() {
        saveZoomFromField();
        ZoomManager.apply(this, root);
    }

    private void saveZoomFromField() {
        EditText zoom = findViewById(R.id.zoom);
        int percent;
        try {
            percent = ZoomManager.clamp(Integer.parseInt(zoom.getText().toString().trim()));
        } catch (NumberFormatException e) {
            percent = ZoomManager.getPercent(this);
        }
        String normalized = String.valueOf(percent);
        if (!normalized.equals(zoom.getText().toString())) {
            zoom.setText(normalized);
        }
        prefs.setZoomPercent(percent);
    }

    @Override
    protected void onPause() {
        saveZoomFromField();
        super.onPause();
    }
}
