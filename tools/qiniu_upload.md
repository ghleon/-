
```python

1.创建上传图片工具类

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class QiniuUtil {

    @Value("${qiniu.image.access_key}")
    private String accessKey;

    @Value("${qiniu.image.secret_key}")
    private String secretKey;

    @Value("${qiniu.image.bucket}")
    private String bucket;

    @Value("${qiniu.image.domain}")
    private String domain;

    //上传文件夹
    private static final String prefix = "distribute/poster/";

    //刷新接口
    private static final String REFRESH = "http://fusion.qiniuapi.com/v2/tune/refresh";

    //签发access_token字符串参数
    private static final String SIGNINGSTR = "/v2/tune/refresh\n";

    private static final ThreadLocal<SimpleDateFormat> FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    /**
     * 上传
     *
     * @param data
     * @return
     */
    public DefaultPutRet uploadImage(byte[] data, String resourceName) {

        if (Preconditions.isBlank(resourceName)) {
            String nowDate = FORMAT.get().format(new Date());
            resourceName = String.format("%s%s/%s", prefix, nowDate, UUID.randomUUID().toString());
        }

        return uploadImage(null, data, resourceName);
    }

    /**
     * 上传
     *
     * @param data
     * @param dataByte
     * @param resourceName
     * @return
     */
    private String upload(File data, byte[] dataByte, String resourceName) {
        Configuration cfg = new Configuration(Zone.zone0());
        UploadManager uploadManager = new UploadManager(cfg);

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket, resourceName);

        try {
            Response response;
            if (data == null) {
                response = uploadManager.put(dataByte, resourceName, upToken);
            } else {
                response = uploadManager.put(data, resourceName, upToken);
            }
            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info("----->七牛返回参数：{}", JSON.toJSONString(putRet));
            if (Preconditions.isNotBlank(putRet.hash)) {
                return domain + resourceName;
            }
        } catch (QiniuException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 返回七牛参数
     *
     * @param data
     * @param dataByte
     * @param resourceName
     * @return
     */
    private DefaultPutRet uploadImage(File data, byte[] dataByte, String resourceName) {
        Configuration cfg = new Configuration(Zone.zone0());
        UploadManager uploadManager = new UploadManager(cfg);

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket, resourceName);

        try {
            Response response;
            if (data == null) {
                response = uploadManager.put(dataByte, resourceName, upToken);
            } else {
                response = uploadManager.put(data, resourceName, upToken);
            }
            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info("----->七牛返回参数：{}", JSON.toJSONString(putRet));
            if (Preconditions.isNotBlank(putRet.hash)) {
                //刷新缓存
                refreshCache(domain + putRet.key);
            }

            return putRet;
        } catch (QiniuException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 刷新缓存
     *
     * @param imageUrl
     */
    private void refreshCache(String imageUrl) {
        Auth auth = Auth.create(accessKey, secretKey);
        String access_token = auth.sign(SIGNINGSTR);
        log.info(access_token);
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        arr.add(imageUrl);
        obj.put("urls", arr);
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("Authorization", "QBox " + access_token);
        httpHeaders.add("Content-Type", "application/json");
        try {
            String result = AsyncHttpUtils.syncPost(REFRESH, JSON.toJSONString(obj), httpHeaders);
            log.info(result);
        } catch (HttpServiceException e) {
            e.printStackTrace();
        }
    }
}
```

