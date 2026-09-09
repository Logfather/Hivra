FROM nvcr.io/nvidia/cuda-dl-base:25.08-cuda13.0-runtime-ubuntu24.04

ARG PYTHON_VERSION=3.13.14
ARG UV_VERSION=0.12.2

LABEL org.opencontainers.image.title="HIM A100 Reference Training Runtime" \
      org.opencontainers.image.version="v1" \
      org.opencontainers.image.description="Provider-neutral HIM runtime with a thin RunPod SSH adapter" \
      org.opencontainers.image.base.name="nvcr.io/nvidia/cuda-dl-base:25.08-cuda13.0-runtime-ubuntu24.04"

# These are operational image-build packages. HIM numerical dependencies are
# realized only from the copied, unchanged uv.lock below.
RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
        openssh-server \
    && rm -rf /var/lib/apt/lists/*

# The official Astral installer is version-pinned through UV_VERSION.
RUN bash -o pipefail -c \
    'curl --fail --silent --show-error --location https://astral.sh/uv/install.sh \
      | env UV_VERSION="${UV_VERSION}" UV_UNMANAGED_INSTALL=/usr/local/bin sh'

ENV UV_PYTHON_INSTALL_DIR=/opt/him/python \
    PATH=/opt/him/runtime/bin:/opt/him/python/bin:/usr/local/bin:$PATH \
    UV_PROJECT_ENVIRONMENT=/opt/him/runtime

RUN install --directory --mode=0755 \
        /opt/him \
        /opt/him/bin \
        /opt/him/dependency-authority \
        /opt/him/validation \
        /opt/him/runtime \
        /workspace

COPY pyproject.toml uv.lock /opt/him/dependency-authority/
COPY validation/a100_validation_v1.py /opt/him/validation/a100_validation_v1.py
COPY runtime-image-definition.json /opt/him/runtime-image-definition.json
COPY runtime-identity.json /opt/him/runtime/runtime-identity.json
COPY start.sh /opt/him/bin/start.sh
COPY validation/him_runtime_validation_v1.py /opt/him/validation/him_runtime_validation_v1.py

# Python and all numerical dependencies are realized from exact version and
# lock authority at image build time; no manual torch installation is used.
RUN uv python install --install-dir "${UV_PYTHON_INSTALL_DIR}" "${PYTHON_VERSION}" \
    && test -x /opt/him/python/bin/python3.13 \
    && uv sync --frozen --no-dev \
        --project /opt/him/dependency-authority \
        --python "${PYTHON_VERSION}" \
    && test -x /opt/him/runtime/bin/python \
    && test -f /opt/him/runtime/lib/python3.13/site-packages/torch/__init__.py \
    && /opt/him/runtime/bin/python \
        /opt/him/validation/him_runtime_validation_v1.py \
        --mode build \
        --lineage-output /opt/him/validation/build-lineage.json

# Copy the existing trainer/parser package unchanged into the canonical
# interpreter site-packages path.
COPY trainer/him_trainer /opt/him/runtime/lib/python3.13/site-packages/him_trainer

RUN /opt/him/runtime/bin/python \
        -c 'import him_trainer; import him_trainer.__main__ as entrypoint; assert callable(entrypoint.run)' \
    && test -f /opt/him/runtime/lib/python3.13/site-packages/him_trainer/protocol_v1.py

RUN chmod 0755 /opt/him/bin/start.sh \
    && chmod 0644 /opt/him/validation/a100_validation_v1.py \
    && chmod 0644 /opt/him/validation/him_runtime_validation_v1.py \
    && chmod 0644 /opt/him/validation/build-lineage.json \
    && chmod 0644 /opt/him/runtime-image-definition.json \
    && chmod 0644 /opt/him/runtime/runtime-identity.json

ENTRYPOINT ["/opt/him/bin/start.sh"]
