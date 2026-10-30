package com.rexchain.cinema.service;

/** Shared, email-client-friendly HTML for account verification messages. */
final class OtpEmailContent {
    private OtpEmailContent() {}

    static String html(String customerName, String code, int expiryMinutes, boolean passwordReset) {
        String safeName = escape(customerName == null || customerName.isBlank() ? "b&#7841;n" : customerName);
        String title = passwordReset ? "Kh&#244;i ph&#7909;c m&#7853;t kh&#7849;u" : "X&#225;c th&#7921;c t&#224;i kho&#7843;n";
        String description = passwordReset
                ? "Nh&#7853;p m&#227; b&#234;n d&#432;&#7899;i &#273;&#7875; ti&#7871;p t&#7909;c &#273;&#7863;t l&#7841;i m&#7853;t kh&#7849;u Rex Cinemas."
                : "Nh&#7853;p m&#227; b&#234;n d&#432;&#7899;i &#273;&#7875; x&#225;c th&#7921;c email v&#224; ho&#224;n t&#7845;t &#273;&#259;ng k&#253; Rex Cinemas.";
        String ignoreNote = passwordReset
                ? "N&#7871;u b&#7841;n kh&#244;ng y&#234;u c&#7847;u kh&#244;i ph&#7909;c m&#7853;t kh&#7849;u, h&#227;y b&#7887; qua email n&#224;y. M&#7853;t kh&#7849;u hi&#7879;n t&#7841;i c&#7911;a b&#7841;n v&#7851;n an to&#224;n."
                : "N&#7871;u b&#7841;n kh&#244;ng t&#7841;o t&#224;i kho&#7843;n Rex Cinemas, h&#227;y b&#7887; qua email n&#224;y.";

        return """
                <!doctype html>
                <html lang="vi">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#090d18;color:#eef2f8;font-family:Arial,Helvetica,sans-serif">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#090d18;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:560px;border:1px solid #26334b;border-radius:18px;overflow:hidden;background:#111a2b">
                        <tr><td style="padding:25px 30px;background:#151f33;border-bottom:1px solid #2a3750">
                          <div style="font-size:13px;font-weight:800;letter-spacing:3px;color:#f4d52e">REX CINEMAS</div>
                          <div style="margin-top:7px;font-size:11px;letter-spacing:1.5px;color:#a8b6cc">B&#7842;O M&#7852;T T&#192;I KHO&#7842;N</div>
                        </td></tr>
                        <tr><td style="padding:30px 30px 12px">
                          <div style="display:inline-block;padding:7px 11px;border-radius:999px;background:#292b20;color:#f4d52e;font-size:11px;font-weight:700;letter-spacing:.5px">M&#195; D&#217;NG M&#7896;T L&#7846;N</div>
                          <h1 style="margin:17px 0 8px;font-size:25px;line-height:1.3;color:#fff">%s</h1>
                          <p style="margin:0;color:#bdc8d8;font-size:15px;line-height:1.7">Xin ch&#224;o <strong style="color:#fff">%s</strong>, %s</p>
                        </td></tr>
                        <tr><td style="padding:18px 30px 22px">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border:1px solid #39465e;border-radius:14px;background:#0d1524">
                            <tr><td align="center" style="padding:23px 16px 20px">
                              <div style="font-size:11px;letter-spacing:1.5px;color:#9eacc1">M&#195; X&#193;C TH&#7920;C OTP</div>
                              <div style="margin-top:12px;font-size:36px;line-height:1.3;font-weight:800;letter-spacing:10px;color:#f4d52e">%s</div>
                              <div style="margin-top:12px;color:#bdc8d8;font-size:13px">C&#243; hi&#7879;u l&#7921;c trong <strong style="color:#fff">%d ph&#250;t</strong> &middot; Ch&#7881; s&#7917; d&#7909;ng m&#7897;t l&#7847;n</div>
                            </td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 30px 26px">
                          <p style="margin:0;padding:14px 16px;border-left:3px solid #f4d52e;border-radius:7px;background:#18243a;color:#aebbd0;font-size:13px;line-height:1.6">V&#236; an to&#224;n, kh&#244;ng chia s&#7867; m&#227; n&#224;y v&#7899;i b&#7845;t k&#7923; ai, k&#7875; c&#7843; ng&#432;&#7901;i t&#7921; x&#432;ng l&#224; nh&#226;n vi&#234;n Rex Cinemas.</p>
                        </td></tr>
                        <tr><td style="padding:18px 30px;background:#0d1524;color:#8898b0;font-size:12px;line-height:1.6">%s<br>Email &#273;&#432;&#7907;c g&#7917;i t&#7921; &#273;&#7897;ng b&#7903;i Rex Cinemas. Vui l&#242;ng kh&#244;ng tr&#7843; l&#7901;i th&#432; n&#224;y.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body></html>
                """.formatted(title, safeName, description, escape(code), expiryMinutes, ignoreNote);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
