package com.rama.okapi_zero.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.rama.okapi_zero.objects.PrefTheme;
import com.rama.okapi_zero.objects.Themes;

public class PrefsManager {
    private static final String PREFS_NAME = "okapi";
    private static final String KEY_FLAVOR_DARK = "settings:flavor_dark";
    private static final String KEY_ACCENT = "settings:accent";
    private static final String KEY_ACCENT_ROLLED = "settings:accent_rolled";
    private static final String KEY_FLAVOR_LIGHT = "settings:flavor_light";
    /** Older builds stored flavor and accent as one id per mode, e.g. "catppuccin_mocha_peach". Only read to migrate. */
    private static final String KEY_LEGACY_DARK = "settings:theme_dark";
    private static final String KEY_LEGACY_LIGHT = "settings:theme_light";
    private static final String LEGACY_MOCHA_PREFIX = "catppuccin_mocha_";
    private static final String KEY_LIGHT_MODE = "settings:light_mode";
    private static final String KEY_ZOOM_PERCENT = "settings:zoom_percent";
    public static final String KEY_STATUS_BAR = "settings:show_status_bar";
    public static final String KEY_KEEP_AWAKE = "settings:keep_screen_awake";
    public static final String KEY_QUICK_ERASE = "settings:quick_erase";
    private static PrefsManager instance;
    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyThemes();
    }

    /** Flavor and accent of an old single theme id, or null when it is not one. Latte's blue was fixed, not a choice. */
    private static String[] parseLegacyTheme(String id, boolean light) {
        if (id == null) return null;
        if (id.startsWith(LEGACY_MOCHA_PREFIX)) {
            String accent = id.substring(LEGACY_MOCHA_PREFIX.length());
            return !light && Themes.isAccent(accent) ? new String[]{"mocha", accent} : null;
        }
        if ("catppuccin_latte".equals(id)) return light ? new String[]{"latte", null} : null;
        if (Themes.isFlavor(id, light) && !Themes.hasAccents(id)) return new String[]{id, null};
        return null;
    }

    private void migrateLegacyThemes() {
        if (!prefs.contains(KEY_LEGACY_DARK) && !prefs.contains(KEY_LEGACY_LIGHT)) return;
        SharedPreferences.Editor editor = prefs.edit();
        String[] dark = parseLegacyTheme(prefs.getString(KEY_LEGACY_DARK, null), false);
        if (dark != null && !prefs.contains(KEY_FLAVOR_DARK)) {
            editor.putString(KEY_FLAVOR_DARK, dark[0]);
            // The old dark accent is the one the user picked, so it becomes the shared accent.
            if (dark[1] != null && !prefs.contains(KEY_ACCENT)) editor.putString(KEY_ACCENT, dark[1]);
        }
        String[] light = parseLegacyTheme(prefs.getString(KEY_LEGACY_LIGHT, null), true);
        if (light != null && !prefs.contains(KEY_FLAVOR_LIGHT)) {
            editor.putString(KEY_FLAVOR_LIGHT, light[0]);
        }
        editor.remove(KEY_LEGACY_DARK).remove(KEY_LEGACY_LIGHT).commit();
    }

    public static synchronized PrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsManager(context);
        }
        return instance;
    }

    public String getFlavor(boolean light) {
        String def = light ? PrefTheme.DEFAULT_LIGHT_FLAVOR : PrefTheme.DEFAULT_DARK_FLAVOR;
        String id = prefs.getString(light ? KEY_FLAVOR_LIGHT : KEY_FLAVOR_DARK, def);
        return Themes.isFlavor(id, light) ? id : def;
    }

    public void setFlavor(boolean light, String flavorId) {
        prefs.edit().putString(light ? KEY_FLAVOR_LIGHT : KEY_FLAVOR_DARK, flavorId).commit();
    }

    public String getAccent() {
        String id = prefs.getString(KEY_ACCENT, PrefTheme.DEFAULT_ACCENT);
        return Themes.isAccent(id) || PrefTheme.ACCENT_RANDOM.equals(id) ? id : PrefTheme.DEFAULT_ACCENT;
    }

    public void setAccent(String accentId) {
        prefs.edit().putString(KEY_ACCENT, accentId).commit();
    }

    public String getRolledAccent() {
        String id = prefs.getString(KEY_ACCENT_ROLLED, Themes.DEFAULT_ACCENT_COLOR);
        return Themes.isAccent(id) ? id : Themes.DEFAULT_ACCENT_COLOR;
    }

    public void setRolledAccent(String accentId) {
        prefs.edit().putString(KEY_ACCENT_ROLLED, accentId).commit();
    }

    public boolean isLightMode() {
        return getBoolean(KEY_LIGHT_MODE, false);
    }

    public void setLightMode(boolean value) {
        setBoolean(KEY_LIGHT_MODE, value);
    }

    public int getZoomPercent() {
        return prefs.getInt(KEY_ZOOM_PERCENT, 100);
    }

    public void setZoomPercent(int percent) {
        prefs.edit().putInt(KEY_ZOOM_PERCENT, percent).commit();
    }

    public boolean isStatusBarVisible() {
        return getBoolean(KEY_STATUS_BAR, false);
    }

    public boolean isKeepScreenAwake() {
        return getBoolean(KEY_KEEP_AWAKE, true);
    }

    public boolean isQuickEraseEnabled() {
        return getBoolean(KEY_QUICK_ERASE, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }
}
