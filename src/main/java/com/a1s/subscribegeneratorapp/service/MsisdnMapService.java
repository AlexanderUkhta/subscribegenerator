package com.a1s.subscribegeneratorapp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MsisdnMapService {
    private static MsisdnMapService msisdnMapService = new MsisdnMapService();
    private static Map<String, String> msisdnMap = new ConcurrentHashMap<>();


}
