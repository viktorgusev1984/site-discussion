FROM node:22-alpine

RUN npm install --global @qwen-code/qwen-code@0.24.1 \
    && addgroup -S qwen \
    && adduser -S -G qwen -h /home/qwen qwen \
    && mkdir -p /workspace /home/qwen/.qwen \
    && chown -R qwen:qwen /workspace /home/qwen

USER qwen
WORKDIR /workspace

EXPOSE 4170

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- --header="Authorization: Bearer ${QWEN_SERVER_TOKEN}" http://127.0.0.1:4170/health >/dev/null || exit 1

# A Render free instance has a 512 MiB cgroup limit. qwen serve also launches a
# separate ACP Node process, so letting the daemon use Node's automatic heap
# limit can make the kernel kill the ACP child during preheat. Keep the small
# HTTP daemon bounded and omit its unused built-in UI; the application ships
# the Web Shell itself. The ACP child still derives its own 256 MiB heap limit.
CMD ["node", "--max-old-space-size=96", "/usr/local/lib/node_modules/@qwen-code/qwen-code/cli-entry.js", "serve", "--hostname", "0.0.0.0", "--port", "4170", "--workspace", "/workspace", "--require-auth", "--rate-limit", "--no-web", "--prompt-deadline-ms", "600000", "--writer-idle-timeout-ms", "120000"]
