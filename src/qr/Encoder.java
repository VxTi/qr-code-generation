package qr;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Encoder {

  private static final int MODE_BIT_COUNT = 4;
  private static final int LENGTH_BIT_COUNT = 9;

  private static final int QR_MASK = 0b101010000010010;

  private static final int[] RS_LOG_TABLE = new int[256];
  private static final int[] RS_EXP_TABLE = new int[256];
  private static final int[] RS_GENERATOR_POLY = {0, 43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150, 39, 243, 163, 136};

  private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[0-9A-Z $%*+\\-./:]+$");
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  /*
   * Initialize RS tables
   */
  static {
    int x = 1;
    for (int i = 0; i < 255; i++) {
      RS_EXP_TABLE[i] = x;
      RS_LOG_TABLE[x] = i;
      x = (x * 2) ^ ((x >>> 7) * 0x11D);
    }
  }


  /**
   * Determines the length offset based on the provided version of the QR encoding.
   * The offset is used to calculate the total bit length for encoding.
   *
   * @param version the QR code version, which determines various encoding parameters; values typically range from 1 to 40
   * @return the length offset: 0 if the version is less than 10, 1 if the version is between 10 and 26 (inclusive), or 2 if the version is 27 or higher
   */
  public static int getVersionLengthOffset(int version) {
    return version < 10 ? 0 : version < 27 ? 1 : 2;
  }

  /**
   * Calculates the number of bits required to represent the given integer input.
   * The method computes the bit length by determining the position of the highest
   * non-zero bit from the least significant bit.
   *
   * @param input the integer whose bit length is to be calculated
   * @return the number of bits needed to represent the input integer
   */
  public static int getBitLength(int input) {
    int mask, offset;

    for (offset = 0; offset < 32; offset++) {
      // Create a mask for trailing bits
      mask = -(1 << offset);

      // Check if input has bits at the trailing region
      if ((input & mask) == 0) {
        return offset;
      }
    }

    return 32;
  }

  /**
   * Encodes the specified input string into a byte array using the QR code encoding standards,
   * defaulting to UTF-8 character encoding. The method chooses the optimal QR code encoding mode
   * (numeric, alphanumeric, or byte) based on the content of the input string.
   *
   * @param data    the input string to be encoded; cannot be null
   * @param version the QR code version, which dictates encoding parameters; typically ranges from 1 to 40
   * @return a byte array representing the encoded data in the chosen QR code encoding mode
   */
  public static byte[] encode(String data, int version) {
    return encode(data, version, StandardCharsets.UTF_8);
  }

  /**
   * Encodes the specified input data string into a byte array based on the QR code encoding standards.
   * The encoding process chooses the most efficient QR code encoding mode based on the input data:
   * numeric, alphanumeric, or byte mode.
   *
   * @param data     the input string to be encoded; cannot be null
   * @param version  the QR code version, which dictates encoding parameters; typically ranges from 1 to 40
   * @param encoding the character encoding to use when encoding in byte mode; cannot be null
   * @return a byte array representing the encoded data in the chosen QR code encoding mode
   */
  public static byte[] encode(String data, int version, Charset encoding) {
    // First step; figure out which encoding method is most efficient for the input data.

    byte[] encodedBytes = canEncodeNumeric(data)
        ? encodeNumeric(data.chars().map(digit -> digit - '0').toArray(), version)
        : canEncodeAlphaNumeric(data)
        ? encodeAlphaNumeric(data, version)
        : encodeBytes(data.getBytes(encoding), version);

    byte[] codewords = createCodewordsForBytes(encodedBytes);
    int remainderBits = getRemainderBitsForVersion(version);

    return codewords;
  }

  private static int getRemainderBitsForVersion(int version) {
    if (version == 1
        || (version >= 7 && version <= 13)
        || (version >= 35 && version <= 40)
    ) return 0;

    if (version >= 2 && version <= 6) return 7;

    if ((version >= 14 && version <= 20)
        || (version >= 28 && version <= 34)
    ) return 3;

    return 4;
  }


  /**
   * Determines if the given input string contains numeric characters that can be encoded.
   *
   * @param input the string to check for numeric content; cannot be null
   * @return true if the input contains numeric characters that match the numeric pattern, false otherwise
   */
  public static boolean canEncodeNumeric(String input) {
    return NUMERIC_PATTERN.matcher(input).find();
  }

  /**
   * Determines if the given input string can be encoded using the alphanumeric mode
   * as per the QR code encoding specifications. The alphanumeric mode restricts
   * the input to a specific set of characters (digits, uppercase letters, and certain symbols).
   *
   * @param input the string to check for alphanumeric compatibility; cannot be null
   * @return true if the input string contains only characters allowed by the QR code
   * alphanumeric encoding mode, false otherwise
   */
  public static boolean canEncodeAlphaNumeric(String input) {
    return ALPHANUMERIC_PATTERN.matcher(input).find();
  }

  private static int getAlphanumericValue(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
    return switch (c) {
      case ' ' -> 36;
      case '$' -> 37;
      case '%' -> 38;
      case '*' -> 39;
      case '+' -> 40;
      case '-' -> 41;
      case '.' -> 42;
      case '/' -> 43;
      case ':' -> 44;
      default -> -1;
    };
  }

  /**
   * Encodes the given input string into a byte array using the QR code encoding standard.
   * The encoding may choose the most appropriate QR code mode (e.g., numeric, alphanumeric, byte)
   * based on the provided input string and version.
   *
   * @param input   the string to be encoded; cannot be null or empty
   * @param version the QR code version, which affects the encoding parameters; typically ranges from 1 to 40
   * @return a byte array representing the encoded input string, including mode indicators, length information,
   * and the encoded data
   */
  public static byte[] encodeBytes(byte[] input, int version) {
    int i, j, bitOffset, byteOffset;

    int modeIndicator = 0b0100;
    int[] versionBitlengths = {8, 16, 16};
    int blockLength = versionBitlengths[Encoder.getVersionLengthOffset(version)];

    int totalBits = (input.length * 8) + MODE_BIT_COUNT + blockLength;
    byte[] bytes = new byte[(int) Math.ceil(totalBits / 8.0)];

    int metadata = (modeIndicator << blockLength) | input.length;
    int metadataLength = MODE_BIT_COUNT + blockLength;

    // Add metadata (mode indicator and length)
    for (bitOffset = 0; bitOffset < metadataLength; bitOffset++) {
      if ((metadata & (1 << (metadataLength - bitOffset - 1))) == 0) continue;

      byteOffset = bitOffset / 8;
      bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
    }

    // Add input bytes
    for (i = 0, bitOffset = metadataLength; i < input.length; i++) {
      for (j = 0; j < 8; j++) {
        if ((input[i] & (1 << (7 - j))) != 0) {
          byteOffset = bitOffset / 8;
          bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
        }
        bitOffset++;
      }
    }

    return bytes;
  }

  /**
   * Encodes the given alphanumeric string into a byte array, following QR code standards.
   * The encoding includes the mode indicator, length information, and encoded input data.
   *
   * @param input   the alphanumeric string to be encoded; must contain only characters
   *                allowed in the QR code alphanumeric mode (digits, uppercase letters, and some symbols)
   * @param version the QR code version, which affects the encoding parameters; typically ranges from 1 to 40
   * @return a byte array representing the encoded alphanumeric data, including metadata and padding
   */
  public static byte[] encodeAlphaNumeric(String input, int version) {
    int i, j, byteOffset, bitOffset, bitMask, charCode, variableBitLength, firstChar, secondChar;

    int[] versionBitlengths = {9, 11, 13};

    int blockLength = versionBitlengths[Encoder.getVersionLengthOffset(version)];
    int modeIndicator = 0b0010;

    int totalBits = ((input.length() / 2) * 11) + ((input.length() % 2) * 6) + MODE_BIT_COUNT + blockLength;
    byte[] bytes = new byte[(int) Math.ceil(totalBits / 8.0)];

    int metadata = (modeIndicator << blockLength) | input.length();
    int metadataLength = MODE_BIT_COUNT + blockLength;

    // Append the first 4 + n bytes of metadata (mode, payload size)
    for (bitOffset = byteOffset = 0; bitOffset < metadataLength; bitOffset++) {
      if ((metadata & (1 << (metadataLength - bitOffset - 1))) == 0) continue;

      byteOffset = bitOffset / 8;
      bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
    }

    for (i = 0; i < input.length(); i += 2) {
      firstChar = getAlphanumericValue(input.charAt(i));
      secondChar = (i + 1 < input.length()) ? getAlphanumericValue(input.charAt(i + 1)) : -1;

      charCode = (secondChar == -1) ? firstChar : (firstChar * 45 + secondChar);
      variableBitLength = (secondChar == -1) ? 6 : 11;

      for (j = 0; j < variableBitLength; j++) {
        bitMask = 1 << (variableBitLength - j - 1);

        if ((charCode & bitMask) != 0) {
          bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
        }

        bitOffset++;
        byteOffset = bitOffset / 8;
      }
    }

    return bytes;
  }

  /**
   * Encodes a given array of numeric digits into a byte array suitable for QR code encoding,
   * following the QR code numeric mode specification.
   *
   * @param input   an array of integers representing the numeric digits to encode; values must be in the range 0-9
   * @param version the QR code version, which dictates encoding parameters; typically ranges from 1 to 40
   * @return a byte array containing the encoded numeric data with appropriate metadata and padding
   */
  public static byte[] encodeNumeric(int[] input, int version) {
    int i, j, bitMask, computedNumber, variableBitLength, bitOffset, byteOffset;

    int modeIndicator = 0b0001;
    int[] versionBitlengths = {10, 12, 14};

    var blockLength = versionBitlengths[Encoder.getVersionLengthOffset(version)];
    byte[] bytes = new byte[(int) (Math.ceil((input.length / 3.0f) * 10 + LENGTH_BIT_COUNT + MODE_BIT_COUNT) / 8.0)];

    // Metadata for payload; mode indicator and input length (4 + n bytes)
    int metadata = (modeIndicator << blockLength) | input.length;
    int metadataLength = MODE_BIT_COUNT + blockLength;

    for (bitOffset = 0; bitOffset < metadataLength; bitOffset++) {
      if ((metadata & (1 << (metadataLength - bitOffset - 1))) == 0) continue;

      byteOffset = bitOffset / 8;
      bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
    }

    for (i = 0, bitOffset = metadataLength; i < input.length; i += 3) {
      // Compose a single number from three digits
      for (j = computedNumber = 0; j < 3 && i + j < input.length; j++) {
        computedNumber = computedNumber * 10 + input[i + j];
      }

      variableBitLength = j == 3 ? 10 : j == 2 ? 7 : 4;

      for (j = 0; j < variableBitLength; j++) {
        bitMask = 1 << (variableBitLength - j - 1);
        byteOffset = bitOffset / 8;

        if ((computedNumber & bitMask) != 0) {
          bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
        }

        bitOffset++;
      }
    }

    return bytes;
  }

  /**
   * Computes and returns the version information for the QR code as an integer
   * encoded with error correction bits. This method applies a polynomial division
   * to calculate the remainder, which is used to provide version information
   * conforming to QR code standards. The result combines the version number
   * with the calculated error correction bits.
   *
   * @param version                 The version of the QR code to use
   * @param polynomialGeneratorMask The generator polynomial mask
   * @return an integer representing the combined version number and error
   * correction bits for version information in a QR code.
   */
  public static int ECVersionInfo(int version, int polynomialGeneratorMask) {
    int paddedVersionBits = (version << 12);
    int paddedGeneratorPolynomial = polynomialGeneratorMask;

    int versionBitLength = Encoder.getBitLength(paddedVersionBits);
    int generatorBitLength = Encoder.getBitLength(paddedGeneratorPolynomial);

    do {
      // right-pad generator to match formatting string length
      paddedGeneratorPolynomial = polynomialGeneratorMask << (versionBitLength - generatorBitLength);
      paddedVersionBits ^= paddedGeneratorPolynomial;
      versionBitLength = Encoder.getBitLength(paddedVersionBits);
    } while (versionBitLength > 12);

    return version << 12 | paddedVersionBits;
  }

  /**
   * Computes and returns the format information for error correction and mask patterns
   * as a 15-bit integer encoded with error correction and masking logic. This value is
   * used to draw the format information in a QR code.
   *
   * @param errorCorrection         The error correction mask to encode
   * @param maskPattern             The mask pattern to encode
   * @param polynomialGeneratorMask The polynomial mask to use in the encoding
   * @return a 15-bit integer representing the formatted error correction and mask pattern
   * information, including error correction bits and a final XOR with a predefined
   * QR mask constant.
   */
  public static int ECFormatInfo(ErrorCorrection errorCorrection, MaskPattern maskPattern, int polynomialGeneratorMask) {
    int formatted = (errorCorrection.getMask() << 3 | maskPattern.getMask());
    int paddedFormattingBits = (formatted << 10) & 0x7FFF; // right-pad to ensure Length = 15 bits
    int paddedGeneratorPolynomial = polynomialGeneratorMask;

    int formatBitLength = Encoder.getBitLength(paddedFormattingBits);
    int generatorBitLength = Encoder.getBitLength(paddedGeneratorPolynomial);

    while (formatBitLength > 11) {
      // right-pad generator to match formatting string length
      if (generatorBitLength < formatBitLength) {
        paddedGeneratorPolynomial = polynomialGeneratorMask << (formatBitLength - generatorBitLength);
      }
      paddedFormattingBits ^= paddedGeneratorPolynomial;
      formatBitLength = Encoder.getBitLength(paddedFormattingBits);
    }

    // Final X-or, no need to pad generator since length is equal
    paddedFormattingBits ^= polynomialGeneratorMask;

    // right-pad if the length is below 10
    if (formatBitLength < 10) {
      paddedFormattingBits <<= (10 - formatBitLength);
    }

    int combined = formatted << 10 | paddedFormattingBits;

    return combined ^ QR_MASK;
  }

  /**
   * Combines the input data bytes with generated error correction (EC) codewords,
   * forming the final codeword sequence for encoding.
   * <p>
   * This method calculates the required EC codewords for the given data bytes
   * and appends them to the original data, producing a complete sequence
   * suitable for QR code or similar encoding standards.
   *
   * @param data the input byte array containing data to be encoded; cannot be null
   *             and must contain the necessary data bytes.
   * @return a byte array containing the concatenation of the input data bytes
   * and the computed error correction codewords.
   */
  public static byte[] createCodewordsForBytes(byte[] data) {
    // Calculate required EC codewords based on data length
    int ecCodewords = RS_GENERATOR_POLY.length - 1;
    int totalLength = data.length + ecCodewords;

    byte[] codewords = new byte[totalLength];

    // Copy data bytes
    System.arraycopy(data, 0, codewords, 0, data.length);

    // Calculate EC codewords
    byte[] ecBytes = calculateECCodewords(data, ecCodewords);

    // Append EC codewords
    System.arraycopy(ecBytes, 0, codewords, data.length, ecCodewords);

    return codewords;
  }

  /**
   * Calculates and returns the error correction (EC) codewords for a given data array.
   * The method generates a specified number of EC codewords using Reed-Solomon algorithms.
   *
   * @param data     a byte array representing the input data to calculate the error correction codewords for
   * @param ecLength the number of error correction codewords to generate
   * @return a byte array containing the calculated EC codewords
   */
  private static byte[] calculateECCodewords(byte[] data, int ecLength) {
    byte[] ecBytes = new byte[ecLength];

    for (byte datum : data) {
      int feedback = (datum ^ ecBytes[0]) & 0xFF;

      if (feedback != 0) {
        for (int j = 1; j < ecLength; j++) {
          ecBytes[j - 1] = (byte) (ecBytes[j] ^
              RS_EXP_TABLE[(RS_LOG_TABLE[feedback] + RS_GENERATOR_POLY[ecLength - j]) % 255]);
        }
        ecBytes[ecLength - 1] = (byte) RS_EXP_TABLE[(RS_LOG_TABLE[feedback] + RS_GENERATOR_POLY[0]) % 255];
      } else {
        System.arraycopy(ecBytes, 1, ecBytes, 0, ecLength - 1);
        ecBytes[ecLength - 1] = 0;
      }
    }

    return ecBytes;
  }
}
