import java.io.Serializable;
import javax.crypto.spec.SecretKeySpec;

public class KeyData implements Serializable {
  public static final long serialVersionUID = 1288588976588802975L;

  private byte[] initVector;
  private SecretKeySpec secretKey;

  public KeyData(SecretKeySpec _secretKeySpec, byte[] _initVector) {
    secretKey = _secretKeySpec;
    initVector = _initVector;
  }

  public void setKey(SecretKeySpec _secretKey) {
    secretKey = _secretKey;
  }

  public void setInitVector(byte[] _initVector) {
    initVector = _initVector;
  }

  public SecretKeySpec getKey() {
    return secretKey;
  }

  public byte[] getInitVector() {
    return initVector;
  }
}
