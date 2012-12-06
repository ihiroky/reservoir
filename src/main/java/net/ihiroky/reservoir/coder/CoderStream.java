package net.ihiroky.reservoir.coder;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created on 12/10/17, 12:14
 *
 * @author Hiroki Itoh
 */
public class CoderStream {

    static final int BITS_1BYTE = 7;
    static final int BITS_2BYTES = 14;
    static final int BITS_3BYTES = 21;
    static final int BITS_4BYTES = 28;

    static final int FOUR_BYTES_BITS = 28;
    static final int BYTE_MARK2 = 0x80;
    static final int BYTE_MARK3 = 0xC0;
    static final int BYTE_MARK4 = 0xE0;
    static final int BYTE_MARK5 = 0xF0;
    static final int NULL = 0xFF;

    static byte[] asBytes(int i) {
        if (i < 0 || i >= 1 << 28) {
            byte[] result = new byte[5];
            result[0] = (byte) BYTE_MARK5;
            result[1] = (byte) ((i >>> 24) & 0xFF);
            result[2] = (byte) ((i >>> 16) & 0xFF);
            result[3] = (byte) ((i >>> 8) & 0xFF);
            result[4] = (byte) ((i) & 0xFF);
            return result;
        }
        if (i < (1 << BITS_1BYTE)) {
            byte[] result = new byte[1];
            result[0] = (byte) i;
            return result;
        }
        if (i < (1 << BITS_2BYTES)) {
            byte[] result = new byte[2];
            result[0] = (byte) (BYTE_MARK2 | ((i >>> 8) & 0xFF));
            result[1] = (byte) (i & 0xFF);
            return result;
        }
        if (i < (1 << BITS_3BYTES)) {
            byte[] result = new byte[3];
            result[0] = (byte) (BYTE_MARK3 | ((i >>> 16) & 0xFF));
            result[1] = (byte) ((i >>> 8) & 0xFF);
            result[2] = (byte) ((i) & 0xFF);
            return result;
        }
        //if (i < (1 << BITS_4BYTES)) {
        byte[] result = new byte[4];
        result[0] = (byte) (BYTE_MARK4 | ((i >>> 24) & 0xFF));
        result[1] = (byte) ((i >>> 16) & 0xFF);
        result[2] = (byte) ((i >>> 8) & 0xFF);
        result[3] = (byte) ((i) & 0xFF);
        return result;
    }

    static int asInt(byte[] bytes, int offset) {
        int first = bytes[offset] & 0xFF;
        if (first < BYTE_MARK2) {
            return first & ~BYTE_MARK2;
        } else if (first < BYTE_MARK3) {
            return ((first & ~BYTE_MARK3) << 8) | (bytes[++offset] & 0xFF);
        } else if (first < BYTE_MARK4) {
            return ((first & ~BYTE_MARK4) << 16)
                    | ((bytes[++offset] & 0xFF) << 8) | (bytes[++offset] & 0xFF);
        } else if (first < BYTE_MARK5) {
            return ((first & ~BYTE_MARK5) << 24)
                    | ((bytes[++offset] & 0xFF) << 16) | ((bytes[++offset] & 0xFF) << 8)
                    | (bytes[++offset] & 0xFF);
        } else {
            return ((bytes[++offset] & 0xFF) << 24) | ((bytes[++offset] & 0xFF) << 16)
                    | ((bytes[++offset] & 0xFF) << 8) | (bytes[++offset] & 0xFF);
        }
    }

    static int bytesLength(int i) {
        if (i < 0 || i >= 1 << BITS_4BYTES) {
            return 5;
        } else if (i < (1 << BITS_1BYTE)) {
            return 1;
        } else if (i < (1 << BITS_2BYTES)) {
            return 2;
        } else if (i < (1 << BITS_3BYTES)) {
            return 3;
        } else { // if (i < (1 << BITS_4BYTES)) {
            return 4;
        }
    }

    static class EncoderOutputStream extends FilterOutputStream {

        EncoderOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void writeString(String s) throws IOException {
            char c;
            if (s == null) {
                writeInt(1);
                out.write(NULL);
                return;
            } else if (s.length() == 1 && (c = s.charAt(0)) < BYTE_MARK2) {
                writeInt(1);
                out.write(c);
                return;
            }

            int length = s.length();
            int increment = length * 2;
            writeInt(increment);

            for (int i = 0, j; i < length; i++) {
                c = s.charAt(i);
                out.write(c >>> 8);
                out.write(c);
            }
        }

        public void writeInt(int i) throws IOException {
            OutputStream out = this.out;
            if (i < 0 || i >= 1 << BITS_4BYTES) {
                out.write(BYTE_MARK5);
                out.write((i >>> 24) & 0xFF);
                out.write((i >>> 16) & 0xFF);
                out.write((i >>> 8) & 0xFF);
                out.write((i) & 0xFF);
            } else if (i < (1 << BITS_1BYTE)) {
                out.write(i);
            } else if (i < (1 << BITS_2BYTES)) {
                out.write(BYTE_MARK2 | ((i >>> 8) & 0xFF));
                out.write(i & 0xFF);
            } else if (i < (1 << BITS_3BYTES)) {
                out.write(BYTE_MARK3 | ((i >>> 16) & 0xFF));
                out.write((i >>> 8) & 0xFF);
                out.write((i) & 0xFF);
            } else { // if (i < (1 << BITS_4BYTES)) {
                out.write(BYTE_MARK4 | ((i >>> 24) & 0xFF));
                out.write((i >>> 16) & 0xFF);
                out.write((i >>> 8) & 0xFF);
                out.write((i) & 0xFF);
            }
        }
    }

    static class DecoderInputStream extends FilterInputStream {

        private static final String[] CACHED_CHAR;

        static {
            String[] a = new String[BYTE_MARK2];
            for (int i = 0; i < a.length; i++) {
                a[i] = String.valueOf((char) i);
            }
            CACHED_CHAR = a;
        }

        DecoderInputStream(InputStream inputStream) {
            super(inputStream);
        }

        public String readString() throws IOException {
            int length = readInt();
            if (length == -1) {
                throw new EOFException();
            }
            if (length == 0) {
                return "";
            } else if (length == 1) {
                int b = in.read() & 0xFF;
                return (b == NULL) ?
                        null : ((b < BYTE_MARK2) ? CACHED_CHAR[b] : String.valueOf((char) b));
            }

            int charLength = length / 2;
            char[] array = new char[charLength];
            for (int i = 0; i < charLength; i++) {
                array[i] = (char) ((in.read() & 0xFF) << 8 | (in.read() & 0xFF));
            }
            return new String(array);
        }

        public int readInt() throws IOException {
            int first = in.read();
            if (first == -1) {
                throw new EOFException();
            }

            first &= 0xFF;
            if (first < BYTE_MARK2) {
                return first & ~BYTE_MARK2;
            } else if (first < BYTE_MARK3) {
                return ((first & ~BYTE_MARK3) << 8) | (in.read() & 0xFF);
            } else if (first < BYTE_MARK4) {
                return ((first & ~BYTE_MARK4) << 16) | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else if (first < BYTE_MARK5) {
                return ((first & ~BYTE_MARK5) << 24) | ((in.read() & 0xFF) << 16)
                        | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else {
                return ((in.read() & 0xFF) << 24) | ((in.read() & 0xFF) << 16)
                        | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            }
        }
    }
}
