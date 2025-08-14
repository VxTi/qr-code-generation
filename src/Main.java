import qr.MaskPattern;
import qr.ErrorCorrection;
import qr.QRCodeBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {

    var qr = new QRCodeBuilder()
        .setErrorCorrection(ErrorCorrection.LOW)
        .setMaskPattern(MaskPattern.MASK0)
        .setBackgroundColor(Color.WHITE)
        .setModuleSize(20)
        .setData("https://google.com")
        .build();

    ImageIO.write(qr.getImage(), "PNG", new File("qr-code.png"));
  }
}