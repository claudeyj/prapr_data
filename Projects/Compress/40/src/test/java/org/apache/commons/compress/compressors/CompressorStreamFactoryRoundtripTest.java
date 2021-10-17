package org.apache.commons.compress.compressors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CompressorStreamFactoryRoundtripTest {

    @Parameters(name = "{0}")
    public static String[] data() {
        return new String[] { //
                CompressorStreamFactory.BZIP2, //
                CompressorStreamFactory.DEFLATE, //
                CompressorStreamFactory.GZIP, //
                // CompressorStreamFactory.LZMA, // Not implemented yet
                // CompressorStreamFactory.PACK200, // Bug
                // CompressorStreamFactory.SNAPPY_FRAMED, // Not implemented yet
                // CompressorStreamFactory.SNAPPY_RAW, // Not implemented yet
                CompressorStreamFactory.XZ, //
                // CompressorStreamFactory.Z, // Not implemented yet
        };
    }

    private final String compressorName;

    public CompressorStreamFactoryRoundtripTest(final String compressorName) {
        this.compressorName = compressorName;
    }

    @Test
    public void testCompressorStreamFactoryRoundtrip() throws Exception {
        final CompressorStreamFactory factory = new CompressorStreamFactory();
        final ByteArrayOutputStream compressedOs = new ByteArrayOutputStream();
        final CompressorOutputStream compressorOutputStream = factory.createCompressorOutputStream(compressorName,
                compressedOs);
        final String fixture = "The quick brown fox jumps over the lazy dog";
        compressorOutputStream.write(fixture.getBytes("UTF-8"));
        compressorOutputStream.flush();
        compressorOutputStream.close();
        final ByteArrayInputStream is = new ByteArrayInputStream(compressedOs.toByteArray());
        final CompressorInputStream compressorInputStream = factory.createCompressorInputStream(compressorName, is);
        final ByteArrayOutputStream decompressedOs = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, decompressedOs);
        compressorInputStream.close();
        decompressedOs.flush();
        decompressedOs.close();
        Assert.assertEquals(fixture, decompressedOs.toString("UTF-8"));
    }

}
