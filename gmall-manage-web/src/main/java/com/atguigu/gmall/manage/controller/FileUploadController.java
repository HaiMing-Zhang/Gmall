package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl;
    /**
     * 上传图片 并返回路径
     * @return
     */
    @PostMapping("/fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file){
        String fileImgUrl = fileUrl;
        String Configfile = this.getClass().getResource("/tracker.conf").getFile();
        try {
            ClientGlobal.init(Configfile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            String fileExtName = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            String[] upload_file = storageClient.upload_file(file.getBytes(), fileExtName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String s = upload_file[i];
                System.out.println("s = " + s);
                fileImgUrl += "/"+s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileImgUrl;
    }
}
