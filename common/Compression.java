package common;
import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Allows for compression and decompression
 * of byte[] using zip
 */
public class Compression {
    /**
     * Compresses a byte[] input and returns the output
     * as a byte[]
     * If it fails then null is returned
     * @param input byte[] input
     * @return output byte[]
     */
    public byte[] compress(byte[] input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(baos);
            deflater.write(input);
            deflater.flush();
            deflater.close();
            return baos.toByteArray();
        }
        catch (Exception ex) {
            DispAlert.alertException(ex);
            return null;
        }
    }
    /**
     * Decompresses a byte[] input and returns the output
     * as a byte[]
     * If it fails then null is returned
     * @param input compressed byte[] input
     * @return output decompressed byte[]
     */
    public byte[] decompress(byte[] compressed) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InflaterOutputStream inflater = new InflaterOutputStream(baos);
            inflater.write(compressed);
            inflater.flush();
            inflater.close();
            return baos.toByteArray();
        }
        catch (Exception ex) {
            DispAlert.alertException(ex);
            return null;
        }
    }

}