package qr.encoding;

import java.util.BitSet;

public class NumericEncoder {

  public static BitSet encode(int[] input) {
    var set = new BitSet();
    System.out.println("Encoding data: " + input);

    int i, remainder, numericBits;

    for (i = 0; i < input.length; i += 3) {
       remainder = input.length - i + 1;

       if (remainder >= 3) {
         numericBits = input[i] | input[i + 1] | input[i + 2];
         continue;
       }

       // Less than two remaining numbers
    }

    return set;
  }
}
