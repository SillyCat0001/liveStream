package cn.livestream.server.service;

import cn.livestream.server.model.agent.AgentInfo;
import cn.livestream.server.model.agent.AgentStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentRegistry {
    private static final String AGENT_INFO_KEY = "agent:%s:info";
    private static final String AGENT_STATUS_KEY = "agent:%s:status";
    private static final String AGENT_HEARTBEAT_KEY = "agent:%s:lastHeartbeat";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, AgentInfo> localCache = new ConcurrentHashMap<>();

    public AgentRegistry(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(AgentInfo info) {
        String agentId = info.getAgentId();
        redisTemplate.opsForHash().put(String.format(AGENT_INFO_KEY, agentId), "agentId", agentId);
        redisTemplate.opsForHash().put(String.format(AGENT_INFO_KEY, agentId), "deviceName", info.getDeviceName());
        redisTemplate.opsForHash().put(String.format(AGENT_INFO_KEY, agentId), "capabilities", info.getCapabilities());
        redisTemplate.opsForValue().set(String.format(AGENT_STATUS_KEY, agentId), AgentStatus.ONLINE.name());
        updateHeartbeat(agentId);
        localCache.put(agentId, info);
    }

    public void updateHeartbeat(String agentId) {
        redisTemplate.opsForValue().set(
            String.format(AGENT_HEARTBEAT_KEY, agentId),
            String.valueOf(System.currentTimeMillis())
        );
    }

    public AgentStatus getStatus(String agentId) {
        Object status = redisTemplate.opsForValue().get(String.format(AGENT_STATUS_KEY, agentId));
        if (status == null) return AgentStatus.OFFLINE;
        return AgentStatus.valueOf(status.toString());
    }

    public void setStatus(String agentId, AgentStatus status) {
        redisTemplate.opsForValue().set(String.format(AGENT_STATUS_KEY, agentId), status.name());
        AgentInfo info = localCache.get(agentId);
        if (info != null) {
            AgentInfo updated = new AgentInfo();
            updated.setAgentId(agentId);
            updated.setDeviceName(info.getDeviceName());
            updated.setCapabilities(info.getCapabilities());
            updated.setStatus(status);
            localCache.put(agentId, updated);
        }
    }

    public AgentInfo getAgentInfo(String agentId) {
        if (localCache.containsKey(agentId)) {
            return localCache.get(agentId);
        }
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(String.format(AGENT_INFO_KEY, agentId));
        if (entries.isEmpty()) return null;
        AgentInfo info = new AgentInfo();
        info.setAgentId(agentId);
        info.setDeviceName(entries.get("deviceName") != null ? entries.get("deviceName").toString() : null);
        Object status = redisTemplate.opsForValue().get(String.format(AGENT_STATUS_KEY, agentId));
        info.setStatus(status != null ? AgentStatus.valueOf(status.toString()) : AgentStatus.OFFLINE);
        localCache.put(agentId, info);
        return info;
    }

    public boolean isAgentOnline(String agentId) {
        return getStatus(agentId) == AgentStatus.ONLINE;
    }
}