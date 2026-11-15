package com.umeed.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stores and retrieves files by key, for example "donations/2026/11/abc123.jpg".
 * Implementations decide where the bytes actually live.
 */
public interface FileStorage {

    /** Saves the content under the given key. Fails if the key already exists. */
    void store(String key, InputStream content) throws IOException;

    /** Opens the stored file for reading. The caller must close the stream. */
    InputStream open(String key) throws IOException;

    /** Removes the stored file. Does nothing if it does not exist. */
    void delete(String key) throws IOException;

    /** Returns the address a client can use to download the file. */
    String urlFor(String key);
}
