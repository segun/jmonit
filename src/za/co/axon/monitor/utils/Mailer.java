package za.co.axon.monitor.utils;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.JSONException;
import org.json.JSONObject;
import za.co.axon.monitor.config.MonitorSystem;

/**
 *
 * @author Akintayo A. Olusegun
 * @version 1.0
 */
public class Mailer implements Serializable {

    Properties properties;

    public Mailer(MonitorSystem system) throws JSONException {
        properties = new Properties();
        properties.setProperty("mail.host", system.mailServer.host);
        properties.setProperty("mail.user", system.mailServer.user);
        properties.setProperty("mail.pass", system.mailServer.password);
        properties.setProperty("mail.smtp.port", system.mailServer.port + "");
        properties.setProperty("mail.smtp.starttls.enable", system.mailServer.startTLS + "");
        properties.setProperty("mail.smtp.auth", system.mailServer.useAuth + "");
    }

    public boolean sendMail(String fromFullName, String from, String rcpts, String msgContent, String subject) {
        try {
            Authenticator auth = new MailAuthenticator();
            properties.setProperty("mail.smtp.from", from);
            Session session = Session.getInstance(properties, auth);
            Message msg = new MimeMessage(session);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setFrom(new InternetAddress(from, fromFullName));
            msg.setReplyTo(InternetAddress.parse(from, true));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rcpts, true));
            msg.setHeader("Precedence", "bulk");
            msg.setContent(msgContent, "text/html");
            Transport.send(msg);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(Mailer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public String contructHTMLMessage(String message, String header) {
        String msg = "<html><body>"
                + "<h3 align=center>" + header + "</h3><br />"
                + "<b>" + message + "</b></body></html>";
        return msg;
    }

    private class MailAuthenticator extends Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(properties.getProperty("mail.user"), properties.getProperty("mail.pass"));
        }
    };
}
