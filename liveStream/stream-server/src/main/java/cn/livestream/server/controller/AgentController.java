package cn.livestream.server.controller;

import cn.livestream.server.model.agent.AgentInfo;
import cn.livestream.server.service.AgentRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentRegistry registry;

    public AgentController(AgentRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAgents() {
        List<AgentInfo> agents = registry.getOnlineAgents();
        Map<String, Object> result = new HashMap<>();
        result.put("count", agents.size());
        result.put("agents", agents);
        return ResponseEntity.ok(result);
    }
}
