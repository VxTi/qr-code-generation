package qr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCode {

  // Size with quiet zone included
  private final int size;
  private final int version;
  private final ErrorCorrection ec;
  private final MaskPattern maskPattern;

  private final BufferedImage image;
  private final Graphics2D gfx;

  private static final Color ACTIVE_COLOR = Color.BLACK;
  private static final Color INACTIVE_COLOR = Color.WHITE;

  private static final int QUIET_ZONE_SIZE = 3;
  private static final int FINDER_PATTERN_SIZE = 7;

  private static final int QR_MASK = 0b101010000010010;
  private static final int EC_FORMAT_POLYNOMIAL_MASK = 0b10100110111;
  private static final int EC_VERSION_POLYNOMIAL_MASK = 0b1111100100101;

  protected QRCode(String data, ErrorCorrection errorCorrection, MaskPattern maskPattern) {
    this.version = getVersionFromPayload(data, errorCorrection);
    this.ec = errorCorrection;
    this.maskPattern = maskPattern;
    this.size = 17 + version * 4;

    this.image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    this.gfx = (Graphics2D) image.getGraphics();

    this.generate(data);
  }

  private void generate(String data) {
    int i, x, y;
    var generator = this.maskPattern.getGenerator();

    drawFinderPatterns();
    drawTimingPattern();
    drawFormatInfo();
    drawDarkModule();
    drawVersionInfo();

    encodeData(data);
  }

  public BufferedImage getImage() {
    return this.image;
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
  private static int getVersionFromPayload(String data, ErrorCorrection errorCorrection) {
    for (int i = 0; i < payloadVersionMapping.length; i++) {
      if (data.length() < payloadVersionMapping[i][errorCorrection.ordinal()])
        return i + 1;
    }

    throw new IllegalArgumentException("Unable to determine version due to payload size being too large: " + data.length());
  }

  /**
   * Computes and returns the format information for error correction and mask patterns
   * as a 15-bit integer encoded with error correction and masking logic. This value is
   * used to draw the format information in a QR code.
   *
   * @return a 15-bit integer representing the formatted error correction and mask pattern
   *         information, including error correction bits and a final XOR with a predefined
   *         QR mask constant.
   */
  private int ecFormatInfo() {
    int formatted = (ec.getMask() << 3 | maskPattern.getMask());
    int paddedFormattingBits = (formatted << 10) & 0x7FFF; // right-pad to ensure Length = 15 bits
    int paddedGeneratorPolynomial = EC_FORMAT_POLYNOMIAL_MASK;

    int formatBitLength = Encoder.getBitLength(paddedFormattingBits);
    int generatorBitLength = Encoder.getBitLength(paddedGeneratorPolynomial);

    while (formatBitLength > 11) {
      // right-pad generator to match formatting string length
      if (generatorBitLength < formatBitLength) {
        paddedGeneratorPolynomial = EC_FORMAT_POLYNOMIAL_MASK << (formatBitLength - generatorBitLength);
      }
      paddedFormattingBits ^= paddedGeneratorPolynomial;
      formatBitLength = Encoder.getBitLength(paddedFormattingBits);
    }

    // Final X-or, no need to pad generator since length is equal
    paddedFormattingBits ^= EC_FORMAT_POLYNOMIAL_MASK;

    // right-pad if the length is below 10
    if (formatBitLength < 10) {
      paddedFormattingBits <<= (10 - formatBitLength);
    }

    int combined = formatted << 10 | paddedFormattingBits;

    return combined ^ QR_MASK;
  }

  /**
   * Computes and returns the version information for the QR code as an integer
   * encoded with error correction bits. This method applies a polynomial division
   * to calculate the remainder, which is used to provide version information
   * conforming to QR code standards. The result combines the version number
   * with the calculated error correction bits.
   *
   * @return an integer representing the combined version number and error
   *         correction bits for version information in a QR code.
   */
  private int ecVersionInfo() {
    int paddedVersionBits = (this.version << 12);
    int paddedGeneratorPolynomial = EC_VERSION_POLYNOMIAL_MASK;

    int versionBitLength = Encoder.getBitLength(paddedVersionBits);
    int generatorBitLength = Encoder.getBitLength(paddedGeneratorPolynomial);

    do {
      // right-pad generator to match formatting string length
      paddedGeneratorPolynomial = EC_VERSION_POLYNOMIAL_MASK << (versionBitLength - generatorBitLength);
      paddedVersionBits ^= paddedGeneratorPolynomial;
      versionBitLength = Encoder.getBitLength(paddedVersionBits);
    } while (versionBitLength > 12);

    return this.version << 12 | paddedVersionBits;
  }

  private byte[] encodeData(String data) {
    var encodedBytes = Encoder.encode(data, version);

    System.out.printf("Encoded: %s", toBinaryString(encodedBytes));

    return encodedBytes;
  }

  private String toBinaryString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
    }
    return sb.toString();
  }

  private void drawVersionInfo() {
    // Below version 7, version info is not rendered.
    if (this.version < 7) return;

    int versionInfoBits = ecVersionInfo();
    int i, xOffset, yOffset, bitState;

    for (i = 0; i < 18; i++) {
      xOffset = i / 3;
      yOffset = 3 - (i % 3);
      bitState = versionInfoBits & (1 << i);

      gfx.setColor(bitState != 0 ? ACTIVE_COLOR : INACTIVE_COLOR);

      gfx.fillRect(xOffset, size - FINDER_PATTERN_SIZE - yOffset - 1, 1, 1);
      gfx.fillRect(size - FINDER_PATTERN_SIZE - yOffset - 1, xOffset, 1, 1);
    }
  }

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
