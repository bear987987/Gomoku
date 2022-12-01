package worker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import state.ConnectState;

public interface SyncWriter {
    /**
     * Use {@code BufferedWriter} with synchronized block, it can be written in
     * {@code ConnectState}, {@code StreamReactState} and {@code Object}
     * 
     * @param bw           source writer
     * @param connectState {@code ConnectState}
     * @param reactState   {@code StreamReactState}
     * @param elseOutputs  other object need to be written
     * 
     * @throws IOException {@link BufferedWriter#write(String)} or
     *                     {@code StreamReactState} not match {@code ConnectState}
     */
    default void write(final BufferedWriter bw, Integer connectState, String reactState, final Object... elseOutputs)
            throws IOException {
        synchronized (bw) {
            if (connectState != null)
                bw.write(connectState);
            if (reactState != null) {
                if (connectState != ConnectState.CLIENT_REACT_STREAM
                        && connectState != ConnectState.MULTICAST_REACT_STREAM)
                    throw new IOException("State code " + connectState + " is not react stream!");
                bw.write(reactState + "\r\n");
            }
            for (Object output : elseOutputs)
                bw.write(output + "\r\n");
            bw.flush();
        }
    }

    /**
     * Use {@code BufferedWriter} with synchronized block, it can be written in
     * {@code ConnectState}, {@code StreamReactState} and {@code Object}
     * 
     * @param sk           source socket
     * @param connectState {@code ConnectState}
     * @param reactState   {@code StreamReactState}
     * @param elseOutputs  other object need to be written
     * 
     * @throws IOException {@link BufferedWriter#write(String)} or
     *                     {@code StreamReactState} not match {@code ConnectState}
     */
    default void write(final Socket sk, Integer connectState, String reactState, final Object... elseOutputs)
            throws IOException {
        final BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(sk.getOutputStream(), StandardCharsets.UTF_8));
        write(bw, connectState, reactState, elseOutputs);
    }
}