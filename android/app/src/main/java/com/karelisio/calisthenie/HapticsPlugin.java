package com.karelisio.calisthenie;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Vibration native : navigator.vibrate() côté web exige un geste utilisateur
 * direct (règle du navigateur, appliquée aussi dans la WebView Android) et
 * échoue donc silencieusement quand l'appel part d'un minuteur (setInterval)
 * plutôt que d'un clic — le cas de la plupart des vibrations de cette app
 * (fin de série, pause "changez de côté"...). Ce module natif contourne
 * cette restriction : l'app découpe ses motifs en plusieurs appels
 * "vibre pendant X ms" (voir vibrate() dans index.html).
 */
@CapacitorPlugin(name = "Haptics")
public class HapticsPlugin extends Plugin {

    @PluginMethod
    public void vibrate(PluginCall call) {
        Integer duration = call.getInt("duration");
        int ms = duration != null ? duration : 50;

        Vibrator vibrator = getVibrator();
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(Math.max(1, ms), VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
        call.resolve();
    }

    private Vibrator getVibrator() {
        Context context = getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager != null ? manager.getDefaultVibrator() : null;
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
