package swapp.printer;

import java.util.EnumMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class BarCodeUtil {

  private BarCodeUtil() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public static encodeAsBitmap(String contents, int img_width, int img_height) {
      return encodeAsBitmap(contents, BarcodeFormat.CODE_128, img_width, img_height);
    }

    public static encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
      String contentsToEncode = contents;
      if (contentsToEncode == null) {
          return null;
      }
      Map<EncodeHintType, Object> hints = null;
      String encoding = guessAppropriateEncoding(contentsToEncode);
      if (encoding != null) {
          hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
          hints.put(EncodeHintType.CHARACTER_SET, encoding);
      }
      MultiFormatWriter writer = new MultiFormatWriter();
      BitMatrix result;
      try {
          result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
      } catch (IllegalArgumentException iae) {
          // Unsupported format
          return null;
      }
      int width = result.getWidth();
      int height = result.getHeight();
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
          int offset = y * width;
          for (int x = 0; x < width; x++) {
          pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
          }
      }

      Bitmap bitmap = Bitmap.createBitmap(width, height,
          Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
      return bitmap;
    }

}