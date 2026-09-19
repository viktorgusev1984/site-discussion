FROM node:22-alpine

COPY patch-acp-memory.mjs /tmp/patch-acp-memory.mjs

RUN npm install --global @qwen-code/qwen-code@0.24.1 \
    && node /tmp/patch-acp-memory.mjs /usr/local/lib/node_modules/@qwen-code/qwen-code \
    && rm /tmp/patch-acp-memory.mjs \
    && addgroup -S qwen \
    && adduser -S -G qwen -h /home/qwen qwen \
    && mkdir -p /workspace /home/qwen/.qwen \
    && chown -R qwen:qwen /workspace /home/qwen

USER qwen
WORKDIR /workspace

EXPOSE 4170

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- --header="Authorization: Bearer ${QWEN_SERVER_TOKEN}" http://127.0.0.1:4170/health >/dev/null || exit 1

# The 512 MiB POC instance runs both this HTTP daemon and an ACP Node child.
# Bound both heaps and omit the unused built-in UI. The stock 0.24.1 launcher
# always gives the child half of container memory; the build-time patch above
# makes that independently configurable through QWEN_ACP_HEAP_MB.
ENV QWEN_ACP_HEAP_MB=160
# The default ACP initialize deadline is only 10 seconds. On a throttled free
# instance the child can need longer than that just to load the CLI bundle; the
# daemon otherwise kills the healthy-but-slow child and repeats the same failure
# for the first real session.
CMD ["node", "--max-old-space-size=96", "/usr/local/lib/node_modules/@qwen-code/qwen-code/cli-entry.js", "serve", "--hostname", "0.0.0.0", "--port", "4170", "--workspace", "/workspace", "--require-auth", "--rate-limit", "--no-web", "--max-journal-bytes", "1048576", "--initialize-timeout-ms", "60000", "--prompt-deadline-ms", "600000", "--writer-idle-timeout-ms", "120000"]
