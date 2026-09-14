package com.karelisio.calisthenie;

import android.content.Context;
import android.media.AudioAttributes;
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

    /* Usage "alarme" plutôt que l'usage par défaut : sans attributs, Android
       traite la vibration comme un retour tactile et la supprime quand le
       retour haptique au toucher est désactivé dans les réglages du téléphone.
       Ici c'est un signal de fin de série : il doit passer comme une alarme. */
    private static final AudioAttributes VIBRATION_ATTRS = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();

    @PluginMethod
    public void vibrate(PluginCall call) {
        Integer duration = call.getInt("duration");
        int ms = duration != null ? duration : 50;

        Vibrator vibrator = getVibrator();
        if (vibrator == null || !vibrator.hasVibrator()) {
            // Rejeter plutôt que résoudre : l'app peut ainsi le dire à
            // l'utilisateur au lieu de faire croire que ça a marché.
            call.reject("aucun vibreur détecté sur l'appareil");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(Math.max(1, ms), VibrationEffect.DEFAULT_AMPLITUDE),
                    VIBRATION_ATTRS);
            } else {
                vibrator.vibrate(ms, VIBRATION_ATTRS);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("vibration refusée par Android : " + e.getMessage(), e);
        }
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
