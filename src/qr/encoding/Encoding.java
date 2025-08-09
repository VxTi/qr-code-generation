package qr.encoding;

public enum Encoding {
  NUMERIC(0b0001, 10, 12, 14),
  ALPHANUMERIC(0b0010,  9, 11, 13),
  BYTE(0b0100,  8, 16, 16);

  private final int bitmask;
  private final int v1_9_bitlength;
  private final int v10_26_bitlength;
  private final int v27_40_bitlength;

  Encoding(int bitmask, int v1_9_bitlength, int v10_26_bitlength, int v27_40_bitlength) {
    this.bitmask = bitmask;
    this.v1_9_bitlength = v1_9_bitlength;
    this.v10_26_bitlength = v10_26_bitlength;
    this.v27_40_bitlength = v27_40_bitlength;
  }

  public int getBitLengthForVersion(int version) {
    return version < 10 ? v1_9_bitlength :
        version < 27 ? v10_26_bitlength :
            v27_40_bitlength;
  }


  public int getMask() {
    return this.bitmask;
  }
}
