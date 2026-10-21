package com.example.fyp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared remote-image fetching for recipe thumbnails and dialogs.
 *
 * <p>Every request runs on a small fixed daemon pool (never one raw
 * thread per image), carries explicit connect and read timeouts, and
 * reports failure as a null result instead of throwing: a missing or
 * broken image must never block a recipe from being shown or saved.
 * Callbacks arrive on a background thread, so callers must hand results
 * to the UI through their own lifecycle-aware post.
 */
public final class RemoteImageLoader {

    static final int CONNECT_TIMEOUT_MS = 5000;
    static final int READ_TIMEOUT_MS = 5000;

    public interface Callback {
        /** Called with the response bytes, or null on any failure. */
        void onBytes(byte[] bytesOrNull);
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2,
            runnable -> {
                Thread thread = new Thread(runnable, "remote-image-loader");
                thread.setDaemon(true);
                return thread;
            });

    private RemoteImageLoader() {
    }

    /**
     * Fetches an image URL asynchronously. A null/empty URL completes
     * immediately with a null result on the calling thread.
     */
    public static void fetchAsync(String urlString, Callback callback) {
        if (urlString == null || urlString.isEmpty()) {
            callback.onBytes(null);
            return;
        }
        POOL.execute(() -> callback.onBytes(fetch(urlString)));
    }

    /**
     * Synchronous fetch used by the pool and by tests. Returns the body
     * for 2xx responses and null for malformed URLs, wrong protocols,
     * non-success statuses, timeouts, connection failures, and empty
     * bodies. Never throws.
     */
    static byte[] fetch(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            int status = connection.getResponseCode();
            if (status < 200 || status > 299) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                in.close();
            }
            return out.size() == 0 ? null : out.toByteArray();
        } catch (IOException | RuntimeException e) {
            // Malformed URL, unsupported protocol, refused connection,
            // timeout, or interrupted read: callers get a null image.
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
