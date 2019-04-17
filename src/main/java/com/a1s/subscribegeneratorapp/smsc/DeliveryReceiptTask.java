package com.a1s.subscribegeneratorapp.smsc;

import com.a1s.subscribegeneratorapp.service.RequestQueueService;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
class DeliveryReceiptTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryReceiptTask.class);

    private SmppSession session;
    private SubmitSm request;
    private String messageId;
    private String errorCode;

    void setParameters(SmppSession session, SubmitSm request, String messageId, String errorCode) {
        this.session = session;
        this.request = request;
        this.messageId = messageId;
        this.errorCode = errorCode;
    }

    @Override
    public void run() {
        try {
            DeliverSm dsm = new DeliverSm();

            DeliveryReceipt receipt = new DeliveryReceipt();

            receipt.setMessageId(messageId);

            dsm.setSourceAddress(request.getDestAddress());
            dsm.setDestAddress(request.getSourceAddress());
            receipt.setSubmitDate(DateTime.now().minusSeconds(10));
            receipt.setDoneDate(DateTime.now());

            dsm.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);

            byte receiptState = (byte)((DateTime.now().getSecondOfMinute() % 7) + 1);
            receipt.setErrorCode(0);

            if (Integer.parseInt(errorCode) != 0) {
                receiptState = SmppConstants.STATE_UNDELIVERABLE;
                receipt.setErrorCode(100);
            }

            receipt.setState(receiptState);
            receipt.setSubmitCount(session.getCounters().getRxSubmitSM().getRequest());
            receipt.setDeliveredCount(session.getCounters().getTxDeliverSM().getRequest());

            String str = receipt.toShortMessage();

            byte[] msgBuffer = CharsetUtil.encode(str, CharsetUtil.CHARSET_GSM8);
            dsm.setShortMessage(msgBuffer);
            logger.info("Sending delivery_receipt...");
            session.sendRequestPdu(dsm, TimeUnit.SECONDS.toMillis(60), false);

        } catch (Exception ex) {
            logger.error("{}", ex);
        }

    }
}

