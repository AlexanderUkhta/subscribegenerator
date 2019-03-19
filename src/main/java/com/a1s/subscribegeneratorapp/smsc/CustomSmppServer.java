package com.a1s.subscribegeneratorapp.smsc;

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.SmppChannelException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a1s.ConfigurationConstantsAndMethods.ultimateWhile;

@Component
public class CustomSmppServer extends DefaultSmppServer {
    private static final Logger logger = LoggerFactory.getLogger(CustomSmppServer.class);

    private static ConcurrentHashMap<String, SmppServerSession> serverSessionsActive = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, SubmitSm> receivedSubmitSmMessages = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, DeliverSmResp> receivedDeliverSmResps = new ConcurrentHashMap<>();

    private static CountDownLatch bindCompleted;

    @Autowired
    private static CustomSmppSessionHandler customSmppSessionHandler;

    private static ScheduledThreadPoolExecutor monitorExecutor =
        (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {
        private AtomicInteger sequence = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
    });

    public CustomSmppServer(SmppServerConfiguration configuration, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        super(configuration, new CustomSmppServerHandler(), monitorExecutor, bossGroup, workerGroup);
    }


    public void startServerMain(CountDownLatch bindCompleted) {
        CustomSmppServer.bindCompleted = bindCompleted;
        logger.info("Starting SMPP server...");
        try {
            this.start();
        } catch (SmppChannelException e) {
            logger.error("Got SmppChannelException", e);
        }
        logger.info("SMPP server started");
    }

    public static SmppServerConfiguration getBaseServerConfiguration(Integer port, String systemID) {
        SmppServerConfiguration configuration = new SmppServerConfiguration();

        configuration.setPort(port);
        configuration.setMaxConnectionSize(10);
        configuration.setNonBlockingSocketsEnabled(true);
        configuration.setDefaultRequestExpiryTimeout(10000);
        configuration.setDefaultWindowMonitorInterval(5000);
        configuration.setDefaultWindowSize(100);
        configuration.setDefaultWindowWaitTimeout(configuration.getDefaultRequestExpiryTimeout());
        configuration.setDefaultSessionCountersEnabled(true);
        configuration.setJmxEnabled(true);
        configuration.setSystemId(systemID);
        configuration.setName("Server" + port);
        return configuration;
    }

    public static SmppServerSession getServerSession(String key) {
        return serverSessionsActive.get(key);
    }


    public static class CustomSmppServerHandler implements SmppServerHandler {
        ConcurrentHashMap<Long, SmppSessionHandler> sessionHandlers = new ConcurrentHashMap<>();

        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) {
            // test name change of sessions
            // this name actually shows up as thread context...
            sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());
            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) {
            logger.info("Session created: {}", session);

            // static session_handler is used, if more than 1 session on a server - it needs being refactored
            session.serverReady(customSmppSessionHandler);
            customSmppSessionHandler.setSessionId(sessionId);
            sessionHandlers.put(sessionId, customSmppSessionHandler);

            try {
                ultimateWhile(() -> (!session.isBound()), 30);
            } catch (TimeoutException e) {
                logger.error("Session did not start at 30 seconds");
            }
            serverSessionsActive.put(session.getConfiguration().getSystemId(), session);
            CustomSmppServer.bindCompleted.countDown();
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            sessionHandlers.get(sessionId).fireChannelUnexpectedlyClosed();
            logger.info("Session destroyed: {}", session);
            // print out final stats
            if (session.hasCounters()) {
                logger.info(" final session rx-submitSM: {}", session.getCounters().getRxSubmitSM());
            }

            sessionHandlers.remove(sessionId);
            // make sure it's really shutdown
            session.destroy();
        }
    }

}
