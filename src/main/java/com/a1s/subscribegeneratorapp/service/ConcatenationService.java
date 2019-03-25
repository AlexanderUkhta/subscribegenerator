package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.SubmitSmData;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.google.common.collect.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service, that is used to process all the submit_sm requests, received by CustomSmppServer.
 * Processes UDH, SAR, PAYLOAD concatenation and also simple one-part submit_sm requests.
 */
@Service("concatenationService")
public class ConcatenationService {
    private static final Log logger = LogFactory.getLog(ConcatenationService.class);

    private Multimap<String, SubmitSmData> messageParts = ArrayListMultimap.create();

    @Autowired
    private TransactionReportService transactionReportService;

    /**
     * Processes submit_sm, if it has the userDataHeader.
     * Processed request is than put in the pool of message parts. If all parts for current msisdn are received,
     * they are then concatenated and are put to the report map. In this case, msisdn becomes NOT_BUSY.
     * @param submitSm received submit_sm request
     */
    public void processUdhConcatPart(final SubmitSm submitSm) {
        String msisdn = submitSm.getDestAddress().getAddress();
        String linkId = submitSm.getSourceAddress().getAddress();

        byte[] userDataHeader = GsmUtil.getShortMessageUserDataHeader(submitSm.getShortMessage());
        int udhReferenceId = userDataHeader[3] & 0xff;
        int udhPartsQuantity = userDataHeader[4] & 0xff;
        int udhCurrentPart = userDataHeader[5] & 0xff;

        String messageFullId = msisdn + linkId + udhReferenceId;

        messageParts.put(messageFullId, new SubmitSmData(udhCurrentPart,
                GsmUtil.getShortMessageUserData(submitSm.getShortMessage())));

        if (udhPartsQuantity == messageParts.get(messageFullId).size()) {
            logger.info("Got all UDH parts for msisdn = " + submitSm.getDestAddress().getAddress() +
                    ", processing UDH parts");
            byte[] finalMessage = concatenateUdhOrSar(messageParts.get(messageFullId));
            messageParts.asMap().remove(messageFullId);

            transactionReportService.processOneInfoReport(finalMessage, msisdn);
        }

    }

    /**
     * Processes submit_sm, if it has the sar optional parameters.
     * Processed request is than put in the pool of message parts. If all parts for current msisdn are received,
     * they are then concatenated and are put to the report map. In this case, msisdn becomes NOT_BUSY.
     * @param submitSm received submit_sm request
     */
    public void processSarConcatPart(final SubmitSm submitSm) {
        String msisdn = submitSm.getDestAddress().getAddress();
        String linkId = submitSm.getSourceAddress().getAddress();

        int sarTotalSegments = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValue()) {
            sarTotalSegments = sarTotalSegments * 10 + i;
        }

        int sarSegmentSeqnum = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValue()) {
            sarSegmentSeqnum = sarSegmentSeqnum * 10 + i;
        }

        int sarMsgRefnum = 0;
        for (int i : submitSm.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM).getValue()) {
            sarMsgRefnum = sarMsgRefnum * 10 + i; //todo: проверить, нужно ли в цикле умножать на 10 ref_num
        }

        String messageFullId = msisdn + linkId + sarMsgRefnum;

        messageParts.put(messageFullId, new SubmitSmData(sarSegmentSeqnum,
                submitSm.getShortMessage()));

        if (sarTotalSegments == messageParts.get(messageFullId).size()) {
            logger.info("Got all SAR parts for msisdn = " + submitSm.getDestAddress().getAddress() +
                    ", processing SAR parts");
            byte[] finalMessage = concatenateUdhOrSar(messageParts.get(messageFullId));
            messageParts.asMap().remove(messageFullId);

            transactionReportService.processOneInfoReport(finalMessage, msisdn);
        }

    }

    /**
     * Processes submit_sm, if it has the payload optional parameters.
     * The data from submit_sm id then put to the report map, msisdn becomes NOT_BUSY.
     * @param submitSm received submit_sm request
     */
    public void processPayloadConcatMessage(final SubmitSm submitSm) {
        logger.info("Got PAYLOAD for msisdn = " + submitSm.getDestAddress().getAddress() +
                ", processing");
        Tlv messagePayload = submitSm.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        byte[] shortMessage = messagePayload.getValue();

        transactionReportService.processOneInfoReport(shortMessage, submitSm.getDestAddress().getAddress());

    }

    /**
     * Processes simple one-part submit_sm.
     * The data from submit_sm id then put to the report map, msisdn becomes NOT_BUSY.
     * @param submitSm received submit_sm request
     */
    public void processSimpleMessage(final SubmitSm submitSm) {
        logger.info("Got simple message for msisdn = " + submitSm.getDestAddress().getAddress() +
                ", processing");
        byte[] shortMessage = submitSm.getShortMessage();

        transactionReportService.processOneInfoReport(shortMessage, submitSm.getDestAddress().getAddress());

    }

    /**
     * Makes one byte message from several message parts, received by UDH or SAR.
     * @param messagePartsCollection message parts, that need to be concatenated
     * @return concatenated byte message
     */
    private byte[] concatenateUdhOrSar(final Collection<SubmitSmData> messagePartsCollection) {

        Map<Integer, byte[]> treeMap = new TreeMap<>();

        for(SubmitSmData oneData : messagePartsCollection) {
            treeMap.put(oneData.getPartId(), oneData.getShortMessage());
        }

        ByteArrayOutputStream fullMultipartMessage = new ByteArrayOutputStream();

        for(Map.Entry<Integer, byte[]> entry : treeMap.entrySet()) {
            try {
                fullMultipartMessage.write(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
       return fullMultipartMessage.toByteArray();

    }

}
