package net.ihiroky.reservoir.accessor;

import java.io.File;

/**
 * Created on 12/10/31, 18:32
 *
 * @author Hiroki Itoh
 */
public class FileInfo {
    File file;
    long size;
    Mode mode;

    public enum Mode {
        READ_WRITE("rw"),
        SYNC("rws"),
        DIRECT_SYNC("rwd"),;

        final String value;

        private Mode(String value) {
            this.value = value;
        }
    }

    FileInfo() {
        mode = Mode.READ_WRITE;
    }

    public FileInfo(String path, long size) {
        this(path, size, Mode.READ_WRITE);
    }

    public FileInfo(String path, long size, Mode mode) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("path is null or empty.");
        }
        setPath(path);
        this.size = size;
        this.mode = (mode != null) ? mode : Mode.READ_WRITE;
        validate();
    }

    void setPath(String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("path is null or empty.");
        }
        this.file = new File(path);
    }

    void setMode(String mode) {
        this.mode = Mode.valueOf(mode);
    }

    void validate() {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive.");
        }
        if (file == null || !file.getParentFile().isDirectory()) {
            throw new RuntimeException("invalid path : " + file);
        }
    }
}
