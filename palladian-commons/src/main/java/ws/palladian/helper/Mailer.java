package ws.palladian.helper;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Send mail via SMTP server.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Mailer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Mailer.class);

    private final Properties properties;

    private final Authenticator authenticator;

    /**
     * <p>
     * Create a new {@link SendMail}. The {@link Properties} configuration should usually provide the following
     * information:
     * <ul>
     * <li>mail.smtp.host</li>
     * <li>mail.smtp.socketFactory.port</li>
     * <li>mail.smtp.socketFactory.class</li>
     * <li>mail.smtp.auth</li>
     * <li>mail.smtp.port</li>
     * </ul>
     * </p>
     * 
     * @param properties {@link Properties} object supplying the configuration, not <code>null</code>.
     * @param username The username, not <code>null</code> or empty.
     * @param password The password, not <code>null</code> or empty.
     */
    public Mailer(Properties properties, final String username, final String password) {
        Validate.notNull(properties, "properties must not be null");
        Validate.notEmpty(username, "username must not be empty");
        Validate.notEmpty(password, "password must not be empty");

        this.properties = properties;
        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            };
        };
    }

    /**
     * <p>
     * Send a new (html) mail to multiple recipients.
     * </p>
     * 
     * @param senderAddress The address of the sender, not <code>null</code> or empty.
     * @param senderName The name of the sender, not <code>null</code> or empty.
     * @param recipients The list of recipients, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param content The (html) content of the mail, not <code>null</code>.
     * @param isHtml If true, the content will be sent as html.
     * @param replyToAddresses Addresses to reply to.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String senderAddress, String senderName, Map<RecipientType, List<String>> recipients,
            String subject,
            String content, boolean isHtml, Address... replyToAddresses) {
        Validate.notEmpty(senderAddress, "sender must not be empty");
        Validate.notEmpty(senderName, "sender must not be empty");
        Validate.notEmpty(recipients, "recipients must not be empty");
        Validate.notNull(subject, "subject must not be null");
        Validate.notNull(content, "content must not be null");

        boolean success = false;

        Session mailSession = Session.getDefaultInstance(properties, authenticator);
        Message simpleMessage = new MimeMessage(mailSession);

        try {

            simpleMessage.setFrom(new InternetAddress(senderAddress, senderName));
            for (Entry<RecipientType, List<String>> recipient : recipients.entrySet()) {
                for (String address : recipient.getValue()) {
                    simpleMessage.addRecipient(recipient.getKey(), new InternetAddress(address));
                }
            }
            simpleMessage.setSubject(subject);
            if (replyToAddresses.length > 0) {
                simpleMessage.setReplyTo(replyToAddresses);
            }
            if (isHtml) {
                simpleMessage.setContent(content, "text/html");
            } else {
                simpleMessage.setText(content);
            }

            Transport.send(simpleMessage);

            LOGGER.debug("Successfully sent mail to {} ", recipients);
            success = true;

        } catch (AddressException e) {
            LOGGER.error("Exception while sending: {}", e);
        } catch (MessagingException e) {
            LOGGER.error("Exception while sending: {}", e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Exception while sending: {}", e);
        }

        return success;
    }

    /**
     * <p>
     * Send a new plain text mail to multiple recipients.
     * </p>
     * 
     * @param sender The sender, not <code>null</code> or empty.
     * @param recipients The list of recipients, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param text The text content of the mail, not <code>null</code>.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String sender, List<String> recipients, String subject, String text) {
        Map<RecipientType, List<String>> recipientMap = new HashMap<>();
        recipientMap.put(RecipientType.TO, recipients);
        return sendMail(sender, sender, recipientMap, subject, text, false);
    }

    /**
     * <p>
     * Send a new mail to multiple recipients.
     * </p>
     * 
     * @param sender The sender, not <code>null</code> or empty.
     * @param recipient The recipient, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param text The text content of the mail, not <code>null</code>.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String sender, String recipient, String subject, String text) {
        Validate.notEmpty(sender, "sender must not be empty");
        Validate.notEmpty(recipient, "recipient must not be empty");
        Validate.notNull(subject, "subject must not be null");
        Validate.notNull(text, "text must not be null");

        return sendMail(sender, Arrays.asList(recipient), subject, text);
    }

}
