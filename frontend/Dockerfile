# syntax=docker/dockerfile:1.7
FROM node:22.14.0-alpine3.21 AS build
ARG BUILD_CONFIGURATION=uat
ARG BUILD_VERSION=unknown
ARG BUILD_REVISION=unknown
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npx ng build --configuration "${BUILD_CONFIGURATION}" \
    && printf '{"version":"%s","revision":"%s"}\n' "${BUILD_VERSION}" "${BUILD_REVISION}" \
       > dist/frontend/browser/build-info.json

FROM nginxinc/nginx-unprivileged:1.27.4-alpine3.21 AS runtime
ARG BUILD_VERSION=unknown
ARG BUILD_REVISION=unknown
LABEL org.opencontainers.image.title="MeghaConnect Frontend" \
      org.opencontainers.image.version="${BUILD_VERSION}" \
      org.opencontainers.image.revision="${BUILD_REVISION}"

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=101:101 /workspace/dist/frontend/browser/ /usr/share/nginx/html/

USER 101:101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1
