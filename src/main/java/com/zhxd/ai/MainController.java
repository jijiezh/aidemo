package com.zhxd.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.alibaba.fastjson.JSON;

import com.aliyun.tea.TeaException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author joffre
 * @date 2020/4/6
 */
@Controller
@RequestMapping("/")
public class MainController {
    //身份证
    private static final String  RECOGNIZE_TTYPE_IDCARD = "RecognizeIdentityCard";
    //车牌
    private static final String  RECOGNIZE_TTYPE_LICENSEPLATE = "RecognizeLicensePlate";
    //驾驶证
    private static final String  RECOGNIZE_TTYPE_DRIVERLICENSE = "RecognizeDriverLicense";

    private String uploadDirectory;
    private OcrService ocrService;

    //身份证 正面
    private List<String> faceImages;
    //身份证 反面
    private List<String> backImages;
    //身份证 数据
    private List<Map<String, String>> faceResults;
    //身份证 数据
    private List<Map<String, String>> backResults;

    //行驶证 正面
    private List<String> driverImages;
    //行驶证 反面
    private List<String> driverbImages;
    //行驶证 数据
    private List<Map<String, String>> driverResults;
    //行驶证 数据
    private List<Map<String, String>> driverbResults;

    //车牌 正面
    private List<String> plateImages;
    //车牌 数据
    private List<Map<String, String>> plateResults;



    private String recognizeType ;

    public MainController(@Value("${file.upload.path}") String uploadDirectory, OcrService ocrService) {
        this.uploadDirectory = uploadDirectory;
        this.ocrService = ocrService;
        faceImages = new ArrayList<>();
        backImages = new ArrayList<>();
        faceResults = new ArrayList<>();
        backResults = new ArrayList<>();

        plateImages = new ArrayList<>();
        plateResults = new ArrayList<>();

        driverImages = new ArrayList<>();
        driverbImages = new ArrayList<>();
        driverResults = new ArrayList<>();
        driverbResults = new ArrayList<>();
    }

    private String saveFile(MultipartFile file) throws Exception {
        String suffix = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
        String filename = UUID.randomUUID().toString() + "." + suffix;
        Path path = Paths.get(uploadDirectory + filename);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    @RequestMapping("/")
    public String index(Model model) {


        if (!CollectionUtils.isEmpty(faceImages) && faceImages.size() == backImages.size()) {
            model.addAttribute("faceImage", faceImages.get(faceImages.size() - 1));
            model.addAttribute("faceResult", faceResults.get(faceResults.size() - 1));
            model.addAttribute("backImage", backImages.get(backImages.size() - 1));
            model.addAttribute("backResult", backResults.get(backResults.size() - 1));
            model.addAttribute("recognizeType",this.recognizeType);
        }else if(!CollectionUtils.isEmpty(plateImages)){
            model.addAttribute("plateImages", plateImages.get(plateImages.size() - 1));
            model.addAttribute("plateResults", plateResults.get(plateResults.size() - 1));
            model.addAttribute("recognizeType",this.recognizeType);
        }else if(!CollectionUtils.isEmpty(driverImages)  && driverImages.size() == driverbImages.size() ){
            model.addAttribute("driverImages", driverImages.get(driverImages.size() - 1));
            model.addAttribute("driverResults", driverResults.get(driverResults.size() - 1));
            model.addAttribute("driverbImages", driverbImages.get(driverbImages.size() - 1));
            model.addAttribute("driverbResults", driverbResults.get(driverbResults.size() - 1));
            model.addAttribute("recognizeType",this.recognizeType);
        }

        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("type") String recognizeType, @RequestParam("face") MultipartFile face, @RequestParam(required = false,value="back") MultipartFile back, RedirectAttributes attributes) {
        if (face.isEmpty() && back.isEmpty()) {
            attributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/";
        }

        String errorMessage = null;
        try {
            Path dir = Paths.get(uploadDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            if (!face.isEmpty()) {
                String filename = saveFile(face);
                if(RECOGNIZE_TTYPE_IDCARD.equalsIgnoreCase(recognizeType)){
                    Map<String, String> res = ocrService.recognizeIdCard(uploadDirectory + filename, "face");
                    faceImages.add("/aidemo/images/" + filename);
                    faceResults.add(res);
                    this.recognizeType = RECOGNIZE_TTYPE_IDCARD;
                }else if(RECOGNIZE_TTYPE_LICENSEPLATE.equalsIgnoreCase(recognizeType)){
                    String imgURL =  ocrService.uploadFile2OSS(filename,uploadDirectory+filename);
                    Map<String, String> res = ocrService.recognizeLicensePlate(imgURL);
                    plateImages.add("/aidemo/images/" + filename);
                    plateResults.add(res);

                    faceImages.clear();
                    backImages.clear();
                    faceResults.clear();
                    backResults.clear();

                    driverImages.clear();
                    driverbImages.clear();
                    driverResults.clear();
                    driverbResults.clear();

                    this.recognizeType = RECOGNIZE_TTYPE_LICENSEPLATE;
                }else if(RECOGNIZE_TTYPE_DRIVERLICENSE.equalsIgnoreCase(recognizeType)){
                    String imgURL =  ocrService.uploadFile2OSS(filename,uploadDirectory+filename);
                    Map<String, String> res = ocrService.recognizeDriverLicense(imgURL,"face");
                    driverImages.add("/aidemo/images/" + filename);
                    driverResults.add(res);
                    this.recognizeType =RECOGNIZE_TTYPE_DRIVERLICENSE;

                    faceImages.clear();
                    backImages.clear();
                    faceResults.clear();
                    backResults.clear();

                    plateImages.clear();
                    plateResults.clear();
                }
            }
            if (back !=null && !back.isEmpty()) {
                String filename = saveFile(back);
                if(RECOGNIZE_TTYPE_IDCARD.equalsIgnoreCase(recognizeType)){
                    Map<String, String> res = ocrService.recognizeIdCard(uploadDirectory + filename, "back");
                    backImages.add("/aidemo/images/" + filename);
                    backResults.add(res);
                    this.recognizeType =RECOGNIZE_TTYPE_IDCARD;
                }else if(RECOGNIZE_TTYPE_DRIVERLICENSE.equalsIgnoreCase(recognizeType)){
                    String imgURL =  ocrService.uploadFile2OSS(filename,uploadDirectory+filename);
                    Map<String, String> res = ocrService.recognizeDriverLicense(imgURL, "back");
                    driverbImages.add("/aidemo/images/" + filename);
                    driverbResults.add(res);
                    this.recognizeType =RECOGNIZE_TTYPE_DRIVERLICENSE;
                }
            }

        } catch (TeaException e) {
            e.printStackTrace();
            errorMessage = JSON.toJSONString(e.getData());
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        }

        if (StringUtils.isNotBlank(errorMessage)) {
            attributes.addFlashAttribute("message", errorMessage);
        }
        return "redirect:/";
    }
}