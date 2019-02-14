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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Repository
//@Transactional ??
public class SubscribeRequestDao extends JdbcDaoSupport {
    private static final Log logger = LogFactory.getLog(SubscribeRequestDao.class);

    @Autowired
    public SubscribeRequestDao(DataSource dataSource) {
        this.setDataSource(dataSource);
    }

    @NotNull
    public Map<Integer, SubscribeRequest> getAllAsTreeMap() {
        String sql = SubscribeRequestMapper.BASE_SQL;
        Map<Integer, SubscribeRequest> map = new TreeMap<>();

        Object[] params = new Object[] {};
        SubscribeRequestMapper mapper = new SubscribeRequestMapper();

        List<SubscribeRequest> list;
        list = this.getJdbcTemplate().query(sql, params, mapper);

        for (SubscribeRequest one : list) {
            map.put(one.getId(), one);
        }

        return map;
    }
}
