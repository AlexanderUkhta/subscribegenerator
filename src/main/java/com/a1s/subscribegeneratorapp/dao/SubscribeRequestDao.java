package com.a1s.subscribegeneratorapp.dao;

import com.a1s.subscribegeneratorapp.mapper.SubscribeRequestMapper;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@Repository
public class SubscribeRequestDao extends JdbcDaoSupport {
    private static final Log logger = LogFactory.getLog(SubscribeRequestDao.class);

    @Autowired
    private SubscribeRequestMapper requestMapper;

    @Autowired
    public SubscribeRequestDao(DataSource dataSource) {
        this.setDataSource(dataSource);
    }

    @NotNull
    public Map<Integer, SubscribeRequest> findAll() {
        String sql = SubscribeRequestMapper.BASE_SQL;
        Map<Integer, SubscribeRequest> requestDataMap = new ConcurrentHashMap<>();

        Object[] params = new Object[] {};

        List<SubscribeRequest> list = new LinkedList<>();
        try {
            list = this.getJdbcTemplate().query(sql, params, requestMapper);

        } catch(NullPointerException e) {
            logger.error("Empty result for query from database", e);
        }

        for (SubscribeRequest one : list) {
            requestDataMap.put(one.getId(), one);
        }
        return requestDataMap;
    }
}
