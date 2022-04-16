import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;


public class Compression {

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