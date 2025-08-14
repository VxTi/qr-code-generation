package qr;

public enum Version {
  // TODO: Change max byte count to actual
  VERSION_1(21, 21, 0),
  VERSION_2(25, 64, 1),
  VERSION_3(29, 64, 6),
  VERSION_4(33, 64, 13),
  VERSION_5(37, 64, 22),
  VERSION_6(41, 64, 33),
  VERSION_7(45, 64, 46);
  // VERSION_8(49, 64),
  // VERSION_9(53, 64),
  // VERSION_10(57, 64),
  // VERSION_11(61, 64);

  public static Version getHighestVersion() {
    return values()[values().length - 1];
  }

  private static final int[] alignmentFactors = {0, 1, 6, 13, 22, 33, 46};

  private final int maxByteCount;
  private final int size;
  private final int alignmentOffset;

  public static boolean canEncode(byte[] payload) {
    var allVersions = values();

    return payload.length < allVersions[allVersions.length - 1].maxByteCount;
  }

  public static Version fromBytes(byte[] payload) {
    for (Version version : values()) {
      if (payload.length < version.maxByteCount)
        return version;
    }

    // Highest version supported.
    return Version.getHighestVersion();
  }

  public int getMaxByteCount() {
    return this.maxByteCount;
  }

  public int getSize() {
    return this.size;
  }

  public int getAlignmentOffset() {
    return this.alignmentOffset;
  }

  Version(int size, int maxByteCount, int alignmentOffset) {
    this.size = size;
    this.maxByteCount = maxByteCount;
    this.alignmentOffset = alignmentOffset;
  }

  /**
   * Determines the QR code version based on the provided data and error correction level.
   * The method evaluates the length of the input data against predefined size constraints
   * for each version and error correction level. It returns the version number that
   * can accommodate the given data and error correction level. If no suitable version is
   * found, an IllegalArgumentException is thrown.
   *
   * @param data the input data string to be encoded into the QR code
   * @param errorCorrection the level of error correction to be applied, determining the
   *                        allowed data size for each version
   * @return the version number that can accommodate the input data with the specified
   *         error correction level
   * @throws IllegalArgumentException if the input data exceeds the maximum size
   *                                  supported by the highest version
   */
  public static int fromData(String data, ErrorCorrection errorCorrection) {
    for (int i = 0; i < Encoder.VERSION_EC_CAPACITY_MAPPING.length; i++) {
      if (data.length() < Encoder.VERSION_EC_CAPACITY_MAPPING[i][errorCorrection.ordinal()])
        return i + 1;
    }

    throw new IllegalArgumentException("Unable to determine version due to payload size being too large: " + data.length());
  }

  public static int getCapacityForVersion(int version, ErrorCorrection errorCorrection) {
    if (version <= 0) throw new IllegalArgumentException("Cannot calculate capacity for versions < 1");

    return Encoder.VERSION_EC_CAPACITY_MAPPING[version - 1][errorCorrection.ordinal()];
  }
}
