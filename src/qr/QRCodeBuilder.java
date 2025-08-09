package qr;

import qr.encoding.Encoding;

public class QRCodeBuilder {
  private ErrorCorrection errorCorrection = ErrorCorrection.LOW;
  private Encoding encoding = Encoding.NUMERIC;
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

  public QRCodeBuilder setEncoding(Encoding encoding) {
    this.encoding = encoding;
    return this;
  }

  public QRCode build() {
    return new QRCode(data, errorCorrection, maskPattern, encoding);
  }
}
