# AI fix setup

1. In Cloudflare Worker > Edit code, replace the entire Worker code with `backend/worker.js` from this project and Deploy.
2. Keep `OPENAI_API_KEY` as an encrypted Secret.
3. Android is configured to call: `https://daily-task-manager-ai.loaniigroup.workers.dev/api/chat`.
4. Rebuild the APK on GitHub Actions.

The fix removes the false `Done` fallback, returns Arabic replies when Arabic is used, supports web search, and keeps add-task confirmation.
