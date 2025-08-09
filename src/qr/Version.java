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
    for (int i = 0; i < payloadVersionMapping.length; i++) {
      if (data.length() < payloadVersionMapping[i][errorCorrection.ordinal()])
        return i + 1;
    }

    throw new IllegalArgumentException("Unable to determine version due to payload size being too large: " + data.length());
  }

  private static final int[][] payloadVersionMapping = {
      {17, 14, 11, 7},
      {32, 26, 20, 14},
      {53, 42, 32, 24},
      {78, 62, 46, 34},
      {106, 84, 60, 44},
      {134, 106, 74, 58},
      {154, 122, 86, 64},
      {192, 152, 108, 84},
      {230, 180, 130, 98},
      {271, 213, 151, 119},
      {321, 251, 177, 137},
      {367, 287, 203, 155},
      {425, 331, 241, 177},
      {458, 362, 258, 194},
      {520, 412, 292, 220},
      {586, 450, 322, 250},
      {644, 504, 364, 280},
      {718, 560, 394, 310},
      {792, 624, 442, 338},
      {858, 666, 482, 382},
      {929, 711, 509, 403},
      {1003, 779, 565, 439},
      {1091, 857, 611, 461},
      {1171, 911, 661, 511},
      {1273, 997, 715, 535},
      {1367, 1059, 751, 593},
      {1465, 1125, 805, 625},
      {1528, 1190, 868, 658},
      {1628, 1264, 908, 698},
      {1732, 1370, 982, 742},
      {1840, 1452, 1030, 790},
      {1952, 1538, 1112, 842},
      {2068, 1628, 1168, 898},
      {2188, 1722, 1228, 958},
      {2303, 1809, 1283, 983},
      {2431, 1911, 1351, 1051},
      {2563, 1989, 1423, 1093},
      {2699, 2099, 1499, 1139},
      {2809, 2213, 1579, 1219},
      {2953, 2331, 1663, 1273}
  };
}
