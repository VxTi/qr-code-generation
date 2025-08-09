package qr.encoding;

import java.nio.charset.StandardCharsets;

public class NumericStringEncoder implements IStringEncoder {

  @Override
  public boolean[] encode(String input) {
    System.out.println("Encoding data: " + input);
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    int i, remainder;

    for (i = 0; i < bytes.length; i += 3) {
       remainder = bytes.length - i;

       
    }

    return new boolean[0];
  }
}
