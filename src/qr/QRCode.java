package qr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCode {

  // Size with quiet zone included
  private final int size;
  private final int version;
  private final ErrorCorrection errorCorrection;
  private final MaskPattern maskPattern;
  private final byte[] encodedData;
  private final int moduleSize;
  private final int arcSize;

  private final Color backgroundColor;
  private final Color activeColor;
  private final Color inactiveColor;
  private BufferedImage embeddedImage;
  private BufferedImage image;
  private final Graphics2D gfx;

  private static final int QUIET_ZONE_SIZE = 3;
  private static final int FINDER_PATTERN_SIZE = 7;
  private static final int FINDER_PATTERN_INNER_SIZE = 3;
  private static final int ALIGNMENT_PATTERN_SIZE = 5;

  private static final int EC_FORMAT_POLYNOMIAL_MASK = 0b10100110111;
  private static final int EC_VERSION_POLYNOMIAL_MASK = 0b1111100100101;

  protected QRCode(
      String data,
      ErrorCorrection errorCorrection,
      MaskPattern maskPattern,
      BufferedImage embeddedImage,
      int moduleSize,
      int moduleBorderRadius,
      Color activeColor,
      Color inactiveColor,
      Color backgroundColor
  ) {
    this.activeColor = activeColor;
    this.inactiveColor = inactiveColor;
    this.backgroundColor = backgroundColor;

    this.arcSize = moduleBorderRadius * 2;
    this.moduleSize = moduleSize;
    this.version = Version.fromData(data, errorCorrection);
    this.errorCorrection = errorCorrection;
    this.maskPattern = maskPattern;
    this.size = 17 + version * 4;

    this.image = new BufferedImage((size + 2 * QUIET_ZONE_SIZE) * moduleSize, (size + 2 * QUIET_ZONE_SIZE) * moduleSize, BufferedImage.TYPE_INT_ARGB);
    this.gfx = (Graphics2D) image.getGraphics();

    this.embeddedImage = embeddedImage;
    this.encodedData = encodeData(data);
    this.generateImage();
  }

  private void generateImage() {
    // Fills quiet zone and the rest of the background
    gfx.setColor(this.backgroundColor);
    gfx.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());

    drawFinderPattern(0, 0);
    drawFinderPattern(0, size - FINDER_PATTERN_SIZE);
    drawFinderPattern(size - FINDER_PATTERN_SIZE, 0);

    // Timing pattern
    for (int i = 0; i < size - FINDER_PATTERN_SIZE * 2 - 2; i += 2) {
      drawModule(FINDER_PATTERN_SIZE - 1, FINDER_PATTERN_SIZE + i + 1, true); // left alignment pattern
      drawModule(FINDER_PATTERN_SIZE + i + 1, FINDER_PATTERN_SIZE - 1, true); // top alignment pattern
    }

    drawFormatInfo();
    drawModule(FINDER_PATTERN_SIZE + 1, size - FINDER_PATTERN_SIZE - 1, true); // Dark module
    drawVersionInfo();
    drawAlignmentPatterns();
    drawData();
    drawEmbeddedImage();
  }

  public BufferedImage getImage() {
    return this.image;
  }

  private byte[] encodeData(String data) {
    return Encoder.encode(data, this.version, this.errorCorrection);
  }

  private void drawEmbeddedImage() {
    // TODO: Make functional
    if (this.version < 10 || this.embeddedImage == null) return;

    int embeddedModuleCount = 8;
    int scaledX = (this.size / 2 - embeddedModuleCount / 2) * this.moduleSize;
    int scaledY = (this.size / 2 - embeddedModuleCount / 2) * this.moduleSize;
    int embeddedSize = embeddedModuleCount * this.moduleSize;

    gfx.drawImage(this.embeddedImage, scaledX, scaledY, embeddedSize, embeddedSize, null);
  }

  /**
   * Draws the finder pattern at the given coordinates
   */
  private void drawFinderPattern(int x, int y) {

    int centerOffset = (FINDER_PATTERN_SIZE - FINDER_PATTERN_INNER_SIZE) / 2;

    drawModule(x, y, true, FINDER_PATTERN_SIZE);
    drawModule(x + centerOffset / 2, y + centerOffset / 2, false, (FINDER_PATTERN_SIZE + FINDER_PATTERN_INNER_SIZE) / 2);
    drawModule(x + centerOffset, y + centerOffset, true, FINDER_PATTERN_INNER_SIZE);
  }

  private void drawModule(int x, int y, boolean active, int cells) {
    gfx.setColor(active ? this.activeColor : this.inactiveColor);

    int scaledX = (x + QUIET_ZONE_SIZE) * this.moduleSize;
    int scaledY = (y + QUIET_ZONE_SIZE) * this.moduleSize;
    int size = this.moduleSize * cells;

    if (this.arcSize == 0) {
      gfx.fillRect(scaledX, scaledY, size, size);
    } else {
      gfx.fillRoundRect(scaledX, scaledY, size, size, this.arcSize * cells, this.arcSize * cells);
    }
  }

  private void drawModule(int x, int y, boolean active) {
    drawModule(x, y, active, 1);
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

      drawModule(xOffset, size - FINDER_PATTERN_SIZE - yOffset - 1, bitState != 0);
      drawModule(size - FINDER_PATTERN_SIZE - yOffset - 1, xOffset, bitState != 0);
    }
  }

  private void drawFormatInfo() {
    int formatStringBits = Encoder.ECFormatInfo(errorCorrection, maskPattern, EC_FORMAT_POLYNOMIAL_MASK);

    int i, offset;
    boolean active;

    // 0-6 bits
    for (i = 0; i < 8; i++) {
      active = (formatStringBits & (1 << (15 - i))) != 0;

      // Skip alignment pattern
      drawModule(i == FINDER_PATTERN_SIZE ? i + 1 : i - 1, FINDER_PATTERN_SIZE + 1, active);
      drawModule(FINDER_PATTERN_SIZE + 1, this.size - i, active);
    }

    for (i = 0, offset = 8; i < 9; i++) {
      active = (formatStringBits & (1 << (15 - i - offset))) != 0;

      // Skip alignment pattern at y = FINDER_PATTERN_SIZE
      drawModule(FINDER_PATTERN_SIZE + 1, FINDER_PATTERN_SIZE - i - (i == 1 ? 1 : 0), active);
      drawModule(size - FINDER_PATTERN_SIZE + i - 1, FINDER_PATTERN_SIZE + 1, active);
    }
  }

  private void drawData() {
    int x, y, bitOffset, byteIndex, skippedModules, moduleOffset;
    var generator = this.maskPattern.getGenerator();

    boolean active;
    gfx.setFont(new Font("Sans Serif", Font.PLAIN, this.moduleSize / 2));

    for (byteIndex = skippedModules = 0; byteIndex < encodedData.length; byteIndex++) {
      for (bitOffset = 0; bitOffset < 8; ) {

        moduleOffset = skippedModules + byteIndex * 8 + bitOffset;

        x = size - (moduleOffset % 2) - (moduleOffset / (2 * size)) - 1;
        y = size - ((moduleOffset / 2) % (size * 2)) * (moduleOffset % size > size ? -1 : 1) - 1;

        if (isReservedArea(x, y)) {
          skippedModules++;
          bitOffset++;
          continue;
        }

        active = ((generator.mask(x, y) ? 1 : 0) ^ (this.encodedData[byteIndex] & (1 << (7 - bitOffset)))) != 0;

        drawModule(x, y, active);

        gfx.setColor(active ? this.inactiveColor : activeColor);
        gfx.drawString(String.format("%d", bitOffset), (QUIET_ZONE_SIZE + x + 0.2f) * moduleSize, (QUIET_ZONE_SIZE + y + 0.6f) * moduleSize);

        bitOffset++;
      }
    }
  }

  private void drawAlignmentPattern(int x, int y) {
    drawModule(x - 2, y - 2, true, 5);
    drawModule(x - 1, y - 1, false, 3);
    drawModule(x, y, true);
  }

  private void drawAlignmentPatterns() {
    if (version == 1) return;

    var coordinates = this.alignmentVersionCoordinateMapping[this.version - 2];

    for (int y : coordinates) {
      for (int x : coordinates) {
        // Skip if pattern would overlap with finder patterns
        if (!isFinderPattern(x, y)) {
          drawAlignmentPattern(x, y);
        }
      }
    }
  }

  private boolean isFinderPattern(int x, int y) {
    boolean topLeft = x >= 0 && y >= 0 && x < FINDER_PATTERN_SIZE && y < FINDER_PATTERN_SIZE;
    boolean topRight = x >= size - FINDER_PATTERN_SIZE - 1 && y >= 0 && x < size && y < FINDER_PATTERN_SIZE + 1;
    boolean bottomLeft = x >= 0 && y >= size - FINDER_PATTERN_SIZE - 1 && x < FINDER_PATTERN_SIZE + 1 && y < size;

    return topLeft || topRight || bottomLeft;
  }

  private boolean isAlignmentPattern(int x, int y) {
    if (this.version < 2) return false;

    int[] alignmentPositions = this.alignmentVersionCoordinateMapping[this.version - 2];

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
    if (isFinderPattern(x, y) || isAlignmentPattern(x, y)) return true;

    // alignment line
    if (x == FINDER_PATTERN_SIZE - 1 || y == FINDER_PATTERN_SIZE - 1) return true;

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

  // From version 2
  final int[][] alignmentVersionCoordinateMapping = {
      {6, 18},
      {6, 22},
      {6, 26},
      {6, 30},
      {6, 34},
      {6, 22, 38},
      {6, 24, 42},
      {6, 26, 46},
      {6, 28, 50},
      {6, 30, 54},
      {6, 32, 58},
      {6, 34, 62},
      {6, 26, 46, 66},
      {6, 26, 48, 70},
      {6, 26, 50, 74},
      {6, 30, 54, 78},
      {6, 30, 56, 82},
      {6, 30, 58, 86},
      {6, 34, 62, 90},
      {6, 28, 50, 72, 94},
      {6, 26, 50, 74, 98},
      {6, 30, 54, 78, 102},
      {6, 28, 54, 80, 106},
      {6, 32, 58, 84, 110},
      {6, 30, 58, 86, 114},
      {6, 34, 62, 90, 118},
      {6, 26, 50, 74, 98, 122},
      {6, 30, 54, 78, 102, 126},
      {6, 26, 52, 78, 104, 130},
      {6, 30, 56, 82, 108, 134},
      {6, 34, 60, 86, 112, 138},
      {6, 30, 58, 86, 114, 142},
      {6, 34, 62, 90, 118, 146},
      {6, 30, 54, 78, 102, 126, 150},
      {6, 24, 50, 76, 102, 128, 154},
      {6, 28, 54, 80, 106, 132, 158},
      {6, 32, 58, 84, 110, 136, 162},
      {6, 26, 54, 82, 110, 138, 166},
      {6, 30, 58, 86, 114, 142, 170}};
}
