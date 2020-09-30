package com.zhxd.ai;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import com.aliyun.ocr.Client;
import com.aliyun.ocr.models.*;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.teautil.models.RuntimeOptions;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author joffre
 * @date 2020/4/6
 */
@Service
public class OcrService {

    private Client ocrClient;
    private RuntimeOptions runtime;

    @Value("${viapi.accessKeyId}")
    private String accessKeyId;
    @Value("${viapi.accessKeySecret}")
    private String accessKeySecret;

    @PostConstruct
    private void init() throws Exception {
        Config config = new Config();
        config.type = "access_key";
        config.regionId = "cn-shanghai";
        config.accessKeyId = accessKeyId;
        config.accessKeySecret = accessKeySecret;
        config.endpoint = "ocr.cn-shanghai.aliyuncs.com";

        ocrClient = new Client(config);
        runtime = new RuntimeOptions();


    }

    /**
     * 识别身份证
     *
     * @param filePath 待识别图片路径
     * @param side     正面/反面
     * @return Map<String   ,       String>
     * @throws Exception 异常
     */
    public Map<String, String> recognizeIdCard(String filePath, String side) throws Exception {

        RecognizeIdentityCardAdvanceRequest request = new RecognizeIdentityCardAdvanceRequest();
        request.imageURLObject = Files.newInputStream(Paths.get(filePath));
        request.side = side;
        RecognizeIdentityCardResponse response = ocrClient.recognizeIdentityCardAdvance(request, runtime);

        if ("face".equals(side)) {
            return JSON.parseObject(JSON.toJSONString(response.data.frontResult), new TypeReference<Map<String, String>>() {});
        } else {
            return JSON.parseObject(JSON.toJSONString(response.data.backResult), new TypeReference<Map<String, String>>() {});
        }
    }

    /**
     * 识别车牌、车型
     *
     * @param filePath 待识别图片OSS路径
     * @return Map<String   ,   String> 返回结果
     * @throws Exception 异常
     */
    public Map<String, String> recognizeLicensePlate(String filePath) throws Exception {
        RecognizeLicensePlateRequest request = new RecognizeLicensePlateRequest();
        System.out.println(filePath);
        request.imageURL = filePath;
        RecognizeLicensePlateResponse response = ocrClient.recognizeLicensePlate(request, runtime);
        return JSON.parseObject(JSON.toJSONString(response.data.plates), new TypeReference<Map<String, String>>() {
        });
    }

    /**
     * 识别驾照
     *
     * @param filePath 待识别图片OSS路径
     * @param side     主页/副页
     * @return Map<String   ,   String> 返回结果
     * @throws Exception 异常
     */
    public Map<String, String> recognizeDriverLicense(String filePath, String side) throws Exception {
        RecognizeDriverLicenseRequest request = new RecognizeDriverLicenseRequest();
        request.imageURL = filePath;
        request.side = side;
        RecognizeDriverLicenseResponse response = ocrClient.recognizeDriverLicense(request, runtime);

        if ("face".equals(side)) {
            return JSON.parseObject(JSON.toJSONString(response.data.faceResult), new TypeReference<Map<String, String>>() {
            });
        } else {
            return JSON.parseObject(JSON.toJSONString(response.data.backResult), new TypeReference<Map<String, String>>() {
            });
        }
    }


    /**
     * 上传文件到OSS
     * @param fileName 文件名
     * @param filePath 文件路径
     * @return String oss路径
     */
    public String  uploadFile2OSS(String fileName, String filePath) {
        String endpoint = "http://oss-cn-shanghai.aliyuncs.com";
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建PutObjectRequest对象。
        PutObjectRequest putObjectRequest = new PutObjectRequest("jijiezh-bj", fileName, new File(filePath));
        PutObjectResult putObjectResult =  ossClient.putObject(putObjectRequest);
        //String etag = putObjectResult.getETag();
        //OSSObject ossObject = ossClient.getObject("jijiezh", etag);

        Date expiration = new Date(System.currentTimeMillis()+ 3600 * 1000);
        URL url = ossClient.generatePresignedUrl("jijiezh-bj", fileName, expiration);
        return  url.toString();
    }



}
