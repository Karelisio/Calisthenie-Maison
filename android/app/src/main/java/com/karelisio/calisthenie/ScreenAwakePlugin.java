package com.karelisio.calisthenie;

import android.app.Activity;
import android.view.WindowManager;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Empêche l'écran de s'éteindre pendant une séance. On passe par le drapeau
 * natif FLAG_KEEP_SCREEN_ON plutôt que par l'API web Wake Lock : celle-ci est
 * liée à la visibilité de la page et se relâche toute seule, alors que le
 * drapeau reste posé sur la fenêtre tant qu'on ne le retire pas. L'app ne le
 * pose que le temps de la séance, pour ne pas vider la batterie.
 */
@CapacitorPlugin(name = "ScreenAwake")
public class ScreenAwakePlugin extends Plugin {

    @PluginMethod
    public void setKeepAwake(PluginCall call) {
        Boolean requested = call.getBoolean("on");
        final boolean on = requested != null && requested;

        final Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activité indisponible.");
            return;
        }
        // Le drapeau se pose sur la fenêtre : obligatoirement depuis le thread UI.
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (on) {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
        call.resolve();
    }
}
