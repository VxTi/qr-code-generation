package qr;

public class QRCodeBuilder {
  private ErrorCorrection errorCorrection = ErrorCorrection.LOW;
  private MaskPattern maskPattern = MaskPattern.MASK0;
  private String data;

  public QRCodeBuilder() {
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
    return new QRCode(data, errorCorrection, maskPattern);
  }
}
