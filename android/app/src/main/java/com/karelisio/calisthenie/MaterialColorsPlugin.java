package com.karelisio.calisthenie;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Reads Android 12+ "Material You" wallpaper-derived system colors
 * (system_accent1/2/3, system_neutral1/2) so the web UI can retint its
 * M3 tokens to match the device wallpaper. Resolved by resource name
 * (not android.R.color constants) since those only exist on API 31+.
 */
@CapacitorPlugin(name = "MaterialColors")
public class MaterialColorsPlugin extends Plugin {

    @PluginMethod
    public void getDynamicColors(PluginCall call) {
        JSObject unavailable = new JSObject();
        unavailable.put("available", false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            call.resolve(unavailable);
            return;
        }

        try {
            JSObject ret = new JSObject();
            ret.put("available", true);
            ret.put("accent", hex("system_accent1_200"));
            ret.put("onAccent", hex("system_accent1_800"));
            ret.put("accentContainer", hex("system_accent1_700"));
            ret.put("onAccentContainer", hex("system_accent1_100"));
            ret.put("accent2", hex("system_accent3_200"));
            ret.put("onAccent2", hex("system_accent3_800"));
            ret.put("accent2Container", hex("system_accent3_700"));
            ret.put("onAccent2Container", hex("system_accent3_100"));
            ret.put("bg", hex("system_neutral1_900"));
            ret.put("bg2", hex("system_neutral1_800"));
            ret.put("card", hex("system_neutral1_800"));
            ret.put("card2", hex("system_neutral1_700"));
            ret.put("border", hex("system_neutral2_700"));
            ret.put("text", hex("system_neutral1_100"));
            ret.put("muted", hex("system_neutral2_200"));
            call.resolve(ret);
        } catch (Exception e) {
            call.resolve(unavailable);
        }
    }

    private String hex(String resName) {
        Context context = getContext();
        Resources res = context.getResources();
        int resId = res.getIdentifier(resName, "color", "android");
        if (resId == 0) throw new RuntimeException("Missing dynamic color resource: " + resName);
        int color = res.getColor(resId, context.getTheme());
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
