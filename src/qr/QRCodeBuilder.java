package qr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCodeBuilder {
  private ErrorCorrection errorCorrection = ErrorCorrection.LOW;
  private MaskPattern maskPattern = MaskPattern.MASK0;
  private String data;
  private BufferedImage embeddedImage;
  private int moduleSize = 1;
  private int moduleRadius = 0;
  private Color activeColor = Color.BLACK;
  private Color inactiveColor = Color.WHITE;
  private Color backgroundColor = Color.WHITE;

  public QRCodeBuilder() {
  }

  public QRCodeBuilder setActiveColor(Color color) {
    this.activeColor = color;
    return this;
  }

  public QRCodeBuilder setInactiveColor(Color color) {
    this.inactiveColor = color;
    return this;
  }

  public QRCodeBuilder setBackgroundColor(Color color) {
    this.backgroundColor = color;
    return this;
  }

  public QRCodeBuilder setModuleSize(int size) {
    if (size < 1) throw new IllegalArgumentException("Size must be above 0");

    this.moduleSize = size;
    return this;
  }

  public QRCodeBuilder setModuleBorderRadius(int radius) {
    this.moduleRadius = radius;
    return this;
  }

  public QRCodeBuilder setEmbeddedImage(BufferedImage image) {
    this.embeddedImage = image;
    return this;
  }

  public QRCodeBuilder setMaskPattern(MaskPattern pattern) {
    this.maskPattern = pattern;
    return this;
  }

  public QRCodeBuilder setData(String data) {
    this.data = data;
    return this;
  }

  public QRCodeBuilder setErrorCorrection(ErrorCorrection errorCorrection) {
    this.errorCorrection = errorCorrection;
    return this;
  }

  public QRCode build() {
    return new QRCode(
        data,
        errorCorrection,
        maskPattern,
        embeddedImage,
        moduleSize,
        moduleRadius,
        activeColor,
        inactiveColor,
        backgroundColor
    );
  }
}
