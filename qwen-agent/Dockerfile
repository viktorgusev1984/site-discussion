FROM node:22-alpine

# The daemon's process-tree reaper shells out to `ps -o state=,nlwp=`, which busybox ps rejects.
RUN apk add --no-cache procps

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

CMD ["qwen", "serve", "--hostname", "0.0.0.0", "--port", "4170", "--workspace", "/workspace", "--require-auth"]
