package qr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCode {

  // Size with quiet zone included
  private final int size;
  private final int version;
  private final ErrorCorrection ec;
  private final MaskPattern maskPattern;
  private final byte[] encodedData;

  private BufferedImage image;
  private final Graphics2D gfx;

  private static final Color ACTIVE_COLOR = Color.BLACK;
  private static final Color INACTIVE_COLOR = Color.WHITE;

  private static final int QUIET_ZONE_SIZE = 3;
  private static final int FINDER_PATTERN_SIZE = 7;
  private static final int ALIGNMENT_PATTERN_SIZE = 5;

  private static final int EC_FORMAT_POLYNOMIAL_MASK = 0b10100110111;
  private static final int EC_VERSION_POLYNOMIAL_MASK = 0b1111100100101;

  protected QRCode(String data, ErrorCorrection errorCorrection, MaskPattern maskPattern) {
    this.version = Version.fromData(data, errorCorrection);
    this.ec = errorCorrection;
    this.maskPattern = maskPattern;
    this.size = 17 + version * 4;

    this.image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    this.gfx = (Graphics2D) image.getGraphics();

    this.encodedData = encodeData(data);
    this.generateImage();
  }

  private void generateImage() {
    gfx.setColor(INACTIVE_COLOR);
    gfx.fillRect(0, 0, size, size);

    drawFinderPatterns();
    drawTimingPattern();
    drawFormatInfo();
    drawDarkModule();
    drawVersionInfo();
    drawAlignmentPatterns();
    drawData();

    BufferedImage silentAreaImage = new BufferedImage(size + 2 * QUIET_ZONE_SIZE, size + 2 * QUIET_ZONE_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D gfxSilentArea = silentAreaImage.createGraphics();
    gfxSilentArea.setColor(INACTIVE_COLOR);
    gfxSilentArea.fillRect(0, 0, size + 2 * QUIET_ZONE_SIZE, size + 2 * QUIET_ZONE_SIZE);

    gfxSilentArea.drawImage(image, QUIET_ZONE_SIZE, QUIET_ZONE_SIZE, null);

    this.image = silentAreaImage;
  }

  public BufferedImage getImage() {
    return this.image;
  }

  private byte[] encodeData(String data) {
    return Encoder.encode(data, version);
  }

  private void drawVersionInfo() {
    // Below version 7, version info is not rendered.
    if (this.version < 7) return;

    int versionInfoBits = Encoder.ECVersionInfo(version, EC_VERSION_POLYNOMIAL_MASK);
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
    int formatStringBits = Encoder.ECFormatInfo(ec, maskPattern, EC_FORMAT_POLYNOMIAL_MASK);

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

  private void drawData() {
    int x, y, bitOffset, bitState, byteIndex, reservedOffset;
    var generator = this.maskPattern.getGenerator();

    for (byteIndex = reservedOffset = 0; byteIndex < encodedData.length; byteIndex++) {
      for (bitOffset = 0; bitOffset < 8; bitOffset++) {
        x = size - ((byteIndex + bitOffset + reservedOffset) % 2 == 0 ? bitOffset * 2 + 1 : bitOffset * 2);
        y = size - (byteIndex + bitOffset + reservedOffset) % size;

        while (isReservedArea(x, y)) {
          reservedOffset++;
          x = size - ((byteIndex + bitOffset + reservedOffset) % 2 == 0 ? bitOffset * 2 + 1 : bitOffset * 2);
          y = size - (byteIndex + bitOffset + reservedOffset) % size;
        }
        bitState = (this.encodedData[byteIndex] & (1 << (7 - bitOffset))) ^ (generator.mask(x, y) ? 1 : 0);

        gfx.setColor(bitState != 0 ? ACTIVE_COLOR : INACTIVE_COLOR);
        gfx.fillRect(x, y, 1, 1);
      }
    }
  }

  private int[] getAlignmentPatternPositions() {
    if (version == 1) return new int[0];

    int step = version <= 6 ? 0 : (int) Math.round((version * 4 + 4) / (Math.floor(version / 7.) + 2) * 2) / 2;
    int[] positions = new int[version / 7 + 2];

    positions[0] = 6;
    positions[positions.length - 1] = size - 7;

    for (int i = positions.length - 2; i > 0; i--) {
      positions[i] = positions[i + 1] - step;
    }

    return positions;
  }

  private void drawAlignmentPatterns() {
    if (version == 1) return;

    int[] positions = getAlignmentPatternPositions();

    for (int y : positions) {
      for (int x : positions) {
        // Skip if pattern would overlap with finder patterns
        if (isFinderPattern(x, y)) {
          continue;
        }

        // Draw the alignment pattern
        gfx.setColor(ACTIVE_COLOR);
        gfx.fillRect(x - 2, y - 2, ALIGNMENT_PATTERN_SIZE, ALIGNMENT_PATTERN_SIZE);
        gfx.setColor(INACTIVE_COLOR);
        gfx.fillRect(x - 1, y - 1, 3, 3);
        gfx.setColor(ACTIVE_COLOR);
        gfx.fillRect(x, y, 1, 1);
      }
    }
  }

  private boolean isFinderPattern(int x, int y) {
    boolean topLeft = x >= 0 && y >= 0 && x < FINDER_PATTERN_SIZE && y < FINDER_PATTERN_SIZE;
    boolean topRight = x >= size - FINDER_PATTERN_SIZE && y >= 0 && x < size && y < FINDER_PATTERN_SIZE;
    boolean bottomLeft = x >= 0 && y >= size - FINDER_PATTERN_SIZE && x < FINDER_PATTERN_SIZE && y < size;

    return topLeft || topRight || bottomLeft;
  }

  private boolean isAlignmentPattern(int x, int y) {
    int[] alignmentPositions = getAlignmentPatternPositions();

    if (alignmentPositions.length == 0) return false;

    int dx, dy;

    for (int position : alignmentPositions) {
      dx = Math.abs(x - position);
      dy = Math.abs(y - position);

      if (dx <= 2 && dy <= 2) return true;
    }

    return false;
  }

  private boolean isVersionArea(int x, int y) {
    if (x < 0 || y < 0) return false;

    return (x >= size - FINDER_PATTERN_SIZE - 4 && x <= size - FINDER_PATTERN_SIZE - 1 && y <= FINDER_PATTERN_SIZE - 4)
        || (x <= 6 && y >= size - FINDER_PATTERN_SIZE - 4 && y <= size - FINDER_PATTERN_SIZE - 1);
  }

  private boolean isReservedArea(int x, int y) {
    if (isFinderPattern(x, y)) return true;

    if (isAlignmentPattern(x, y)) return true;

    // alignment line
    if (y == FINDER_PATTERN_SIZE || x == FINDER_PATTERN_SIZE) return true;

    // Dark module
    if (x == FINDER_PATTERN_SIZE + 1 && y == size - FINDER_PATTERN_SIZE - 1)
      return true;

    if (
        (x <= FINDER_PATTERN_SIZE + 2 && y < FINDER_PATTERN_SIZE + 2) ||// top left
            (x >= size - FINDER_PATTERN_SIZE - 1 && y <= FINDER_PATTERN_SIZE + 2) || // top right
            (x <= FINDER_PATTERN_SIZE + 2 && y >= size - FINDER_PATTERN_SIZE) // bottom left
    ) return true;

    // Check if version area is occupied
    return version >= 7 && isVersionArea(x, y);
  }
}
