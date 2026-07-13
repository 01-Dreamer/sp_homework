package com.zxylearn.demo3.sentinel.config;

import java.util.List;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    public static final String CIRCUIT_A_RESOURCE = "sentinelCircuitA";
    public static final String CIRCUIT_B_RESOURCE = "sentinelCircuitB";
    public static final String BULKHEAD_RESOURCE = "sentinelBulkheadInventory";
    public static final String RATE_LIMIT_RESOURCE = "sentinelRateLimitProduct";
    public static final String HOTSPOT_RESOURCE = "sentinelHotspotProduct";

    @PostConstruct
    public void loadRules() {
        DegradeRule circuitA = new DegradeRule(CIRCUIT_A_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.30)
                .setTimeWindow(5)
                .setMinRequestAmount(5)
                .setStatIntervalMs(10_000);

        DegradeRule circuitB = new DegradeRule(CIRCUIT_B_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(2_000)
                .setSlowRatioThreshold(0.30)
                .setTimeWindow(5)
                .setMinRequestAmount(5)
                .setStatIntervalMs(10_000);

        DegradeRuleManager.loadRules(List.of(circuitA, circuitB));

        FlowRule bulkheadRule = new FlowRule(BULKHEAD_RESOURCE)
                .setGrade(RuleConstant.FLOW_GRADE_THREAD)
                .setCount(10);

        FlowRule rateLimitRule = new FlowRule(RATE_LIMIT_RESOURCE)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(2.5);

        FlowRuleManager.loadRules(List.of(bulkheadRule, rateLimitRule));

        ParamFlowItem product3001Item = new ParamFlowItem()
                .setObject("3001")
                .setClassType(String.class.getName())
                .setCount(1);

        ParamFlowRule hotspotRule = new ParamFlowRule(HOTSPOT_RESOURCE)
                .setParamIdx(0)
                .setCount(2)
                .setParamFlowItemList(List.of(product3001Item));

        ParamFlowRuleManager.loadRules(List.of(hotspotRule));
    }
}
