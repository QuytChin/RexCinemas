package com.rexchain.cinema.service;
import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.nio.charset.StandardCharsets; import java.util.HexFormat;
public final class CryptoUtil{
 private CryptoUtil(){}
 public static String hmac(String algorithm,String secret,String data){try{Mac mac=Mac.getInstance(algorithm);mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),algorithm));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Không thể tạo chữ ký thanh toán",e);}}
}
