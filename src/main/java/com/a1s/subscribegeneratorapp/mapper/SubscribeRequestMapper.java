package com.a1s.subscribegeneratorapp.mapper;

import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.springframework.jdbc.core.RowMapper;


import java.sql.ResultSet;
import java.sql.SQLException;

public class SubscribeRequestMapper implements RowMapper<SubscribeRequest> {

    public static final String BASE_SQL =
            "select * from short_message_templates";

    @Override
    public SubscribeRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
        int id = rs.getInt("id");
        String shortNum = rs.getString("short_number");
        String requestText = rs.getString("request");
        String responseText = rs.getString("response");

        return new SubscribeRequest(id, shortNum, requestText, responseText);
    }
}
