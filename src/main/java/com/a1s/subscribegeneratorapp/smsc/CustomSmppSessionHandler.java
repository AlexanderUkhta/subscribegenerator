package com.a1s.subscribegeneratorapp.smsc;
import com.a1s.subscribegeneratorapp.config.ApplicationContextHolder;
import com.a1s.subscribegeneratorapp.service.ConcatenationService;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomSmppSessionHandler extends DefaultSmppSessionHandler {
    private final Logger logger = LoggerFactory.getLogger(CustomSmppSessionHandler.class);

    private ConcatenationService concatenationService =
            (ConcatenationService) ApplicationContextHolder.getApplicationContext().getBean("concatenationService");

    private WeakReference<SmppSession> sessionRef;
    private ScheduledExecutorService pool;

    private long sessionId;

    private AtomicInteger responseCounter = new AtomicInteger(0);
    private static AtomicInteger requestSubmitSmCounter = new AtomicInteger(0);
    private static AtomicInteger deliverSmRespCounter = new AtomicInteger(0);

    CustomSmppSessionHandler(SmppServerSession session, ScheduledExecutorService pool) {
        this.sessionRef = new WeakReference<>(session);
        this.pool = pool;
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

            SubmitSmResp resp = (SubmitSmResp) pduRequest.createResponse();
            long id = responseCounter.incrementAndGet();
            try {
                resp.setMessageId(String.valueOf(id) + session.getConfiguration().getName() + ":" + sessionId);
            } catch (NullPointerException e) {
                logger.error("Current session appears like NULL while .getConfiguration", e);
            }

            if (((SubmitSm) pduRequest).getRegisteredDelivery() > 0) {
                pool.schedule(new DeliveryReceiptTask(
                        session, (SubmitSm) pduRequest, resp.getMessageId(), "000"), 1, TimeUnit.SECONDS);
            }

            return resp;

        } else if (pduRequest instanceof Unbind) {
            session.destroy();

            session.unbind(1000);
            return pduRequest.createResponse();
        }
        return pduRequest.createResponse();
    }



}
