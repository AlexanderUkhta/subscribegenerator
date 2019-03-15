package com.a1s.subscribegeneratorapp.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component("msisdnMapper")
public class MsisdnMapper implements RowMapper<String> {

    public static final String MSISDN_SQL =
            "select * from msisdn";

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        String msisdn = rs.getString("msisdn");

        return msisdn;
    }
}
