package net.ihiroky.reservoir.coder;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides for streams and utility methods to support encoding / decoding primitives (int) and string.
 * These are faster or smaller than {@code java.io.DataInputStream} or {@code java.io.DataOutputStream}.
 *
 * An integer coding is the variable byte code, the lower bytes data is put in the lower addresses
 * and the higher bytes is put in the higher address with end judge bits in the byte array or streams.
 *
 * A string coding is the simple Java bytes, single char is encoded to two bytes and vice versa. If a string
 * consists of ascii characters, single char is encoded to single byte and vice versa using {@code writeAscii(String)}
 * and {@code readAscii()}. When a string to be coded is contains a lot of ascii characters and a few non ascii
 * characters, {@link net.ihiroky.reservoir.coder.CompressionSupport} with low compression level is useful to
 * suppress encoded bytes size.
 *
 * @author Hiroki Itoh
 */
public class CoderStream {

    private static final int BITS_PER_BYTE = 7;
    private static final int BITS_BYTE1 = BITS_PER_BYTE;
    private static final int BITS_BYTE2 = BITS_BYTE1 + BITS_PER_BYTE;
    private static final int BITS_BYTE3 = BITS_BYTE2 + BITS_PER_BYTE;
    private static final int BITS_BYTE4 = BITS_BYTE3 + BITS_PER_BYTE;

    private static final int BITS_END = 0x80;
    private static final int BITS_MASK = 0x7F;
    private static final int BITS_8 = 0xFF;

    private static final int MAX_ASCII = 0x7F;
    private static final int NULL = 0xFF;

    /**
     * Encodes a specified int to a specified byte array.
     *
     * @param value integer to be encoded
     * @param bytes byte array which is encoded to
     */
    private static void encode(final int value, byte[] bytes) {
        int v = value;
        int i = 0;
        for (; (v & ~BITS_MASK) != 0; v >>>= BITS_PER_BYTE) {
            bytes[i++] = (byte) (v & BITS_MASK);
        }
        bytes[i] = (byte) (v | BITS_END);
    }

    /**
     * Returns encoded byte array of a specified int. The byte array length has the minimum length to store
     * the int value.
     *
     * @param i integer to be encoded
     * @return encoded byte array of a specified int
     */
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

    /**
     * Returns int value from a specified byte array. The int value is read from a specified offset in the byte array.
     *
     * @param bytes a byte array to be decoded
     * @param offset an offset in the {@code bytes}
     * @return decoded int value
     */
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

    /**
     * Returns an encoded length of a specified int value .
     *
     * @param i integer to be checked its length
     * @return an encoded length of a specified int value .
     */
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

    /**
     * Provides for streaming methods to encode primitives (int) and string.
     */
    static class EncoderOutputStream extends FilterOutputStream {

        /**
         * Constructs a new {@code EncoderOutputStream}.
         *
         * @param outputStream a base {@code OutputStream}
         */
        EncoderOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        /**
         * Writes a string to the base stream. The {@code null} is acceptable.
         *
         * @param s a string to be written
         * @throws IOException
         */
        public void writeString(String s) throws IOException {
            writeString(s, false);
        }

        /**
         * Write a ASCII string to the base stream. The {@code null} is acceptable. If the argument {@code ascii}
         * contains non-ascii characters, the higher byte {@code char} is dropped.
         *
         * @param ascii an ASCII string to be written
         * @throws IOException
         */
        public void writeAscii(String ascii) throws IOException {
            writeString(ascii, true);
        }

        /**
         *
         * @param s a string to be written
         * @param ascii true if {@code s} is an ascii string
         * @throws IOException
         */
        private void writeString(String s, boolean ascii) throws IOException {
            char c;
            if (s == null) {
                writeInt(1);
                out.write(NULL);
                return;
            } else if (s.length() == 1 && (c = s.charAt(0)) <= MAX_ASCII) {
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

        /**
         * Writes int value to the base stream.
         *
         * @param i an integer to be written
         * @throws IOException
         */
        public void writeInt(final int i) throws IOException {
            OutputStream out = this.out;
            int bits = i;
            for (; (bits & ~BITS_MASK) != 0; bits >>>= BITS_PER_BYTE) {
                out.write(bits & BITS_MASK);
            }
            out.write(bits | BITS_END);
        }
    }

    /**
     * Provides for streaming methods to decode primitives (int) and string.
     */
    static class DecoderInputStream extends FilterInputStream {

        private static final String[] CACHED_CHAR;

        static {
            String[] a = new String[MAX_ASCII + 1];
            for (int i = 0; i < a.length; i++) {
                a[i] = String.valueOf((char) i);
            }
            CACHED_CHAR = a;
        }

        /**
         * Constructs a new DecoderInputStream.
         * @param inputStream a base input stream
         */
        DecoderInputStream(InputStream inputStream) {
            super(inputStream);
        }

        /**
         * Reads a string from the base stream.
         *
         * @return a string
         * @throws EOFException if base stream reaches the end before read the entire string
         * @throws IOException if base stream has some error
         */
        public String readString() throws IOException {
            return readString(false);
        }

        /**
         * Reads an ascii string from the base stream.
         * @return an ascii string
         * @throws EOFException if base stream reaches the end before read the entire string
         * @throws IOException if base stream has some error
         */
        public String readAscii() throws IOException {
            return readString(true);
        }

        /**
         *
         * @param ascii true if a stream to be read is ascii
         * @return a string
         * @throws EOFException if base stream reaches the end before read the entire string
         * @throws IOException if base stream has some error
         */
        private String readString(boolean ascii) throws IOException {
            int length = readInt();
            if (length == 0) {
                return "";
            } else if (length == 1) {
                int b = in.read();
                return (b == NULL) ? null : ((b <= MAX_ASCII) ? CACHED_CHAR[b] : String.valueOf((char) b));
            }

            char[] array;
            if (ascii) {
                array = new char[length];
                for (int i = 0; i < length; i++) {
                    array[i] = (char) in.read();
                }
            } else {
                int charLength = length / 2;
                array = new char[charLength];
                for (int i = 0; i < charLength; i++) {
                    array[i] = (char) (in.read() << 8 | in.read());
                }
            }
            return new String(array);
        }

        /**
         * Reads {@code int} value from the base stream
         *
         * @return {@code int} value
         * @throws EOFException if the base stream reaches the end before the entire integer
         * @throws IOException if the base stream has some error.
         */
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
