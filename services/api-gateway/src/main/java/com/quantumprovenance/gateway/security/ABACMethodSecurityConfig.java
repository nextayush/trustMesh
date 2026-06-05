package com.quantumprovenance.gateway.security;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class ABACMethodSecurityConfig {

    public record PolicyCondition(String attribute, String operator, String value) {}
    public record AbacPolicy(String policyName, String resource, String action, List<PolicyCondition> conditions) {}

    public boolean evaluatePolicy(AbacPolicy policy, Map<String, Object> userAttributes) {
        if (policy == null || policy.conditions() == null) {
            return false;
        }

        for (PolicyCondition condition : policy.conditions()) {
            String attrKey = condition.attribute();
            if (!userAttributes.containsKey(attrKey)) {
                return false;
            }

            String userValue = String.valueOf(userAttributes.get(attrKey));
            String requiredValue = condition.value();

            if ("EQUALS".equalsIgnoreCase(condition.operator())) {
                if (!userValue.equals(requiredValue)) {
                    return false;
                }
            } else if ("GREATER_THAN_OR_EQUAL".equalsIgnoreCase(condition.operator())) {
                try {
                    int userInt = Integer.parseInt(userValue);
                    int requiredInt = Integer.parseInt(requiredValue);
                    if (userInt < requiredInt) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return true;
    }
}
