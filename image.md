### java服务端 生成图片
```
1.创建文本基类，可换行

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextLine {

    //单行内容
    private String lineText;
    //单行内容期望宽度
    private int width;

}
```
```
2.创建图片基础工具类 ImageUtil.java

import lombok.extern.slf4j.Slf4j;
import sun.font.FontDesignMetrics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ImageUtil {

    /**
     * 加载图片并缩放
     *
     * @param imageUrl 网络图片地址
     * @param width    缩放宽度
     * @param height   缩放高度
     * @return
     */
    public static BufferedImage loadAndZoomPic(String imageUrl, int width, int height) {
        try {
            BufferedImage src = ImageIO.read(new URL(imageUrl));
            //获取图片原始宽和高
            int srcWidth = src.getWidth();
            int srcHeight = src.getHeight();

            if (width == 0 && height > 0) {
                width = (int) (srcWidth * 1.0 * height / srcHeight);
            } else if (width > 0 && height == 0) {
                height = (int) (srcHeight * 1.0 * width / srcWidth);
            }
            return zoomImage(src, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 图片缩放
     *
     * @param bufImg
     * @param w      缩放的目标宽度
     * @param h      缩放的目标高度
     * @return
     * @throws Exception
     */
    public static BufferedImage zoomImage(BufferedImage bufImg, int w, int h) throws Exception {
        double wr = 0, hr = 0;
        //获取缩放比例
        wr = w * 1.0 / bufImg.getWidth();
        hr = h * 1.0 / bufImg.getHeight();
        AffineTransformOp ato = new AffineTransformOp(AffineTransform.getScaleInstance(wr, hr), null);
        try {
            return ato.filter(bufImg, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 绘制背景颜色
     *
     * @param w 目标宽度
     * @param h 目标高度
     * @return
     * @throws Exception
     */
    public static BufferedImage drawBackground(int w, int h, Color color) throws Exception {
        //背景图
        BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D dg = (Graphics2D) bufferedImage.createGraphics();
        dg.setColor(color);
        dg.fillRect(0, 0, w, h);
        dg.dispose();

        return bufferedImage;
    }

    /**
     * 获取单个字体的宽度
     *
     * @param font 字体样式
     * @param word 字体
     * @return
     */
    public static int getSingleWordWidth(Font font, char word) {
        FontDesignMetrics metrics = FontDesignMetrics.getMetrics(font);
        return metrics.charWidth(word);
    }

    /**
     * 绘制文字，文字定位坐标起始位置：左下角
     *
     * @param lineText 内容
     * @param alpha    透明度
     * @param buffImg
     * @param font     字体样式
     * @param color    字体颜色
     * @param startX   x坐标
     * @param endY     y坐标
     * @return
     */
    public static BufferedImage drawTextGraphics(String lineText, float alpha,
                                                 BufferedImage buffImg, Font font, Color color, int startX, int endY) {
        // 创建Graphics2D对象，用在底图对象上绘图
        Graphics2D g2d = buffImg.createGraphics();
        // 在图形和图像中实现混合和透明效果
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.black);
        g2d.setFont(font);
        g2d.setColor(color);
        g2d.drawString(lineText, startX, endY);

        // 释放图形上下文使用的系统资源
        g2d.dispose();
        return buffImg;
    }

    /**
     * 内容换行
     *
     * @param text  内容
     * @param width 换行宽度
     * @param font  文字样式
     * @return
     */
    public static List<TextLine> split2lines(String text, int width, Font font) {
        List<TextLine> rows = new ArrayList<>();
        int currentWidth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = ImageUtil.getSingleWordWidth(font, text.charAt(i));
            if (currentWidth + charWidth <= width) {
                currentWidth += charWidth;
            } else {
                rows.add(new TextLine(text.substring(start, i), currentWidth));
                start = i;
                currentWidth = charWidth;
            }
        }
        if (start < text.length()) {
            rows.add(new TextLine(text.substring(start), currentWidth));
        }
        return rows;

    }

    /**
     * 绘制图片，图片定位坐标起始位置：左上角
     *
     * @param source 底图
     * @param over   目标图
     * @param x      x坐标
     * @param y      y坐标
     * @param alpha  透明度
     * @return
     * @throws IOException
     */
    public static BufferedImage watermark(BufferedImage source, BufferedImage over, int x, int y, float alpha) throws IOException {
        drawImageGraphics(over, x, y, alpha, source);
        return source;
    }

    /**
     * 绘制图片，图片定位坐标起始位置：左上角
     *
     * @param waterImg 目标图
     * @param x        x坐标
     * @param y        y坐标
     * @param alpha    透明度
     * @param buffImg  底图
     */
    private static void drawImageGraphics(BufferedImage waterImg, int x, int y, float alpha, BufferedImage buffImg) {
        // 创建Graphics2D对象，用在底图对象上绘图
        Graphics2D g2d = buffImg.createGraphics();
        // 获取层图的高度
        int waterImgHeight = waterImg.getHeight();
        // 获取层图的宽度
        int waterImgWidth = waterImg.getWidth();
        // 在图形和图像中实现混合和透明效果
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        // 绘制
        g2d.drawImage(waterImg, x, y, waterImgWidth, waterImgHeight, null);
        // 释放图形上下文使用的系统资源
        g2d.dispose();
    }
  }  
```


```
3.创建测试类

import com.qiniu.storage.model.DefaultPutRet;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Test extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    @Autowired
    private QiniuUtil qiniuUtil;

    @Value("${qiniu.image.domain}")
    private String domain;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DistributePosterRepository distributePosterRepository;


    /**
     * 生成海报(1)
     * 支持长图／单图／四宫格
     *
     * @param openPlatformId
     * @param uatShortName
     * @param productPosterBean
     */
    public ResponseContent productPoster(String openPlatformId, String uatShortName, ProductPosterBean productPosterBean) throws Exception {

        // 根据openPlateformId查找用户
        User user = getAppUser(openPlatformId, uatShortName);
        if (Preconditions.isBlank(user)) {
            throw new MwException("未查询到相关用户信息");
        }
        if (!userService.checkUserJoinCommunity(openPlatformId, uatShortName)) {
            logger.info("未加入星球，openPlatformId={}", openPlatformId);
        }
        // 获取社群app
        App communityApp = getAppByUatShortName(uatShortName);

        String qiNiuKey = null;
        DistributePoster poster = distributePosterRepository.findByDeletedIsFalseAndUserIdAndAppId(user.getId(), communityApp.getId());
        if (Preconditions.isNotBlank(poster)) {
            qiNiuKey = poster.getQnKey();
        }

        Product product = productRepository.findByIdAndDeletedIsFalse(productPosterBean.getProductId());
        //生成海报并上传七牛
        DefaultPutRet putRet = productPosterShareImage(product, productPosterBean.getPosterType(),
                productPosterBean.getPictureBeans(), productPosterBean.isShowSalePrice(), productPosterBean.getQrCodeUrl(), qiNiuKey);
        if (Preconditions.isNotBlank(putRet.hash)) {
            //保存或更新用户海报
            updateProductPoster(poster, user, communityApp, putRet);
            return ResponseContent.buildSuccess("success", domain + putRet.key);
        }

        return ResponseContent.buildSuccess("success");
    }

    /**
     * 生成海报(2)
     *
     * @param
     * @throws Exception
     */
    public DefaultPutRet productPosterShareImage(Product product, PosterTypeEnum posterType, List<PictureBean> pictureBeans, boolean showSalePrice, String qrCodeUrl, String qiNiuKey) {
        try {
            //原始宽度
            int srcWidth = 750;
            //原始高度
            int srcHeight = 400;
            //边距
            int padding = 30;
            List<BufferedImage> images = new ArrayList<>();
            if (PosterTypeEnum.L_PIC == posterType || PosterTypeEnum.M_PIC == posterType) {
                images = convertImage(pictureBeans, 690, 0);
                //长图
                for (BufferedImage image : images) {
                    srcHeight += (image.getHeight() + padding);
                }
                srcHeight += (90 - padding);
            } else {
                //四宫格
                images = convertImage(pictureBeans, 330, 330);
                srcHeight += Math.ceil(images.size() / 2.0) * (330 + padding);
                srcHeight += (90 - padding);
            }

            //背景图
            BufferedImage bufferedImage = ImageUtil.drawBackground(srcWidth, srcHeight, Color.WHITE);

            //绘制海报头部
            bufferedImage = drawHead(bufferedImage, qrCodeUrl, product, padding, showSalePrice, srcWidth);

            int lastY = 400;
            //图片
            if (PosterTypeEnum.F_PIC == posterType) {
                //四宫格
                for (int i = 0; i < images.size(); i++) {
                    bufferedImage = ImageUtil.watermark(bufferedImage, images.get(i), padding + (i % 2) * (330 + 30), lastY, 1.0f);
                    lastY += (i % 2) * (330 + 30);
                }
            } else {
                //长图 or one pic
                for (int i = 0; i < images.size(); i++) {
                    bufferedImage = ImageUtil.watermark(bufferedImage, images.get(i), padding, lastY, 1.0f);
                    lastY += (images.get(i).getHeight() + padding);
                }
            }

            byte[] bytes = ImageUtil.bufferedImageToByte(bufferedImage, "png");
            byte[] compressBytes = ImageUtil.compress(bytes, 1, "JPEG");
            return qiniuUtil.uploadImage(compressBytes, qiNiuKey);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * 获取images
     *
     * @param pictureBeans 图片
     * @param width        图片宽度
     * @param height       图片高度
     * @return
     */
    private static List<BufferedImage> convertImage(List<PictureBean> pictureBeans, int width, int height) {
        List<BufferedImage> images = new ArrayList<>();
        pictureBeans.forEach(bean -> {
            BufferedImage image = ImageUtil.loadAndZoomPic(bean.getImageUrl(), width, height);
            if (Preconditions.isNotBlank(image)) {
                images.add(image);
            }
        });
        return images;
    }

    /**
     * 绘制标题和描述二维码
     *
     * @return
     */
    private static BufferedImage drawHead(BufferedImage bufferedImage, String qrCode, Product product, int padding, boolean showSalePrice, int srcWidth) throws IOException {

        //绘制
        String title = product.getName();
        Font font = new Font("PingFang SC", Font.BOLD, 32);
        List<TextLine> list = ImageUtil.split2lines(title, 690, font);
        for (int i = 0; i < list.size(); i++) {
            bufferedImage = ImageUtil.drawTextGraphics(list.get(i).getLineText(), 1.0f, bufferedImage, font, new Color(74, 74, 74), 30, 60 + (i + 1) * 32 + i * 20);
        }

        //绘制价格
        if (showSalePrice) {
            String price = "¥ " + product.getSalePrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
            font = new Font("PingFang SC", Font.BOLD, 36);
            list = ImageUtil.split2lines(price, srcWidth, font);
            for (int i = 0; i < list.size(); i++) {
                bufferedImage = ImageUtil.drawTextGraphics(list.get(i).getLineText(), 1.0f, bufferedImage, font, new Color(255, 174, 40), srcWidth - 2 * padding - list.get(i).getWidth() + 18, 116 + (i + 1) * 36);
            }
        }

        //绘制描述
        String description = product.getDescription();
        font = new Font("PingFang SC", Font.BOLD, 24);
        list = ImageUtil.split2lines(description, 530, font);
        for (int i = 0; i < list.size(); i++) {
            logger.info(list.get(i).getLineText());
            logger.info("width:" + list.get(i).getWidth());
            bufferedImage = ImageUtil.drawTextGraphics(list.get(i).getLineText(), 1.0f, bufferedImage, font, new Color(155, 155, 155), padding, 220 + (i + 1) * 24 + i * 12);
        }

        //二维码
        BufferedImage qcodeImg = QrcodeUtil.generatorQrCode(qrCode, 150, 150);
        bufferedImage = ImageUtil.watermark(bufferedImage, qcodeImg, srcWidth - 2 * padding - 110, 206, 1.0f);

        return bufferedImage;
    }

    //绘图
    private static BufferedImage drawPic(BufferedImage bufferedImage, String imageUrl, int width, int height, int x, int y) throws Exception {
        BufferedImage src = ImageIO.read(new URL(imageUrl));
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        if (width == 0 && height > 0) {
            width = (int) (srcWidth * 1.0 * height / srcHeight);
        } else if (width > 0 && height == 0) {
            height = (int) (srcHeight * 1.0 * width / srcWidth);
        }

        bufferedImage = ImageUtil.watermark(bufferedImage, ImageUtil.zoomImage(src, width, height), x, y, 1.0f);
        return bufferedImage;
    }

    /**
     * 保存或更新用户海报
     *
     * @param poster
     * @param user
     * @param communityApp
     * @throws Exception
     */
    private void updateProductPoster(DistributePoster poster, User user, App communityApp, DefaultPutRet putRet) throws Exception {
        if (Preconditions.isBlank(poster)) {
            poster = new DistributePoster();
            poster.setAppId(communityApp.getId());
            poster.setUserId(user.getId());
        }
        poster.setImageUrl(domain + putRet.key);
        poster.setQiNiuHash(putRet.hash);
        poster.setQnKey(putRet.key);
        distributePosterRepository.save(poster);
    }

    public static void main(String[] args) throws Exception {
        int padding = 30;
        BufferedImage bufferedImage = ImageUtil.drawBackground(750, 1500, Color.WHITE);

        String title = "商品名称什显示十个字商品名称什显示十个字商品名称什显示十个";
        Font font = new Font("PingFang SC", Font.BOLD, 32);
        List<TextLine> list = ImageUtil.split2lines(title, 690, font);
        for (int i = 0; i < list.size(); i++) {
            bufferedImage = ImageUtil.drawStringGrapics(list.get(i).getLineText(), 1.0f, bufferedImage, "PingFang SC",
                    32, Font.BOLD, new Color(74, 74, 74), 30, 60 + (i + 1) * 32 + i * 20);
        }

        String price = "$9995.45";
        font = new Font("PingFang SC", Font.BOLD, 36);
        list = ImageUtil.split2lines("商品名称什显", 750, font);
        for (int i = 0; i < list.size(); i++) {
            bufferedImage = ImageUtil.drawStringGrapics(list.get(i).getLineText(), 1.0f, bufferedImage, "PingFang SC",
                    36, Font.BOLD, new Color(255, 174, 40), 750 - 2 * padding - list.get(i).getWidth() + 18, 116 + (i + 1) * 36);
        }

        //商品名称（需要换行）
        String name = "自从《新个税法》实施以后，谁更受2益就成为4全民焦点。起征点多少合适？谁将受益？谁被“多征税”？税改是“劫富济贫”？我们梳理了近40年更使身处各用一般工薪阶层，土豪请绕行）";
        font = new Font("PingFang SC", Font.BOLD, 24);
        list = ImageUtil.split2lines(name, 530, font);
        for (int i = 0; i < list.size(); i++) {
            logger.info(list.get(i).getLineText());
            logger.info("width:" + list.get(i).getWidth());
            bufferedImage = ImageUtil.drawStringGrapics(list.get(i).getLineText(), 1.0f, bufferedImage, "PingFang SC", 24, Font.BOLD, new Color(155, 155, 155), padding, 220 + (i + 1) * 24 + i * 12);
        }

        //二维码
        BufferedImage qcodeImg = QrcodeUtil.generatorQrCode("https://tokencn-kol.liaoyantech.cn/community-app/MAT/mall/googs-details?code=MAT&id=21&isdistribution=&wid=7", 150, 150);
        bufferedImage = ImageUtil.watermark(bufferedImage, qcodeImg, 750 - 2 * padding - 110, 210, 1.0f);

        bufferedImage = drawPic(bufferedImage, "https://img.liaoyantech.cn/FrORS2WzPcOU6KDhWhv16_umdglF", 690, 0, padding, 420);

        bufferedImage = drawPic(bufferedImage, "https://img.liaoyantech.cn/FrORS2WzPcOU6KDhWhv16_umdglF", 690, 0, padding, 420 + 690 + padding);

        File outputfile = new File("/Users/leon/pic/test.png");
        ImageIO.write(bufferedImage, "png", outputfile);

    }

}

```



