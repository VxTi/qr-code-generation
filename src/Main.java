import qr.MaskPattern;
import qr.ErrorCorrection;
import qr.QRCode;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {

    var qr = new QRCode.Builder()
        .setErrorCorrection(ErrorCorrection.LOW)
        .setMaskPattern(MaskPattern.MASK4)
        .setPayload(new byte[10])
        .build();

    ImageIO.write(qr.getImage(), "PNG", new File("qr-code.png"));
  }
}