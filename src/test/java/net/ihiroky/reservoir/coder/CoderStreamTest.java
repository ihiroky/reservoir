package net.ihiroky.reservoir.coder;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class CoderStreamTest {

    private byte[] b(int... a) {
        byte[] b = new byte[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) a[i];
        }
        return b;
    }

    @Test
    public void testAsBytes() throws Exception {
        byte[] data;

        data = CoderStream.asBytes(0x00);
        assertThat(data, is(b(0x80)));
        data = CoderStream.asBytes(0x7E);
        assertThat(data, is(b(0xFE)));
        data = CoderStream.asBytes(0x7F);
        assertThat(data, is(b(0xFF)));

        data = CoderStream.asBytes(0x80); // 1000 0000 / 8 bits
        assertThat(data, is(b(0x00, 0x81)));
        data = CoderStream.asBytes(0x3FFF); //  11 1111 1111 1111 / 14 bits
        assertThat(data, is(b(0x7F, 0xFF)));

        data = CoderStream.asBytes(0x4000); // 100 0000 0000 0000 / 15 bits
        assertThat(data, is(b(0x00,0x00,0x81)));
        data = CoderStream.asBytes(0x1FFFFF); //  1 1111 1111 1111 1111 1111 / 21 bits
        assertThat(data, is(b(0x7F,0x7F,0xFF)));

        data = CoderStream.asBytes(0x200000); // 10 0000  0000 0000 0000 0000 / 22 bits
        assertThat(data, is(b(0x00, 0x00, 0x00, 0x81)));
        data = CoderStream.asBytes(0xFFFFFFF);  //   1111 1111 1111  1111 1111 1111 1111 / 28 bits
        assertThat(data, is(b(0x7F, 0x7F, 0x7F, 0xFF)));

        data = CoderStream.asBytes(0x10000000); // 1 0000 0000 0000  0000 0000 0000 0000 / 29 bits
        assertThat(data, is(b(0x00, 0x00, 0x00, 0x00, 0x81)));
        data = CoderStream.asBytes(0xFFFFFFFF); //  1111 1111 1111 1111  1111 1111 1111 1111 / 32 bits
        assertThat(data, is(b(0x7F, 0x7F, 0x7F, 0x7F, 0x8F)));
    }

    @Test
    public void testAsInt() throws Exception {
        int value;

        value = CoderStream.asInt(b(0x80), 0);
        assertThat(value, is(0));
        value = CoderStream.asInt(b(0xFF), 0);
        assertThat(value, is(0x7F));

        value = CoderStream.asInt(b(0x00, 0x81), 0);
        assertThat(value, is(0x80));
        value = CoderStream.asInt(b(0x7F, 0xFF), 0);
        assertThat(value, is(0x3FFF));

        value = CoderStream.asInt(b(0x00, 0x00, 0x81), 0);
        assertThat(value, is(0x4000));
        value = CoderStream.asInt(b(0x7F, 0x7F, 0xFF), 0);
        assertThat(value, is(0x1FFFFF));

        value = CoderStream.asInt(b(0x00, 0x00, 0x00, 0x81), 0);
        assertThat(value, is(0x200000));
        value = CoderStream.asInt(b(0x7F, 0x7F, 0x7F, 0xFF), 0);
        assertThat(value, is(0xFFFFFFF));

        value = CoderStream.asInt(b(0x00, 0x00, 0x00, 0x00, 0x81), 0);
        assertThat(value, is(0x10000000));
        value = CoderStream.asInt(b(0x7F, 0x7F, 0x7F, 0x7F, 0x8F), 0);
        assertThat(value, is(0xFFFFFFFF));
    }

    @Test
    public void testBytesLength() throws Exception {
        int length;

        length = CoderStream.bytesLength(0);
        assertThat(length, is(1));
        length = CoderStream.bytesLength(0x7F);
        assertThat(length, is(1));

        length = CoderStream.bytesLength(0x80);
        assertThat(length, is(2));
        length = CoderStream.bytesLength(0x3FFF);
        assertThat(length, is(2));

        length = CoderStream.bytesLength(0x4000);
        assertThat(length, is(3));
        length = CoderStream.bytesLength(0x1FFFFF);
        assertThat(length, is(3));

        length = CoderStream.bytesLength(0x200000);
        assertThat(length, is(4));
        length = CoderStream.bytesLength(0xFFFFFFF);
        assertThat(length, is(4));

        length = CoderStream.bytesLength(0x1FFFFFFF);
        assertThat(length, is(5));
        length = CoderStream.bytesLength(0xFFFFFFFF);
        assertThat(length, is(5));
    }

    private void assertWriteInt(int value, byte[] expected) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CoderStream.EncoderOutputStream outputStream = new CoderStream.EncoderOutputStream(result);
        outputStream.writeInt(value);
        outputStream.flush();
        assertThat(result.toByteArray(), is(expected));
    }

    @Test
    public void testWriteInt() throws Exception {
        assertWriteInt(0x00, b(0x80));
        assertWriteInt(0x7F, b(0xFF));
        assertWriteInt(0x80,   b(0x00, 0x81));
        assertWriteInt(0x3FFF, b(0x7F, 0xFF));
        assertWriteInt(0x4000,   b(0x00, 0x00, 0x81));
        assertWriteInt(0x1FFFFF, b(0x7F, 0x7F, 0xFF));
        assertWriteInt(0x200000,  b(0x00, 0x00, 0x00, 0x81));
        assertWriteInt(0xFFFFFFF, b(0x7F, 0x7F, 0x7F, 0xFF));
        assertWriteInt(0x10000000, b(0x00, 0x00, 0x00, 0x00, 0x81));
        assertWriteInt(0xFFFFFFFF, b(0x7F, 0x7F, 0x7F, 0x7F, 0x8F));
    }

    @Test
    public void testWriteString() throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CoderStream.EncoderOutputStream outputStream = new CoderStream.EncoderOutputStream(result);
        outputStream.writeString("あいうえお");
        outputStream.flush();
        byte[] bytes = result.toByteArray();
        assertThat(bytes, is(b(0x8a, 0x30, 0x42, 0x30, 0x44, 0x30, 0x46, 0x30, 0x48, 0x30, 0x4a)));
    }

    @Test
    public void testWriteAscii() throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CoderStream.EncoderOutputStream outputStream = new CoderStream.EncoderOutputStream(result);
        outputStream.writeAscii("test");
        outputStream.flush();
        byte[] bytes = result.toByteArray();
        assertThat(bytes, is(b(0x84, 't', 'e', 's', 't')));
    }

    private void assertReadInt(byte[] bytes, int expected) throws Exception {
        CoderStream.DecoderInputStream inputStream =
                new CoderStream.DecoderInputStream(new ByteArrayInputStream(bytes));
        int result = inputStream.readInt();
        assertThat(result, is(expected));
    }
    @Test
    public void testReadInt() throws Exception {
        assertReadInt(b(0x80), 0);
        assertReadInt(b(0xFF), 0x7F);
        assertReadInt(b(0x00, 0x81), 0x80);
        assertReadInt(b(0x7F, 0xFF), 0x3FFF);
        assertReadInt(b(0x00, 0x00, 0x81), 0x4000);
        assertReadInt(b(0x7F, 0x7F, 0xFF), 0x1FFFFF);
        assertReadInt(b(0x00, 0x00, 0x00, 0x81), 0x200000);
        assertReadInt(b(0x7F, 0x7F, 0x7F, 0xFF), 0xFFFFFFF);
        assertReadInt(b(0x00, 0x00, 0x00, 0x00, 0x81), 0x10000000);
        assertReadInt(b(0x7F, 0x7F, 0x7F, 0x7F, 0x8F), 0xFFFFFFFF);

        assertReadInt(b(0xFF, 0xFF), 0x7F);
    }
}
    

