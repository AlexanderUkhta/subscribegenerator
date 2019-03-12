package com.a1s.smsc;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.tlv.Tlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.commons.gsm.GsmUtil;

public class CustomSmppSessionHandler extends DefaultSmppSessionHandler {
    private final Logger logger;

    private WeakReference<SmppSession> sessionRef;
    private ScheduledExecutorService pool;
    private long sessionId;

    private AtomicInteger responseCounter = new AtomicInteger(0);
    public static AtomicInteger requestSubmitSmCounter = new AtomicInteger(0);
    public static AtomicInteger deliverSmRespCounter = new AtomicInteger(0);
    public static AtomicInteger multipartSubmitSmQuantity = new AtomicInteger(0);
    private static AtomicInteger tempMultipartSubmitSmParts = new AtomicInteger(0);
    private static ConcurrentHashMap<Integer, byte[]> tempMultipartSubmitSmPartsMap = new ConcurrentHashMap<>();

    public CustomSmppSessionHandler(SmppServerSession session, ScheduledExecutorService pool) {
        this(LoggerFactory.getLogger(CustomSmppSessionHandler.class));
        this.sessionRef = new WeakReference<>(session);
        this.pool = pool;
    }

    public CustomSmppSessionHandler(Logger logger) {
        this.logger = logger;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        try {
            if (pduAsyncResponse.getResponse() instanceof DeliverSmResp) {
                CustomSmppServer.receivedDeliverSmResps.put(deliverSmRespCounter.incrementAndGet(), (DeliverSmResp) pduAsyncResponse.getResponse());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pduAsyncResponse.getResponse().getCommandStatus() != SmppConstants.STATUS_OK) {
            logger.info("pdu request sent with a problem, got error in response {}", pduAsyncResponse.getResponse().getClass().getCanonicalName());
        } else {
            logger.info("pdu request sent succesfully, got response {}", pduAsyncResponse.getResponse().getClass().getCanonicalName());
        }
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        SmppSession session = sessionRef.get();

        if (pduRequest instanceof SubmitSm) {

            /* if got an UDH multipart submit_sm */
            /* get UDH and add a part to the full message storage (MultipartSubmitSmMap) */
            if (((SubmitSm) pduRequest).getEsmClass() == SmppConstants.ESM_CLASS_UDHI_MASK) {
                SubmitSm sm = (SubmitSm) pduRequest;
                byte[] userDataHeader = GsmUtil.getShortMessageUserDataHeader(sm.getShortMessage());
                int totalMessages = userDataHeader[4] & 0xff; // Range 0 to 255, not -128 to 127;
                int currentMessageNum = userDataHeader[5] & 0xff; // Range 0 to 255, not -128 to 127;
                logger.info("got UDH multipart sumbit_sm, part {} out of total {} parts in total", currentMessageNum, totalMessages);

                /* Puts new currentMessageNum with currentSM without UDH */
                tempMultipartSubmitSmPartsMap.put(currentMessageNum, GsmUtil.getShortMessageUserData(sm.getShortMessage()));

                /* When we get all parts of multipart SM */
                if (totalMessages == tempMultipartSubmitSmParts.incrementAndGet()) {
                    concatenateMultipartSubmitSm(tempMultipartSubmitSmPartsMap);
                    tempMultipartSubmitSmPartsMap.clear();
                    tempMultipartSubmitSmParts.set(0);
                }
            }

            /* if got a SAR multipart submit_sm */
            /* analise tlv-parameters and add a part to the full message storage (MultipartSubmitSmMap) */
            if (pduRequest.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS) != null) {
                int sarTotalSegments = 0;
                for (int i : pduRequest.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValue()) {
                    sarTotalSegments = sarTotalSegments * 10 + i;
                }

                int sarSegmentSeqnum = 0;
                for (int i : pduRequest.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValue()) {
                    sarSegmentSeqnum = sarSegmentSeqnum * 10 + i;
                }

                logger.info("got SAR multipart sumbit_sm, part {} out of total {} parts in total", sarSegmentSeqnum, sarTotalSegments);

                /* Puts new sarSegmentSeqNum with currentSM in a map */
                tempMultipartSubmitSmPartsMap.put(sarSegmentSeqnum, ((SubmitSm) pduRequest).getShortMessage());

                /* When we get all parts of multipart SM */
                if (sarTotalSegments == tempMultipartSubmitSmParts.incrementAndGet()) {
                    concatenateMultipartSubmitSm(tempMultipartSubmitSmPartsMap);
                    tempMultipartSubmitSmPartsMap.clear();
                    tempMultipartSubmitSmParts.set(0);
                }
            }

            /* if got a PAYLOAD submit_sm */
            /* analise tlv-parameters and add a tlv-message-payload to the receivedMultipartSubmitSmMessages map */
            if (pduRequest.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD) != null) {
                Tlv messagePayload = pduRequest.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
                byte[] shortMessage = messagePayload.getValue();
                CustomSmppServer.receivedMultipartSubmitSmMessages.put(multipartSubmitSmQuantity.incrementAndGet(), shortMessage);
                logger.info("got PAYLOAD multipart sumbit_sm, saving the tlv-payload-message");
            }

            CustomSmppServer.receivedSubmitSmMessages.put(requestSubmitSmCounter.incrementAndGet(), (SubmitSm) pduRequest);

            SubmitSmResp resp = (SubmitSmResp) pduRequest.createResponse();
            long id = responseCounter.incrementAndGet();

            resp.setMessageId(String.valueOf(id) + session.getConfiguration().getName() + ":" + sessionId);
            if (((SubmitSm) pduRequest).getRegisteredDelivery() > 0) {
                pool.schedule(new DeliveryReceiptTask(session, (SubmitSm) pduRequest, resp.getMessageId(), "000"), 1, TimeUnit.SECONDS);
            }

            return resp;

        } else if (pduRequest instanceof Unbind) {
            session.destroy();

            session.unbind(1000);
            return pduRequest.createResponse();
        }
        return pduRequest.createResponse();
    }

    /* combines parts of UDH/SAR-concatenated request */
    private void concatenateMultipartSubmitSm(ConcurrentHashMap<Integer, byte[]> map) {
        /* We are creating a treeMap in order to sort ConcurrentHashMap keys */
        Map<Integer, byte[]> treeMap = new TreeMap<>(map);
        ByteArrayOutputStream fullMultipartMessage = new ByteArrayOutputStream();

        for(Map.Entry<Integer, byte[]> entry : treeMap.entrySet()) {
            try {
                fullMultipartMessage.write(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] finalConcatenatedMessage = fullMultipartMessage.toByteArray();
        /* puts a full multipart message to the Map storage */
        CustomSmppServer.receivedMultipartSubmitSmMessages.put(multipartSubmitSmQuantity.incrementAndGet(), finalConcatenatedMessage);
    }

    public static byte[][] createNoHeaderConcatenatedBinaryShortMessages(byte[] binaryShortMessage) {
        if (binaryShortMessage == null) {
            return null;
        }
        // if the short message does not need to be concatenated
        if (binaryShortMessage.length <= 140) {
            return null;
        }

        // since the UDH will be 6 bytes, we'll split the data into chunks of 134
        int numParts = (int) (binaryShortMessage.length / 134) + (binaryShortMessage.length % 134 != 0 ? 1 : 0);

        byte[][] shortMessageParts = new byte[numParts][];

        for (int i = 0; i < numParts; i++) {
            // default this part length to max of 134
            int shortMessagePartLength = 134;
            if ((i + 1) == numParts) {
                // last part (only need to add remainder)
                shortMessagePartLength = binaryShortMessage.length - (i * 134);
            }

            // part will be UDH (6 bytes) + length of part
            byte[] shortMessagePart = new byte[shortMessagePartLength];

            // copy this part's user data onto the end
            System.arraycopy(binaryShortMessage, (i * 134), shortMessagePart, 0, shortMessagePartLength);
            shortMessageParts[i] = shortMessagePart;
        }

        return shortMessageParts;
    }

}
