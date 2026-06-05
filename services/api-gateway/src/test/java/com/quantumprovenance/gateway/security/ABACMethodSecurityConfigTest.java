package com.quantumprovenance.gateway.security;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ABACMethodSecurityConfigTest {

    @Test
    public void testPolicyEvaluationSuccess() {
        ABACMethodSecurityConfig evaluator = new ABACMethodSecurityConfig();

        // Define policy conditions: organization == Customs && clearanceLevel >= 3
        ABACMethodSecurityConfig.PolicyCondition cond1 = new ABACMethodSecurityConfig.PolicyCondition("organization", "EQUALS", "Customs");
        ABACMethodSecurityConfig.PolicyCondition cond2 = new ABACMethodSecurityConfig.PolicyCondition("clearanceLevel", "GREATER_THAN_OR_EQUAL", "3");

        ABACMethodSecurityConfig.AbacPolicy policy = new ABACMethodSecurityConfig.AbacPolicy(
                "Customs Authority rule", "shipment", "VIEW_MANIFEST", List.of(cond1, cond2)
        );

        // Case 1: Valid customs officer (clearance 3)
        Map<String, Object> user1 = new HashMap<>();
        user1.put("organization", "Customs");
        user1.put("clearanceLevel", "3");

        assertTrue(evaluator.evaluatePolicy(policy, user1));

        // Case 2: Customs officer with lower clearance (clearance 1)
        Map<String, Object> user2 = new HashMap<>();
        user2.put("organization", "Customs");
        user2.put("clearanceLevel", "1");

        assertFalse(evaluator.evaluatePolicy(policy, user2));

        // Case 3: Supplier with clearance 3 (wrong organization)
        Map<String, Object> user3 = new HashMap<>();
        user3.put("organization", "LogiCorp");
        user3.put("clearanceLevel", "3");

        assertFalse(evaluator.evaluatePolicy(policy, user3));
    }
}
