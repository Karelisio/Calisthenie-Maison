package com.karelisio.calisthenie;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@CapacitorPlugin(name = "AppUpdater")
public class AppUpdaterPlugin extends Plugin {

    private static final String RELEASE_API_URL =
        "https://api.github.com/repos/Karelisio/Calisthenie-Maison/releases/tags/apk-material-latest";
    private static final String PREFS = "app_updater";
    private static final String PREF_DOWNLOAD_ID = "pending_download_id";

    /* Une mise à jour est "disponible" si la publication est plus récente que
       l'installation réellement présente sur l'appareil. On lit cette date via
       PackageManager plutôt que de retenir nous-mêmes ce qu'on a installé :
       un marqueur maison se désynchronise dès qu'une installation est faite à
       la main ou qu'un téléchargement est interrompu — ce qui donnait des
       "vous êtes à jour" mensongers et des mises à jour proposées en boucle. */
    @PluginMethod
    public void checkForUpdate(PluginCall call) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(RELEASE_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                int code = conn.getResponseCode();
                if (code != 200) {
                    call.reject("GitHub a répondu HTTP " + code);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject release = new JSONObject(sb.toString());
                JSONArray assets = release.optJSONArray("assets");
                String apkUrl = null;
                String publishedAt = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        if (asset.optString("name", "").endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url");
                            publishedAt = asset.optString("updated_at");
                            break;
                        }
                    }
                }

                if (apkUrl == null) {
                    call.reject("Aucun APK trouvé dans la dernière publication.");
                    return;
                }

                long releaseTime = parseIso8601(publishedAt);
                long installedTime = installedAt();
                // Marge de 2 minutes : l'horloge de l'appareil et celle de
                // GitHub ne sont jamais parfaitement alignées.
                boolean available = releaseTime > 0 && installedTime > 0
                    && releaseTime > installedTime + 120000L;

                JSObject ret = new JSObject();
                ret.put("available", available);
                ret.put("downloadUrl", apkUrl);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Vérification impossible : " + e.getMessage(), e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /* Lance le téléchargement et rend la main tout de suite : l'avancement est
       ensuite lu par getDownloadStatus(). L'ancienne version attendait
       passivement une diffusion système qui, si elle n'arrivait pas (appli
       mise en arrière-plan, échec silencieux du téléchargement), laissait
       l'écran figé sur "Téléchargement…" indéfiniment. */
    @PluginMethod
    public void downloadAndInstall(PluginCall call) {
        String url = call.getString("url");
        if (url == null) {
            call.reject("URL manquante");
            return;
        }

        try {
            Context context = getContext();
            File dest = destinationFile();
            if (dest.exists() && !dest.delete()) {
                call.reject("Impossible de remplacer le fichier téléchargé précédemment.");
                return;
            }

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                call.reject("Le gestionnaire de téléchargement d'Android est indisponible.");
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationUri(Uri.fromFile(dest));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle("Calisthénie Maison – mise à jour");

            long id = dm.enqueue(request);
            prefs().edit().putLong(PREF_DOWNLOAD_ID, id).apply();

            JSObject ret = new JSObject();
            ret.put("started", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Téléchargement impossible : " + e.getMessage(), e);
        }
    }

    /* État réel du téléchargement en cours, interrogé directement auprès
       d'Android. Déclenche l'écran d'installation dès que le fichier est
       complet. */
    @PluginMethod
    public void getDownloadStatus(PluginCall call) {
        long id = prefs().getLong(PREF_DOWNLOAD_ID, -1);
        JSObject ret = new JSObject();
        if (id < 0) {
            ret.put("state", "none");
            call.resolve(ret);
            return;
        }

        DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            call.reject("Le gestionnaire de téléchargement d'Android est indisponible.");
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dm.query(new DownloadManager.Query().setFilterById(id));
            if (cursor == null || !cursor.moveToFirst()) {
                prefs().edit().remove(PREF_DOWNLOAD_ID).apply();
                ret.put("state", "none");
                call.resolve(ret);
                return;
            }

            int status = readInt(cursor, DownloadManager.COLUMN_STATUS);
            long soFar = readLong(cursor, DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            long total = readLong(cursor, DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int reason = readInt(cursor, DownloadManager.COLUMN_REASON);

            if (total > 0 && soFar >= 0) ret.put("percent", (int) (soFar * 100 / total));

            if (status == DownloadManager.STATUS_FAILED) {
                prefs().edit().remove(PREF_DOWNLOAD_ID).apply();
                ret.put("state", "failed");
                ret.put("reason", failureReason(reason));
            } else if (status == DownloadManager.STATUS_PAUSED) {
                ret.put("state", "paused");
                ret.put("reason", pauseReason(reason));
            } else if (status == DownloadManager.STATUS_PENDING) {
                ret.put("state", "pending");
            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                prefs().edit().remove(PREF_DOWNLOAD_ID).apply();
                File dest = destinationFile();
                if (!dest.exists() || dest.length() == 0) {
                    ret.put("state", "failed");
                    ret.put("reason", "le fichier téléchargé est vide");
                } else {
                    launchInstaller(dest);
                    ret.put("state", "installing");
                }
            } else {
                ret.put("state", "running");
            }
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Suivi du téléchargement impossible : " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @PluginMethod
    public void cancelDownload(PluginCall call) {
        long id = prefs().getLong(PREF_DOWNLOAD_ID, -1);
        if (id >= 0) {
            DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) dm.remove(id);
            prefs().edit().remove(PREF_DOWNLOAD_ID).apply();
        }
        call.resolve();
    }

    private void launchInstaller(File dest) {
        Context ctx = getContext();
        Uri apkUri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", dest);
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(installIntent);
    }

    private File destinationFile() {
        return new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private long installedAt() {
        try {
            PackageInfo info = getContext().getPackageManager()
                .getPackageInfo(getContext().getPackageName(), 0);
            return info.lastUpdateTime;
        } catch (Exception e) {
            return 0L;
        }
    }

    private long parseIso8601(String value) {
        if (value == null || value.length() == 0) return 0L;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = fmt.parse(value);
            return parsed != null ? parsed.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private int readInt(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        return idx >= 0 ? cursor.getInt(idx) : -1;
    }

    private long readLong(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        return idx >= 0 ? cursor.getLong(idx) : -1L;
    }

    private String failureReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_INSUFFICIENT_SPACE: return "espace de stockage insuffisant";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND: return "stockage indisponible";
            case DownloadManager.ERROR_FILE_ERROR: return "erreur d'écriture du fichier";
            case DownloadManager.ERROR_HTTP_DATA_ERROR: return "connexion interrompue";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS: return "trop de redirections";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: return "réponse inattendue du serveur";
            case DownloadManager.ERROR_CANNOT_RESUME: return "reprise impossible";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS: return "le fichier existe déjà";
            default: return "code " + reason;
        }
    }

    private String pauseReason(int reason) {
        switch (reason) {
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK: return "en attente de réseau";
            case DownloadManager.PAUSED_WAITING_TO_RETRY: return "nouvelle tentative en cours";
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI: return "en attente du Wi-Fi";
            default: return "en pause";
        }
    }
}
