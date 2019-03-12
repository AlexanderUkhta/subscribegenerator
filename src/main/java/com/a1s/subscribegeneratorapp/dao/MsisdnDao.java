package com.a1s.subscribegeneratorapp.dao;

import com.a1s.subscribegeneratorapp.mapper.MsisdnMapper;
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

import static com.a1s.ConfigurationConstants.MSISDN_NOT_BUSY;

@Repository
public class MsisdnDao extends JdbcDaoSupport {

    private static final Log logger = LogFactory.getLog(MsisdnDao.class);

    @Autowired
    private MsisdnMapper msisdnMapper;

    @Autowired
    public MsisdnDao(DataSource msisdnDataSource) {
        this.setDataSource(msisdnDataSource);
    }

    @NotNull
    public Map<String, Integer> findAll() {
        String sql = MsisdnMapper.MSISDN_SQL;
        Map<String, Integer> msisdnMap = new ConcurrentHashMap<>();

        Object[] params = new Object[] {};

        List<String> list = new LinkedList<>();
        try {
            list = this.getJdbcTemplate().query(sql, params, msisdnMapper);

        } catch(NullPointerException e) {
            logger.error("Empty result for query from database", e);
        }

        for (String one : list) {
            msisdnMap.put(one, MSISDN_NOT_BUSY);
        }
        return msisdnMap;
    }
}
