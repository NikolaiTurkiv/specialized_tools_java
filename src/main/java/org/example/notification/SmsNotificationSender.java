package org.example.notification;

import java.nio.charset.StandardCharsets;
import org.example.config.PropertiesLoader;
import org.example.exception.ApiException;
import org.example.model.DeliveryChannel;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//проверял через докер, оригинальный эмулятор скачать не удалось
public class SmsNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationSender() {
        PropertiesLoader loader = new PropertiesLoader("sms.properties");
        this.host = loader.get("smpp.host", "localhost");
        this.port = loader.getInt("smpp.port", 2775);
        this.systemId = loader.get("smpp.system_id", "");
        this.password = loader.get("smpp.password", "");
        this.systemType = loader.get("smpp.system_type", "OTP");
        this.sourceAddress = loader.get("smpp.source_addr", "OTPService");
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public void send(String username, String destination, String operationId, String code) {
        ensureConfigured();

        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );

            session.connectAndBind(host, port, bindParameter);
            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    buildMessage(username, operationId, code).getBytes(StandardCharsets.UTF_8)
            );

            log.info("OTP SMS sent to {} for operation {}", destination, operationId);
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {} for operation {}", destination, operationId, e);
            throw new ApiException(502, "Failed to send OTP SMS: " + e.getMessage());
        } finally {
            try {
                session.unbindAndClose();
            } catch (Exception e) {
                log.debug("Failed to close SMPP session cleanly", e);
            }
        }
    }

    private void ensureConfigured() {
        if (systemId.isBlank() || password.isBlank()) {
            throw new ApiException(500, "SMS channel is not configured. Fill src/main/resources/sms.properties");
        }
    }

    private String buildMessage(String username, String operationId, String code) {
        return "Hello, %s! Operation %s code: %s".formatted(username, operationId, code);
    }
}
