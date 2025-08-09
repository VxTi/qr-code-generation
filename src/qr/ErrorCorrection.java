package qr;

public enum ErrorCorrection {
  LOW(0b01),
  MEDIUM(0b00),
  QUARTILE(0b11),
  HIGH(0b10);

  private final int mask;

  public int getMask() {
    return this.mask;
  }

  ErrorCorrection(int mask) {
    this.mask = mask;
  }
}
