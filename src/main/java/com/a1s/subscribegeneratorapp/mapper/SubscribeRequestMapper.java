package com.a1s.subscribegeneratorapp.mapper;

import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;


import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
@Component("requestMapper")
public class SubscribeRequestMapper implements RowMapper<SubscribeRequestData> {

    public static final String BASE_SQL =
            "select * from short_message_templates";

    @Override
    public SubscribeRequestData mapRow(ResultSet rs, int rowNum) throws SQLException {
        int id = rs.getInt("id");
        int psid = rs.getInt("psid");
        String shortNum = rs.getString("short_number");
        String requestText = rs.getString("request");
        String responseText = rs.getString("response");
        String psIdName = rs.getString("psIdName");

        return new SubscribeRequestData(id, psid, psIdName, shortNum, requestText, responseText);
    }
}
