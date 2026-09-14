package com.loanii.dailytaskmanager;

/**
 * Set BACKEND_URL to your deployed Cloudflare Worker URL.
 * Example: https://daily-task-ai.your-subdomain.workers.dev/api/chat
 * Never put your OpenAI API key in the Android app.
 */
public final class AiConfig {
    private AiConfig() {}
    public static final String BACKEND_URL = "https://daily-task-manager-ai.loaniigroup.workers.dev/api/chat";
}
