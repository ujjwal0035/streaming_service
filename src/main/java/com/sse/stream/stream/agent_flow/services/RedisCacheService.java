package com.sse.stream.stream.agent_flow.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final long DEFAULT_TTL = 20; // 1 hour in seconds
    private static final long AGENT_SESSION_TTL = 20; // 24 hours for agent sessions
    private static final String AGENT_SESSION_PREFIX = "agent:session:";
    private static final String AGENT_POD_PREFIX = "agent:pod:";
    private static final String ACTIVE_AGENTS_KEY = "agents:active";

    /**
     * Set value in Redis with default TTL (1 hour)
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL, TimeUnit.SECONDS);
            log.info("Value set in Redis - Key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set value in Redis - Key: {}", key, e);
        }
    }

    /**
     * Set value in Redis with custom TTL
     */
    public void set(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.info("Value set in Redis - Key: {}, TTL: {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to set value in Redis - Key: {}", key, e);
        }
    }

    /**
     * Get value from Redis
     */
    public String get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.info("Value retrieved from Redis - Key: {}", key);
            } else {
                log.info("Key not found in Redis - Key: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to get value from Redis - Key: {}", key, e);
            return null;
        }
    }

    /**
     * Delete a key from Redis
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.info("Key deleted from Redis - Key: {}, Success: {}", key, result);
            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to delete key from Redis - Key: {}", key, e);
            return false;
        }
    }

    /**
     * Check if key exists in Redis
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to check key existence in Redis - Key: {}", key, e);
            return false;
        }
    }

    /**
     * Increment a counter in Redis
     */
    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.info("Counter incremented in Redis - Key: {}, Value: {}", key, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Failed to increment counter in Redis - Key: {}", key, e);
            return 0;
        }
    }

    /**
     * Increment a counter by a specific amount in Redis
     */
    public long incrementBy(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.info("Counter incremented in Redis - Key: {}, Delta: {}, Value: {}", key, delta, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Failed to increment counter in Redis - Key: {}", key, e);
            return 0;
        }
    }

    // ==================== AGENT SESSION MANAGEMENT ====================

    /**
     * Register an agent connection (store which pod the agent is connected to)
     */
    public void registerAgentSession(String agentId, String podName) {
        try {
            String sessionKey = AGENT_SESSION_PREFIX + agentId;
            String podKey = AGENT_POD_PREFIX + agentId;
            
            redisTemplate.opsForValue().set(sessionKey, String.valueOf(System.currentTimeMillis()), AGENT_SESSION_TTL, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(podKey, podName, AGENT_SESSION_TTL, TimeUnit.SECONDS);
            redisTemplate.opsForSet().add(ACTIVE_AGENTS_KEY, agentId);
            
            log.info("Agent session registered - AgentId: {}, Pod: {}", agentId, podName);
        } catch (Exception e) {
            log.error("Failed to register agent session - AgentId: {}", agentId, e);
        }
    }

    /**
     * Check if agent is online
     */
    public boolean isAgentOnline(String agentId) {
        try {
            String sessionKey = AGENT_SESSION_PREFIX + agentId;
            Boolean exists = redisTemplate.hasKey(sessionKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check agent online status - AgentId: {}", agentId, e);
            return false;
        }
    }

    /**
     * Get all active agent IDs
     */
    public Set<String> getAllActiveAgents() {
        try {
            Set<String> agents = redisTemplate.opsForSet().members(ACTIVE_AGENTS_KEY);
            log.info("Retrieved {} active agents", agents != null ? agents.size() : 0);
            return agents != null ? agents : Set.of();
        } catch (Exception e) {
            log.error("Failed to get all active agents", e);
            return Set.of();
        }
    }

    public void refreshAgentSessionTTL(String agentId) {
        try {
            String sessionKey = AGENT_SESSION_PREFIX + agentId;
            String podKey = AGENT_POD_PREFIX + agentId;

            // Refresh both TTLs
            redisTemplate.expire(sessionKey, AGENT_SESSION_TTL, TimeUnit.SECONDS);
            redisTemplate.expire(podKey, AGENT_SESSION_TTL, TimeUnit.SECONDS);

            // Safety: ensure agent remains in active set
            redisTemplate.opsForSet().add(ACTIVE_AGENTS_KEY, agentId);

            log.debug("TTL refreshed for agent [{}]", agentId);

        } catch (Exception e) {
            log.error("Failed to refresh TTL for agent [{}]", agentId, e);
        }
    }

    /**
     * Get the pod name where agent is connected
     */
    public String getAgentPod(String agentId) {
        try {
            String podKey = AGENT_POD_PREFIX + agentId;
            return redisTemplate.opsForValue().get(podKey);
        } catch (Exception e) {
            log.error("Failed to get agent pod - AgentId: {}", agentId, e);
            return null;
        }
    }

    /**
     * Remove agent session (agent disconnected)
     */
    public void removeAgentSession(String agentId) {
        try {
            String sessionKey = AGENT_SESSION_PREFIX + agentId;
            String podKey = AGENT_POD_PREFIX + agentId;
            
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(podKey);
            redisTemplate.opsForSet().remove(ACTIVE_AGENTS_KEY, agentId);
            
            log.info("Agent session removed - AgentId: {}", agentId);
        } catch (Exception e) {
            log.error("Failed to remove agent session - AgentId: {}", agentId, e);
        }
    }

    /**
     * Get all active agents
     */
    public Long getActiveAgentCount() {
        try {
            Long count = redisTemplate.opsForSet().size(ACTIVE_AGENTS_KEY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to get active agent count", e);
            return 0L;
        }
    }

    /**
     * Cache borrower data
     */
    public void cacheBorrower(String borrowerId, String borrowerData) {
        String key = "borrower:" + borrowerId;
        set(key, borrowerData, 3600); // 1 hour TTL
        log.info("Borrower cached - ID: {}", borrowerId);
    }

    /**
     * Get cached borrower data
     */
    public String getCachedBorrower(String borrowerId) {
        String key = "borrower:" + borrowerId;
        return get(key);
    }

    /**
     * Remove cached borrower data
     */
    public boolean removeCachedBorrower(String borrowerId) {
        String key = "borrower:" + borrowerId;
        return delete(key);
    }

    /**
     * Cache SSE client session
     */
    public void cacheSSEClient(String clientId, String sessionData) {
        String key = "sse:client:" + clientId;
        set(key, sessionData, 7200); // 2 hour TTL
        log.info("SSE client session cached - ID: {}", clientId);
    }

    /**
     * Get cached SSE client session
     */
    public String getSSEClientSession(String clientId) {
        String key = "sse:client:" + clientId;
        return get(key);
    }

    /**
     * Remove cached SSE client session
     */
    public boolean removeSSEClientSession(String clientId) {
        String key = "sse:client:" + clientId;
        return delete(key);
    }

    /**
     * Increment active connection count
     */
    public long incrementActiveConnections() {
        return increment("metrics:active_connections");
    }

    /**
     * Decrement active connection count
     */
    public long decrementActiveConnections() {
        Long value = redisTemplate.opsForValue().decrement("metrics:active_connections");
        return value != null ? value : 0;
    }

    /**
     * Get active connection count
     */
    public long getActiveConnectionCount() {
        String value = get("metrics:active_connections");
        return value != null ? Long.parseLong(value) : 0;
    }
    
}