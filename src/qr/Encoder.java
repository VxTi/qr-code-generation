package qr;

import qr.encoding.ByteEncoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Encoder {

  private static final int LENGTH_BYTE_COUNT = 2;
  private static final int MODE_BYTE_COUNT = 1;
  private static final int MODE_BIT_LENGTH = 4;

  private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[0-9A-Z $%*+\\-./:]+$");
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");


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

  public static byte[] encode(String data, int version) {
    return encode(data, version, StandardCharsets.UTF_8);
  }

  /**
   * Encodes the specified input data string into a byte array based on the QR code encoding standards.
   * The encoding process chooses the most efficient QR code encoding mode based on the input data:
   * numeric, alphanumeric, or byte mode.
   *
   * @param data the input string to be encoded; cannot be null
   * @param version the QR code version, which dictates encoding parameters; typically ranges from 1 to 40
   * @param encoding the character encoding to use when encoding in byte mode; cannot be null
   * @return a byte array representing the encoded data in the chosen QR code encoding mode
   */
  public static byte[] encode(String data, int version, Charset encoding) {
    // First step; figure out which encoding method is most efficient for the input data.

    // If the entire string matches the numeric pattern, it's composed of only digits
    if (canEncodeNumeric(data)) {
      System.out.println("Encoding numeric: " + data);
      int[] integers = data.chars().map(digit -> digit - '0').toArray();

      return encodeNumeric(integers, version);
    }

    // See if string is composed of only alphanumeric characters
    if (canEncodeAlphaNumeric(data)) {
      System.out.println("Encoding alpha numeric: " + data);
      return encodeAlphaNumeric(data, version);
    }

    return ByteEncoder.encode(data.getBytes(encoding), version);
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
   *         alphanumeric encoding mode, false otherwise
   */
  public static boolean canEncodeAlphaNumeric(String input) {
    return ALPHANUMERIC_PATTERN.matcher(input).find();
  }

  /**
   * Encodes the given alphanumeric string into a byte array, following QR code standards.
   * The encoding includes the mode indicator, length information, and encoded input data.
   *
   * @param input the alphanumeric string to be encoded; must contain only characters
   *              allowed in the QR code alphanumeric mode (digits, uppercase letters, and some symbols)
   * @param version the QR code version, which affects the encoding parameters; typically ranges from 1 to 40
   * @return a byte array representing the encoded alphanumeric data, including metadata and padding
   */
  public static byte[] encodeAlphaNumeric(String input, int version) {
    int[] versionBitlengths = { 9, 11, 13};
    int modeIndicator = 0b0010;

    byte[] bytes = new byte[input.length() + LENGTH_BYTE_COUNT + MODE_BYTE_COUNT];

    int blockLength = versionBitlengths[Encoder.getVersionLengthOffset(version)];
    int i, j, byteOffset, bitOffset, bitMask, charCode, variableBitLength;

    int metadata = (modeIndicator << blockLength) | input.length();
    int metadataLength = MODE_BIT_LENGTH + blockLength;

    // Append the first 4 + n bytes of metadata (mode, payload size)
    for (bitOffset = 0; bitOffset < metadataLength; bitOffset++) {
      if ((metadata & (1 << (metadataLength - bitOffset - 1))) == 0) continue;

      byteOffset = bitOffset / 8;
      bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
    }

    for (i = 0; i < input.length(); i += 2) {
      charCode = i + 1 < input.length()
          ? (input.charAt(i) * 45) | input.charAt(i + 1)
          : input.charAt(i);
      variableBitLength = i + 1 < input.length() ? 11 : 6;

      for (j = 0; j < variableBitLength; j++) {
        bitMask = 1 << (blockLength - j - 1);
        bitOffset++;
        byteOffset = bitOffset / 8;

        if ((charCode & bitMask) == 0) continue;

        bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
      }
    }

    return bytes;
  }

  /**
   * Encodes a given array of numeric digits into a byte array suitable for QR code encoding,
   * following the QR code numeric mode specification.
   *
   * @param input an array of integers representing the numeric digits to encode; values must be in the range 0-9
   * @param version the QR code version, which dictates encoding parameters; typically ranges from 1 to 40
   * @return a byte array containing the encoded numeric data with appropriate metadata and padding
   */
  public static byte[] encodeNumeric(int[] input, int version) {
    int i, j, bitMask, computedNumber, variableBitLength, bitOffset, byteOffset;

    int modeIndicator = 0b0001;
    int[] versionBitlengths = {10, 12, 14};

    var blockLength = versionBitlengths[Encoder.getVersionLengthOffset(version)];
    byte[] bytes = new byte[(int) Math.ceil(input.length / 3.0) + LENGTH_BYTE_COUNT + MODE_BYTE_COUNT];

    // Metadata for payload; mode indicator and input length (4 + n bytes)
    int metadata = (modeIndicator << blockLength) | input.length;
    int metadataLength = MODE_BIT_LENGTH + blockLength;

    for (bitOffset = 0; bitOffset < metadataLength; bitOffset++) {
      if ((metadata & (1 << (metadataLength - bitOffset - 1))) == 0) continue;

      byteOffset = bitOffset / 8;
      bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
    }

    for (i = 0; i < input.length; i += 3) {
      // Compose a single number from three digits
      for (j = computedNumber = 0; j < 3 && i + j < input.length; j++) {
        computedNumber = computedNumber * 10 + input[i + j];
      }

      variableBitLength = computedNumber >= 100 ? 10 : computedNumber >= 10 ? 7 : 4;

      for (j = 0; j < blockLength; j++) {
        bitMask = 1 << (blockLength - j - 1);
        bitOffset++;
        byteOffset = bitOffset / 8;

        // Won't break out of the loop, since we have to keep incrementing the bitOffset
        if ((computedNumber & bitMask) == 0 || j >= variableBitLength) continue;

        bytes[byteOffset] |= (byte) (1 << (7 - (bitOffset % 8)));
      }
    }

    return bytes;
  }
}
