package edu.comp331.agent.tools;

import java.util.Map;

public interface AgentTool {
    String name();
    String description();
    String execute(Map<String, String> args);
}