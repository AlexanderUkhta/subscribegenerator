package com.a1s.subscribegeneratorapp.smsc;

import com.a1s.subscribegeneratorapp.service.ConcatenationService;
import com.a1s.subscribegeneratorapp.service.RequestQueueService;
import com.a1s.subscribegeneratorapp.service.TransactionReportService;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;

import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static com.a1s.ConfigurationConstantsAndMethods.GOT_EXCEPTION_MAKING_BYTECODED_XML;
import static com.a1s.ConfigurationConstantsAndMethods.GOT_SMPP_INVALID_ARG_EXCEPTION;

@Component
public class CustomSmppSessionHandler extends DefaultSmppSessionHandler {
    private final Logger logger = LoggerFactory.getLogger(CustomSmppSessionHandler.class);

    @Autowired
    private ConcatenationService concatenationService;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    @Autowired
    private RequestQueueService requestQueueService;
    @Autowired
    TransactionReportService transactionReportService;
    @Autowired
    private ObjectFactory<DeliveryReceiptTask> deliveryReceiptTaskObjectFactory;

    private WeakReference<SmppSession> sessionRef;
    private long sessionId;

    private AtomicInteger responseCounter = new AtomicInteger(0);
    private AtomicInteger requestSubmitSmCounter = new AtomicInteger(0);
    private AtomicInteger deliverSmRespCounter = new AtomicInteger(0);

    void setSessionRef(SmppServerSession session) {
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
            String msisdn = ((SubmitSm) pduRequest).getDestAddress().getAddress();

            CustomSmppServer.receivedSubmitSmMessages.put(requestSubmitSmCounter.incrementAndGet(), (SubmitSm) pduRequest);

            logger.info("Making pdu_response as an answer to submit_sm, msisdn = " + msisdn + ".");
            SubmitSmResp resp = (SubmitSmResp) pduRequest.createResponse();
            long id = responseCounter.incrementAndGet();

            if (session != null) {
                resp.setMessageId(String.valueOf(id) + session.getConfiguration().getName() + ":" + sessionId);

            } else {
                logger.error("Current session appears like NULL while .getConfiguration()", new NullPointerException());
            }

            if (((SubmitSm) pduRequest).getRegisteredDelivery() > 0) {
                logger.info("Delivery_receipt needed, creating delivery_receipt after 1 second passed, msisdn = " + msisdn + ".");
                DeliveryReceiptTask task = deliveryReceiptTaskObjectFactory.getObject();
                task.setParameters(session, (SubmitSm) pduRequest, resp.getMessageId(), "000");
                threadPoolTaskScheduler.schedule(task, new Date(System.currentTimeMillis() + 1000));

            }

            if (((SubmitSm) pduRequest).getDataCoding() == (byte) 0xF6) {
                logger.info("Got SIM-specific message, IGNORING...");
//                try {
//                    DeliverSm deliverSm = new DeliverSm();
//                    deliverSm.setDestAddress(((SubmitSm) pduRequest).getSourceAddress());
//                    deliverSm.setShortMessage(CharsetUtil.encode("BYTECODE HERE", CharsetUtil.CHARSET_UTF_8)); //todo: enter ENCODED XML with 'YES'
//                    deliverSm.setDataCoding(SmppConstants.DATA_CODING_LATIN1); //todo: is this a correct DCS here?
//                } catch (SmppInvalidArgumentException e) {
//                    logger.error("Smth wrong while setting deliver_sm with bytecoded 'YES'", e);
//                    transactionReportService.processOneFailureReport(requestQueueService.getTransactionIdByMsisdn(msisdn),
//                            GOT_EXCEPTION_MAKING_BYTECODED_XML);
//                }

            } else if (((SubmitSm) pduRequest).getEsmClass() == SmppConstants.ESM_CLASS_UDHI_MASK) {   /* if got an UDH multipart submit_sm */
                logger.info("Got UDH message part for msisdn = " + msisdn + ", processing..." );
                concatenationService.processUdhConcatPart((SubmitSm) pduRequest);


            } else if (pduRequest.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS) != null) {    /* if got a SAR multipart submit_sm */
                logger.info("Got SAR message part for msisdn = " + msisdn + ", processing...");
                concatenationService.processSarConcatPart((SubmitSm) pduRequest);


            } else if (pduRequest.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD) != null) {   /* if got a PAYLOAD submit_sm */
                logger.info("Got Payload message for msisdn = " + msisdn + ", processing...");
                concatenationService.processPayloadConcatMessage((SubmitSm) pduRequest);

            } else {
                logger.info("Got simple message with no concatenation needed for msisdn = " + msisdn + ", processing...");
                concatenationService.processSimpleMessage((SubmitSm) pduRequest);

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
