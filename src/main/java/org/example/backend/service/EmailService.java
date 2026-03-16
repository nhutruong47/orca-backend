package org.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public boolean isEmailConfigured() {
        return mailSender != null
                && mailUsername != null && !mailUsername.isBlank()
                && !mailUsername.equals("your-email@gmail.com")
                && mailPassword != null && !mailPassword.isBlank()
                && !mailPassword.equals("your-app-password");
    }

    /**
     * Gửi email đồng bộ — trả về true nếu thành công, false nếu lỗi.
     */
    public boolean sendInvitationEmail(String toEmail, String teamName, String inviterName, String inviteLink) {
        if (!isEmailConfigured()) {
            System.out.println("[EMAIL - DEV MODE] Chưa cấu hình SMTP thật.");
            System.out.println("[EMAIL - DEV MODE] Lời mời gửi tới: " + toEmail);
            System.out.println("[EMAIL - DEV MODE] Link mời: " + inviteLink);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject("Lời mời tham gia nhóm: " + teamName + " trên ORCA");
            message.setText(
                    "Xin chào!\n\n" +
                            inviterName + " đã mời bạn vào nhóm '" + teamName + "' trên ORCA.\n\n" +
                            "Nhấp vào link để tham gia:\n" + inviteLink + "\n\n" +
                            "(Nếu chưa có tài khoản, bạn sẽ được hướng dẫn tạo tài khoản.)\n\n" +
                            "Trân trọng,\nĐội ngũ ORCA");
            mailSender.send(message);
            System.out.println("[EMAIL] ✅ Đã gửi thành công tới " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] ❌ Không thể gửi tới " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage());
        }
    }
}
