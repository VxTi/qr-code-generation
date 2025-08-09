package qr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCode {

  // Size with quiet zone included
  private final int size;
  private final ErrorCorrection ec;
  private final MaskPattern maskPattern;
  private final BufferedImage image;
  private final Graphics2D gfx;
  private final int version;

  private static final Color ACTIVE_COLOR = Color.BLACK;
  private static final Color INACTIVE_COLOR = Color.WHITE;

  private static final int QUIET_ZONE_SIZE = 3;
  private static final int FINDER_PATTERN_SIZE = 7;

  private static final int QR_MASK = 0b101010000010010;
  private static final int EC_POLYNOMIAL_MASK = 0b10100110111;
  private static final int EC_VERSION_MASK = 0b1111100100101;

  public QRCode(byte[] payload, ErrorCorrection errorCorrection, MaskPattern maskPattern) {
    this.version = getVersionFromPayload(payload, errorCorrection);
    this.ec = errorCorrection;
    this.maskPattern = maskPattern;
    this.size = 17 + version * 4;

    this.image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    this.gfx = (Graphics2D) image.getGraphics();

    this.generate(payload);
  }

  private void generate(byte[] payload) {
    int i, x, y;
    var generator = this.maskPattern.getGenerator();

    gfx.setColor(INACTIVE_COLOR);
    gfx.fillRect(0, 0, size, size);

    drawFinderPatterns();
    drawTimingPattern();
    drawFormatInfo();
    drawDarkModule();
  }

  public BufferedImage getImage() {
    return this.image;
  }

  private int getVersionFromPayload(byte[] payload, ErrorCorrection errorCorrection) {
    for (int i = 0; i < payloadVersionMapping.length; i++) {
      if (payload.length < payloadVersionMapping[i][errorCorrection.ordinal()])
        return i + 1;
    }

    throw new IllegalArgumentException("Unable to determine version due to payload size being too large: " + payload.length);
  }

  private int getBitLength(int input) {
    int mask, offset;

    for (offset = 0; offset < 32; offset++) {
      // Create mask for trailing bits
      mask = -(1 << offset);

      // Check if input has bits at the trailing region
      if ((input & mask) == 0) {
        return offset;
      }
    }

    return 32;
  }

  private int ecFormatInfo() {
    int formatted = (ec.getMask() << 3 | maskPattern.getMask());
    int paddedFormattingBits = (formatted << 10) & 0x7FFF; // right-pad to ensure Length = 15 bits
    int paddedGeneratorPolynomial = EC_POLYNOMIAL_MASK;

    int formattedLength = getBitLength(paddedFormattingBits);
    int generatorLength = getBitLength(paddedGeneratorPolynomial);

    while (formattedLength > 11) {
      // right-pad generator to match formatting string length
      if (generatorLength < formattedLength) {
        paddedGeneratorPolynomial = EC_POLYNOMIAL_MASK << (formattedLength - generatorLength);
      }
      paddedFormattingBits ^= paddedGeneratorPolynomial;
      formattedLength = getBitLength(paddedFormattingBits);
    }

    // Final X-or, no need to pad generator since length is equal
    paddedFormattingBits ^= EC_POLYNOMIAL_MASK;

    // right-pad if length is below 10
    if (formattedLength < 10) {
      paddedFormattingBits <<= (10 - formattedLength);
    }

    int combined = formatted << 10 | paddedFormattingBits;

    return combined ^ QR_MASK;
  }

  private int ecVersionInfo() {
    int paddedVersionBits = (this.version << 15) & 0x7FFF; // right-pad to ensure Length = 18 bits
    int paddedGeneratorPolynomial = EC_POLYNOMIAL_MASK;

    int formattedLength = getBitLength(paddedVersionBits);
    int generatorLength = getBitLength(paddedGeneratorPolynomial);

    while (formattedLength > 12) {
      // right-pad generator to match formatting string length
      if (generatorLength < formattedLength) {
        paddedGeneratorPolynomial = EC_POLYNOMIAL_MASK << (formattedLength - generatorLength);
      }
      paddedVersionBits ^= paddedGeneratorPolynomial;
      formattedLength = getBitLength(paddedVersionBits);
    }

    // Final X-or, no need to pad generator since length is equal
    paddedVersionBits ^= EC_POLYNOMIAL_MASK;

    // right-pad if length is below 10
    if (formattedLength < 10) {
      paddedVersionBits <<= (10 - formattedLength);
    }

    return formatted << 10 | paddedVersionBits;
  }

  private void drawVersionInfo() {}

  private void drawFormatInfo() {
    int formatStringBits = ecFormatInfo();

    int i, offset, bitState;

    // 0-6 bits
    for (i = 0; i < 8; i++) {
      bitState = formatStringBits & (1 << (15 - i));
      gfx.setColor(bitState != 0 ? ACTIVE_COLOR : INACTIVE_COLOR);

      // Skip alignment pattern
      gfx.fillRect(i == FINDER_PATTERN_SIZE ? i + 1 : i - 1, FINDER_PATTERN_SIZE + 1, 1, 1);
      gfx.fillRect(FINDER_PATTERN_SIZE + 1, this.size - i, 1, 1);
    }

    for (i = 0, offset = 8; i < 9; i++) {
      bitState = formatStringBits & (1 << (15 - i - offset));
      gfx.setColor(bitState != 0 ? ACTIVE_COLOR : INACTIVE_COLOR);

      // Skip alignment pattern at y = FINDER_PATTERN_SIZE
      gfx.fillRect(FINDER_PATTERN_SIZE + 1, FINDER_PATTERN_SIZE - i - (i == 1 ? 1 : 0), 1, 1);
      gfx.fillRect(size - FINDER_PATTERN_SIZE + i - 1, FINDER_PATTERN_SIZE + 1, 1, 1);
    }
  }

  private void drawDarkModule() {
    gfx.setColor(ACTIVE_COLOR);
    gfx.fillRect(FINDER_PATTERN_SIZE + 1, size - FINDER_PATTERN_SIZE - 1, 1, 1);
  }

  private void drawTimingPattern() {
    Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        0, new float[]{1, 1}, 0);
    gfx.setStroke(dashed);

    gfx.drawLine(FINDER_PATTERN_SIZE - 1, FINDER_PATTERN_SIZE + 1, FINDER_PATTERN_SIZE - 1, size - FINDER_PATTERN_SIZE + 1);
    gfx.drawLine(FINDER_PATTERN_SIZE + 1, FINDER_PATTERN_SIZE - 1, size - FINDER_PATTERN_SIZE, FINDER_PATTERN_SIZE - 1);

  }

  private void drawFinderPatterns() {
    var adjustedFinderSize = FINDER_PATTERN_SIZE - 1;

    gfx.setColor(ACTIVE_COLOR);
    gfx.drawRect(0, 0, adjustedFinderSize, adjustedFinderSize);
    gfx.fillRect(2, 2, 3, 3);

    gfx.drawRect(size - FINDER_PATTERN_SIZE, 0, adjustedFinderSize, adjustedFinderSize);
    gfx.fillRect(size - adjustedFinderSize + 1, 2, 3, 3);

    gfx.drawRect(0, size - adjustedFinderSize - 1, adjustedFinderSize, adjustedFinderSize);
    gfx.fillRect(2, size - adjustedFinderSize + 1, 3, 3);
  }

  private int getColor(boolean active) {
    return active ? 0xff000000 : -1;
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

  public static class Builder {
    private ErrorCorrection errorCorrection = ErrorCorrection.LOW;
    private MaskPattern maskPattern;
    private byte[] payload;

    public Builder() {
    }

    public Builder setMaskPattern(MaskPattern pattern) {
      this.maskPattern = pattern;
      return this;
    }

    public Builder setPayload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    public Builder setErrorCorrection(ErrorCorrection errorCorrection) {
      this.errorCorrection = errorCorrection;
      return this;
    }

    public QRCode build() {
      return new QRCode(payload, errorCorrection, maskPattern);
    }
  }

  private String toBinaryString(int number) {
    if (number == 0) {
      return "0";
    }

    StringBuilder binaryNumber = new StringBuilder();
    for (; number > 0; number >>= 1) {
      binaryNumber.append(number % 2);
    }

    return binaryNumber.reverse().toString();
  }
}
