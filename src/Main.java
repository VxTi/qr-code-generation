import qr.encoding.Encoding;
import qr.MaskPattern;
import qr.ErrorCorrection;
import qr.QRCodeBuilder;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {

    var qr = new QRCodeBuilder()
        .setErrorCorrection(ErrorCorrection.LOW)
        .setMaskPattern(MaskPattern.MASK4)
        .setEncoding(Encoding.NUMERIC)
        .setData("Test")
        .build();

    ImageIO.write(qr.getImage(), "PNG", new File("qr-code.png"));
  }
}