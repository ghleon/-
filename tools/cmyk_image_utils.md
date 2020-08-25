java处理cmyk模式图片转换rgb模式

```python
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import javax.imageio.stream.ImageInputStream;

public class CmykUtils {
    
    private static BufferedImage handleCmykImage(Exception e, String fileUrl) {
        try {
            if ("Unsupported Image Type".equals(e.getMessage())) {
                // Find a suitable ImageReader
                ImageInputStream input = ImageIO.createImageInputStream(url2InputStream(fileUrl));
                Iterator readers = ImageIO.getImageReaders(input);
                ImageReader reader = (ImageReader) readers.next();
                reader.setInput(input);
                String format = reader.getFormatName();
                if ("JPEG".equalsIgnoreCase(format) || "JPG".equalsIgnoreCase(format)) {
                    Raster raster = reader.readRaster(0, null);//CMYK
                    // Stream the image file (the original CMYK image)
                    int w = raster.getWidth();
                    int h = raster.getHeight();
                    byte[] rgb = new byte[w * h * 3];
                    //彩色空间转换
                    float[] Y = raster.getSamples(0, 0, w, h, 0, (float[]) null);
                    float[] Cb = raster.getSamples(0, 0, w, h, 1, (float[]) null);
                    float[] Cr = raster.getSamples(0, 0, w, h, 2, (float[]) null);
                    float[] K = raster.getSamples(0, 0, w, h, 3, (float[]) null);
                    for (int i = 0, imax = Y.length, base = 0; i < imax; i++, base += 3) {
                        float k = 220 - K[i], y = 255 - Y[i], cb = 255 - Cb[i],
                                cr = 255 - Cr[i];
    
                        double val = y + 1.402 * (cr - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                                : (byte) (val + 0.5);
    
                        val = y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base + 1] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                                : (byte) (val + 0.5);
    
                        val = y + 1.772 * (cb - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base + 2] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                                : (byte) (val + 0.5);
                    }
                    raster = Raster.createInterleavedRaster(new DataBufferByte(rgb, rgb.length), w, h, w * 3, 3, new int[]{0, 1, 2}, null);
                    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                    ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    return new BufferedImage(cm, (WritableRaster) raster, true, null);
                }
    
            } else {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    
        return null;
    }
    
    
    /**
     * url 转 InputStream
     *
     * @param fileUrl
     * @return
     * @throws IOException
     */
    public static InputStream url2InputStream(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(30 * 1000);  // 连接超时:30s
        conn.setReadTimeout(1 * 1000 * 1000); // IO超时:1min
        conn.connect();
    
        // 创建输入流读取文件
        InputStream in = conn.getInputStream();
        return in;
    }

    public static void main(String[] args) throws Exception {
        //将这个图片拷贝到你项目根目录下
        BufferedImage bufferedImage = handleUnsupportedImage(new Exception(), "https://img.mvpcs.cn/FoGCXzXyMMA0ewaHTWDGt83Lz86-");

        File outputfile = new File("/Users/leon/pic/cmyk2jpeg.png");
        ImageIO.write(bufferedImage, "png", outputfile);
    }
}




```