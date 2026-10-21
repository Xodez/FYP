package com.example.fyp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Offline unit tests for the shared remote-image fetch boundary. Error
 * handling is exercised with controlled failures (malformed urls and
 * refused loopback connections), never a live external request.
 */
public class RemoteImageLoaderTest {

    @Test
    public void timeoutsAreExplicitAndReasonable() {
        assertEquals(5000, RemoteImageLoader.CONNECT_TIMEOUT_MS);
        assertEquals(5000, RemoteImageLoader.READ_TIMEOUT_MS);
    }

    @Test
    public void malformedUrlYieldsNullInsteadOfThrowing() {
        assertNull(RemoteImageLoader.fetch("not a url"));
        assertNull(RemoteImageLoader.fetch("http://"));
    }

    @Test
    public void refusedConnectionYieldsNull() {
        // Port 9 (discard) on loopback: fast, controlled connect failure.
        assertNull(RemoteImageLoader.fetch("http://127.0.0.1:9/image.jpg"));
    }

    @Test
    public void unsupportedProtocolYieldsNull() {
        // A protocol with no HttpURLConnection handler must not crash the
        // background loader thread.
        assertNull(RemoteImageLoader.fetch("ftp://127.0.0.1:9/image.jpg"));
    }

    @Test
    public void emptyAndNullUrlsCompleteWithNullSynchronously() {
        final byte[][] captured = new byte[1][];
        RemoteImageLoader.fetchAsync(null, bytes -> captured[0] = bytes);
        assertNull("null url completes with null", captured[0]);
        RemoteImageLoader.fetchAsync("", bytes -> captured[0] = bytes);
        assertNull("empty url completes with null", captured[0]);
    }
}
