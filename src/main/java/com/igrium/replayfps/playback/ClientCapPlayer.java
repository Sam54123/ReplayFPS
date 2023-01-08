package com.igrium.replayfps.playback;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.igrium.replayfps.clientcap.ClientCapFile;
import com.igrium.replayfps.clientcap.ClientCapFile.Chunk;
import com.igrium.replayfps.util.ManagedInputStream;
import com.igrium.replayfps.util.ManagedInputStream.InputStreamSupplier;

public class ClientCapPlayer implements Closeable {

    /**
     * The maximum amount of chunks that can be stored in the cache at a given time.
     */
    public static final int CACHE_SIZE = 512;

    /**
     * Where to find all the chunks in the file.
     */
    private final List<Long> chunks = new ArrayList<>();

    // /**
    //  * A cache of parsed chunks.
    //  */
    // private final Map<Integer, Chunk> chunkCache = new WeakHashMap<>();
    private final LoadingCache<Integer, Chunk> chunkCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build(new CacheLoader<>() {

                @Override
                public Chunk load(Integer index) throws Exception {
                    return loadChunk(index);
                }

            });

    protected final InputStreamSupplier inputStreamSupplier;
    protected ManagedInputStream inputStream;

    private ClientCapFile file;

    public ClientCapPlayer(InputStreamSupplier inputStreamSupplier) {
        this.inputStreamSupplier = inputStreamSupplier;
    }

    public ClientCapFile getFile() {
        return file;
    }

    /**
     * The number of chunks in the file.
     */
    public int numChunks() {
        return chunks.size();
    }

    /**
     * The length of the file in milliseconds.
     */
    public int getLength() {
        return chunks.size() * file.getChunkLength();
    }

    /**
     * Read the file metadata and prepare for playback.
     * @throws IOException If an IO exception occurs.
     */
    public void beginPlayback() throws IOException {
        // CountingInputStream inputStream = new CountingInputStream(new BufferedInputStream(inputStreamSupplier.get()));
        inputStream = new ManagedInputStream(inputStreamSupplier);
        file = ClientCapFile.readHeader(inputStream);

        DataInputStream data = new DataInputStream(inputStream);

        // EOFException breaks us out of the loop
        try {
            long offset;
            int length;
            while (true) {
                offset = inputStream.getHead();
                length = data.readUnsignedShort();
                data.skipBytes(length);

                chunks.add(offset);
            }
        } catch (EOFException e) {}
        
        inputStream.close();
    }

    public Chunk getChunk(int index) {
        if (index < 0 || index >= chunks.size()) {
            throw new IndexOutOfBoundsException(index);
        }
        return chunkCache.getUnchecked(index);
    }
    
    protected Chunk loadChunk(int index) throws IOException {
        long offset = chunks.get(index);
        inputStream.jumpTo(offset);

        return file.readChunk(inputStream);
    }
    
    /**
     * Cache a number of chunks in preparation for playback.
     * @param index Chunk index to start at.
     * @param amount The total amount of chunks to cache.
     */
    public void precache(int index, int amount) {
        int endIndex = Math.min(index + amount, chunks.size());
        for (int i = index; i < endIndex; i++) {
            try {
                chunkCache.get(i);
            } catch (ExecutionException e) {
                LogManager.getLogger().error("Error precaching animation chunk "+index, e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) inputStream.close();
    }

    
}
