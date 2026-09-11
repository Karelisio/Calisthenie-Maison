package com.karelisio.calisthenie;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
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

@CapacitorPlugin(name = "AppUpdater")
public class AppUpdaterPlugin extends Plugin {

    private static final String RELEASE_API_URL =
        "https://api.github.com/repos/Karelisio/Calisthenie-Maison/releases/tags/apk-material-latest";
    private static final String PREFS = "app_updater";
    private static final String PREF_RELEASE_ID = "installed_release_id";

    private long downloadId = -1;
    private PluginCall pendingInstallCall;

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
                    call.reject("HTTP " + code);
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
                String remoteId = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url");
                            remoteId = asset.optString("digest", asset.optString("updated_at"));
                            break;
                        }
                    }
                }

                if (apkUrl == null || remoteId == null) {
                    call.reject("Aucun APK trouvé dans la dernière publication.");
                    return;
                }

                SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String currentId = prefs.getString(PREF_RELEASE_ID, null);
                boolean available = currentId == null || !currentId.equals(remoteId);

                JSObject ret = new JSObject();
                ret.put("available", available);
                ret.put("downloadUrl", apkUrl);
                ret.put("releaseId", remoteId);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Vérification impossible : " + e.getMessage(), e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    @PluginMethod
    public void downloadAndInstall(PluginCall call) {
        String url = call.getString("url");
        String releaseId = call.getString("releaseId");
        if (url == null) {
            call.reject("URL manquante");
            return;
        }

        Context context = getContext();
        final File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        final File dest = new File(dir, "update.apk");
        if (dest.exists()) dest.delete();

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDestinationUri(Uri.fromFile(dest));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("Calisthénie Maison – mise à jour");

        downloadId = dm.enqueue(request);
        pendingInstallCall = call;
        call.setKeepAlive(true);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PREF_RELEASE_ID, releaseId).apply();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;
                ctx.unregisterReceiver(this);

                try {
                    Uri apkUri = FileProvider.getUriForFile(
                        ctx, ctx.getPackageName() + ".fileprovider", dest);

                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(installIntent);

                    if (pendingInstallCall != null) {
                        JSObject ret = new JSObject();
                        ret.put("started", true);
                        pendingInstallCall.resolve(ret);
                        pendingInstallCall = null;
                    }
                } catch (Exception e) {
                    if (pendingInstallCall != null) {
                        pendingInstallCall.reject("Installation impossible : " + e.getMessage());
                        pendingInstallCall = null;
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }
}
