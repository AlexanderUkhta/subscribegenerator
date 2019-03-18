package com.a1s.subscribegeneratorapp.smsc;

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.SmppChannelException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSmppServer extends DefaultSmppServer{
    private static final Logger logger = LoggerFactory.getLogger(CustomSmppServer.class);

    private static ConcurrentHashMap<String, SmppServerSession> serverSessionsActive = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, CustomSmppServerHandler> serverHandlers = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, SubmitSm> receivedSubmitSmMessages = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, DeliverSmResp> receivedDeliverSmResps = new ConcurrentHashMap<>();
    public static Queue<SubmitSm> requestsDeniedByError = new ConcurrentLinkedQueue<>();

    private static CountDownLatch bindCompleted;
    private CustomSmppServerHandler serverHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private SmppServerConfiguration smppServerConfiguration;

    private static ScheduledExecutorService asyncPool = Executors.newScheduledThreadPool(15);
    private static ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
    private AtomicInteger sequence = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
            return t;
        }
    });

    public CustomSmppServer(SmppServerConfiguration configuration, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        super(configuration, getNewServerHandler(configuration.getPort()), monitorExecutor, bossGroup, workerGroup);
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.smppServerConfiguration = configuration;
        this.serverHandler = serverHandlers.get(configuration.getPort());
    }


    public void startServerMain(CountDownLatch bindCompleted) {
        CustomSmppServer.bindCompleted = bindCompleted;
        logger.info("Starting SMPP server...");
        try {
            this.start();
        } catch (SmppChannelException e) {
            e.printStackTrace();
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

    private static CustomSmppServerHandler getNewServerHandler(Integer port) {
        CustomSmppServerHandler smppServerHandler = new CustomSmppServerHandler(asyncPool);
        serverHandlers.put(port, smppServerHandler);
        return smppServerHandler;
    }

    public static class CustomSmppServerHandler implements SmppServerHandler {

        ScheduledExecutorService pool;

        ConcurrentHashMap<Long, SmppSessionHandler> sessionHandlers = new ConcurrentHashMap<>();

        CustomSmppServerHandler(ScheduledExecutorService pool) {
            this.pool = pool;
        }

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

            CustomSmppSessionHandler sessionHandler = new CustomSmppSessionHandler(session, pool);
            session.serverReady(sessionHandler);
            sessionHandler.setSessionId(sessionId);
            sessionHandlers.put(sessionId, sessionHandler);

            while (!session.isBound());
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
