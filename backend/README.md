# AI Backend (Cloudflare Worker)

This keeps the OpenAI API key off the Android phone and out of the APK.

## Deploy
1. Create a free Cloudflare account and open **Workers & Pages**.
2. Create a Worker, replace its code with `worker.js`, and deploy.
3. In Worker **Settings > Variables and Secrets**, add a **Secret** named `OPENAI_API_KEY` with your OpenAI API key.
4. Optional variable: `OPENAI_MODEL=gpt-5.6-luna`.
5. Copy the deployed Worker URL, add `/api/chat`, then put it in:
   `app/src/main/java/com/loanii/dailytaskmanager/AiConfig.java`
6. Rebuild the APK in GitHub Actions.

Do **not** commit your API key to GitHub.
