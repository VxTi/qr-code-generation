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
}
