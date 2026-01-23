package service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.backend.url}")
    private String backendUrl;

    // Slanje email-a za aktivaciju naloga
    public void sendActivationEmail(String toEmail, String username, String token) {
        
        String activationLink = backendUrl + "/auth/activate?token=" + token;
        
        String subject = "Aktivacija naloga - Video App";
        
        String message = "Zdravo " + username + ",\n\n" +
                        "Hvala što si se registrovao!\n\n" +
                        "Klikni na sledeći link da aktiviraš svoj nalog:\n" +
                        activationLink + "\n\n" +
                        "Link važi 24 sata.\n\n" +
                        "Ako nisi ti kreirao ovaj nalog, ignoriši ovaj email.\n\n" +
                        "Pozdrav,\nVideo App Tim";
        
        sendEmail(toEmail, subject, message);
    }

    // Opšta metoda za slanje email-a
    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        
        mailSender.send(message);
    }
}
