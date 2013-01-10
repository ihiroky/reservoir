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

    static final int BITS_PER_BYTE = 7;
    static final int BITS_BYTE1 = BITS_PER_BYTE;
    static final int BITS_BYTE2 = BITS_BYTE1 + BITS_PER_BYTE;
    static final int BITS_BYTE3 = BITS_BYTE2 + BITS_PER_BYTE;
    static final int BITS_BYTE4 = BITS_BYTE3 + BITS_PER_BYTE;

    static final int BITS_END = 0x80;
    static final int BITS_MASK = 0x7F;
    static final int BITS_UPPER_25 = 0xFFFFFF80;
    static final int BITS_8 = 0xFF;

    static final int BYTE_LESS_THAN_ASCII = 0x80;
    static final int NULL = 0xFF;

    private static void encode(final int value, byte[] bytes) {
        int v = value;
        int i = 0;
        for (; (v & BITS_UPPER_25) != 0; v >>>= BITS_PER_BYTE) {
            bytes[i++] = (byte) (v & BITS_MASK);
        }
        bytes[i] = (byte) (v | BITS_END);
    }

    static byte[] asBytes(int i) {
        final int neededBlocks;
        if (i < 0 || i >= 1 << BITS_BYTE4) {
            neededBlocks = 5;
        } else if (i < (1 << BITS_BYTE1)) {
            // shortcut
            byte[] result = new byte[1];
            result[0] = (byte) (i | BITS_END);
            return result;
        } else if (i < (1 << BITS_BYTE2)) {
            neededBlocks = 2;
        } else if (i < (1 << BITS_BYTE3)) {
            neededBlocks = 3;
        } else { // if (i < (1 << BITS_BYTE4)) {
            neededBlocks = 4;
        }
        byte[] result = new byte[neededBlocks];
        encode(i, result);
        return result;
    }

    static int asInt(byte[] bytes, int offset) {
        int read;
        int result = 0;
        int shift = 0;
        for (byte aByte : bytes) {
            read = aByte & BITS_8;
            if ((read & BITS_END) != 0) {
                result |= (read & BITS_MASK) << shift;
                return result;
            }
            result |= read << shift;
            shift += BITS_PER_BYTE;
        }
        throw new IllegalArgumentException("no end bit is found.");
    }

    static int bytesLength(int i) {
        if (i < 0 || i >= 1 << BITS_BYTE4) {
            return 5;
        } else if (i < (1 << BITS_BYTE1)) {
            return 1;
        } else if (i < (1 << BITS_BYTE2)) {
            return 2;
        } else if (i < (1 << BITS_BYTE3)) {
            return 3;
        } else { // if (i < (1 << BITS_BYTE4)) {
            return 4;
        }
    }

    static class EncoderOutputStream extends FilterOutputStream {

        EncoderOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void writeString(String s) throws IOException {
            writeString(s, false);
        }

        public void writeAscii(String ascii) throws IOException {
            writeString(ascii, true);
        }

        private void writeString(String s, boolean ascii) throws IOException {
            char c;
            if (s == null) {
                writeInt(1);
                out.write(NULL);
                return;
            } else if (s.length() == 1 && (c = s.charAt(0)) < BYTE_LESS_THAN_ASCII) {
                writeInt(1);
                out.write(c);
                return;
            }

            int length = s.length();
            if (ascii) {
                writeInt(length);
                for (int i = 0; i < length; i++) {
                    out.write(s.charAt(i));
                }
            } else {
                writeInt(length * 2);
                for (int i = 0; i < length; i++) {
                    c = s.charAt(i);
                    out.write(c >>> 8);
                    out.write(c);
                }
            }
        }

        public void writeInt(final int i) throws IOException {
            OutputStream out = this.out;
            int bits = i;
            for (; (bits & BITS_UPPER_25) != 0; bits >>>= BITS_PER_BYTE) {
                out.write(bits & BITS_MASK);
            }
            out.write(bits | BITS_END);
        }
    }

    static class DecoderInputStream extends FilterInputStream {

        private static final String[] CACHED_CHAR;

        static {
            String[] a = new String[BYTE_LESS_THAN_ASCII];
            for (int i = 0; i < a.length; i++) {
                a[i] = String.valueOf((char) i);
            }
            CACHED_CHAR = a;
        }

        DecoderInputStream(InputStream inputStream) {
            super(inputStream);
        }

        public String readString() throws IOException {
            return readString(false);
        }

        public String readAscii() throws IOException {
            return readString(true);
        }

        private String readString(boolean ascii) throws IOException {
            int length = readInt();
            if (length == 0) {
                return "";
            } else if (length == 1) {
                int b = in.read() & 0xFF;
                return (b == NULL) ? null : ((b < BYTE_LESS_THAN_ASCII) ? CACHED_CHAR[b] : String.valueOf((char) b));
            }

            char[] array;
            if (ascii) {
                array = new char[length];
                for (int i = 0; i < length; i++) {
                    array[i] = (char) (in.read() & 0xFF);
                }
            } else {
                int charLength = length / 2;
                array = new char[charLength];
                for (int i = 0; i < charLength; i++) {
                    array[i] = (char) ((in.read() & 0xFF) << 8 | (in.read() & 0xFF));
                }
            }
            return new String(array);
        }

        public int readInt() throws IOException {
            int read;
            int result = 0;
            int shift = 0;
            while ((read = in.read()) != -1) {
                if ((read & BITS_END) != 0) {
                    break;
                }
                result |= read << shift;
                shift += BITS_PER_BYTE;
            }
            if (read == -1) {
                throw new EOFException();
            }
            result |= (read & BITS_MASK) << shift;
            return result;
        }
    }
}
