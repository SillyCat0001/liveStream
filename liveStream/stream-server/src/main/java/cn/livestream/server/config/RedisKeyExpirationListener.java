package cn.livestream.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyExpirationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisKeyExpirationListener.class);
    private static final String HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Redis key expired: {}", expiredKey);

        if (expiredKey.startsWith(HEARTBEAT_KEY_PREFIX)) {
            String agentId = expiredKey.substring(HEARTBEAT_KEY_PREFIX.length());
            log.info("Heartbeat expired for agent: {}", agentId);
            // StreamCoordinator.onHeartbeatExpired(agentId) will be called in Task 3
        }
    }
}