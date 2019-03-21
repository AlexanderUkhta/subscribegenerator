package com.a1s.subscribegeneratorapp.smsc;

import com.a1s.subscribegeneratorapp.service.ConcatenationService;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CustomSmppSessionHandler extends DefaultSmppSessionHandler {
    private final Logger logger = LoggerFactory.getLogger(CustomSmppSessionHandler.class);

    @Autowired
    private ConcatenationService concatenationService;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private WeakReference<SmppSession> sessionRef;
    private long sessionId;

    private AtomicInteger responseCounter = new AtomicInteger(0);
    private AtomicInteger requestSubmitSmCounter = new AtomicInteger(0);
    private AtomicInteger deliverSmRespCounter = new AtomicInteger(0);

    CustomSmppSessionHandler(SmppServerSession session) {
        this.sessionRef = new WeakReference<>(session);
    }

    void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        try {
            if (pduAsyncResponse.getResponse() instanceof DeliverSmResp) {
                CustomSmppServer.receivedDeliverSmResps.put(deliverSmRespCounter.incrementAndGet(),
                        (DeliverSmResp) pduAsyncResponse.getResponse());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pduAsyncResponse.getResponse().getCommandStatus() != SmppConstants.STATUS_OK) {
            logger.info("pdu request sent with a problem, got error in response {}",
                    pduAsyncResponse.getResponse().getClass().getCanonicalName());

        } else {
            logger.info("pdu request sent succesfully, got response {}",
                    pduAsyncResponse.getResponse().getClass().getCanonicalName());
        }

    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        SmppSession session = sessionRef.get();

        if (pduRequest instanceof SubmitSm) {

            if (((SubmitSm) pduRequest).getEsmClass() == SmppConstants.ESM_CLASS_UDHI_MASK) {   /* if got an UDH multipart submit_sm */
                concatenationService.processUdhConcatPart((SubmitSm) pduRequest);
                logger.info("Got UDH message part, processing..." );

            } else if (pduRequest.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS) != null) {    /* if got a SAR multipart submit_sm */
                concatenationService.processSarConcatPart((SubmitSm) pduRequest);
                logger.info("Got SAR message part, processing...");

            } else if (pduRequest.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD) != null) {   /* if got a PAYLOAD submit_sm */
                concatenationService.processPayloadConcatMessage((SubmitSm) pduRequest);
                logger.info("Got Payload message, processing...");

            } else {
                concatenationService.processSimpleMessage((SubmitSm) pduRequest);
                logger.info("Got simple message with linkId, processing...");

            }

            CustomSmppServer.receivedSubmitSmMessages.put(requestSubmitSmCounter.incrementAndGet(), (SubmitSm) pduRequest);

            logger.info("Making pdu_response as an answer to submit_sm...");
            SubmitSmResp resp = (SubmitSmResp) pduRequest.createResponse();
            long id = responseCounter.incrementAndGet();

            if (session != null) {
                resp.setMessageId(String.valueOf(id) + session.getConfiguration().getName() + ":" + sessionId);

            } else {
                logger.error("Current session appears like NULL while .getConfiguration", new NullPointerException());

            }

            if (((SubmitSm) pduRequest).getRegisteredDelivery() > 0) {
                logger.info("Delivery_receipt needed, creating delivery_receipt after 1 second passed...");
                threadPoolTaskScheduler.scheduleWithFixedDelay(new DeliveryReceiptTask(
                        session, (SubmitSm) pduRequest, resp.getMessageId(), "000"), 1000);

            }

            return resp;

        } else if (pduRequest instanceof Unbind) {
            if (session != null) {
                session.destroy();
            } else {
                logger.error("Cannot destroy an empty session", new NullPointerException());
            }

            if (session != null) {
                session.unbind(1000);
            } else {
                logger.error("Cannot unbind an empty session", new NullPointerException());
            }

            return pduRequest.createResponse();
        }

        return pduRequest.createResponse();
    }

}
