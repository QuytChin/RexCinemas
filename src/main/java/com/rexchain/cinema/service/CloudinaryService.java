package com.rexchain.cinema.service;
import com.cloudinary.Cloudinary; import com.cloudinary.utils.ObjectUtils; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile; import java.io.IOException; import java.util.Map;
@Service
public class CloudinaryService{
 private final Cloudinary cloudinary; private final String cloudName; public CloudinaryService(Cloudinary c,@Value("${cloudinary.cloud-name:}")String cloudName){this.cloudinary=c;this.cloudName=cloudName;}
 public boolean configured(){return cloudName!=null&&!cloudName.isBlank();}
 public String uploadMoviePoster(MultipartFile file)throws IOException{return upload(file,"rex-chain/movies");}
 public String uploadAvatar(MultipartFile file)throws IOException{return upload(file,"rex-chain/avatars");}
 public String uploadComboImage(MultipartFile file)throws IOException{return upload(file,"rex-chain/combos");}
 public String uploadContentImage(MultipartFile file)throws IOException{return upload(file,"rex-chain/content");}
 private String upload(MultipartFile file,String folder)throws IOException{if(!configured())throw new IllegalStateException("Chưa cấu hình Cloudinary");Map<?,?> result=cloudinary.uploader().upload(file.getBytes(),ObjectUtils.asMap("folder",folder,"resource_type","image"));return String.valueOf(result.get("secure_url"));}
}
