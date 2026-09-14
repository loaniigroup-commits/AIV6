export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return cors(new Response(null, { status: 204 }));
    }
    if (url.pathname === "/health") {
      return cors(json({ ok: true, service: "daily-task-manager-ai" }));
    }
    if ((url.pathname !== "/api/chat" && url.pathname !== "/") || request.method !== "POST") {
      return cors(json({ error: "Not found" }, 404));
    }
    if (!env.OPENAI_API_KEY) {
      return cors(json({ error: "OPENAI_API_KEY is not configured" }, 500));
    }

    try {
      const body = await request.json();
      const message = String(body.message || "").trim();
      const currentDate = String(body.current_date || "");
      const timezone = String(body.timezone || "");
      if (!message) return cors(json({ error: "message is required" }, 400));

      const instructions = `You are the AI assistant inside a Daily Task Manager Android app.
Reply in the SAME LANGUAGE as the user. If the user speaks Arabic, answer naturally in Arabic.
Current local date: ${currentDate}. Time zone: ${timezone}.

You can answer normal questions and you can use web search for current information. For legal questions, distinguish verified law/source material from general explanation and never invent article numbers, judgments, or citations. Prefer official sources when available.

If the user asks to add/create/save a task, extract a concise title, useful description, date and time. If a date is not stated, use the current local date. If no time is stated, use an empty string. Resolve today/tomorrow/weekday relative to the current local date. If the request is too ambiguous to safely create a task, ask a short clarification instead.

Return the required structured object only.`;

      const schema = {
        type: "object",
        additionalProperties: false,
        properties: {
          action: { type: "string", enum: ["chat", "add_task"] },
          reply: { type: "string" },
          task: {
            anyOf: [
              { type: "null" },
              {
                type: "object",
                additionalProperties: false,
                properties: {
                  title: { type: "string" },
                  description: { type: "string" },
                  date: { type: "string" },
                  time: { type: "string" }
                },
                required: ["title", "description", "date", "time"]
              }
            ]
          }
        },
        required: ["action", "reply", "task"]
      };

      const openai = await fetch("https://api.openai.com/v1/responses", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${env.OPENAI_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: env.OPENAI_MODEL || "gpt-5.6-luna",
          instructions,
          input: message,
          tools: [{ type: "web_search" }],
          tool_choice: "auto",
          text: {
            format: {
              type: "json_schema",
              name: "task_manager_response",
              strict: true,
              schema
            }
          },
          max_output_tokens: 1200
        })
      });

      const data = await openai.json();
      if (!openai.ok) {
        return cors(json({ error: "OpenAI request failed", detail: data }, openai.status));
      }

      const text = extractText(data);
      let parsed;
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = { action: "chat", reply: text || "لم يصل رد صالح من خدمة الذكاء الاصطناعي.", task: null };
      }
      return cors(json(parsed));
    } catch (e) {
      return cors(json({ error: String(e && e.message ? e.message : e) }, 500));
    }
  }
};

function extractText(data) {
  if (typeof data.output_text === "string" && data.output_text) return data.output_text;
  const parts = [];
  for (const item of (data.output || [])) {
    for (const c of (item.content || [])) {
      if (typeof c.text === "string") parts.push(c.text);
    }
  }
  return parts.join("\n");
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" }
  });
}

function cors(res) {
  const h = new Headers(res.headers);
  h.set("Access-Control-Allow-Origin", "*");
  h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  h.set("Access-Control-Allow-Methods", "POST, OPTIONS, GET");
  return new Response(res.body, { status: res.status, headers: h });
}
