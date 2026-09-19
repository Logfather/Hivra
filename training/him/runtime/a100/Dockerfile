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
    UV_PROJECT_ENVIRONMENT=/opt/him/runtime \
    HIM_CUDA_COMPAT_REQUIRED=YES \
    HIM_CUDA_COMPAT_PREFIX=/usr/local/cuda-13.0/compat/lib.real:/usr/local/cuda-13.0/compat \
    LD_LIBRARY_PATH=/usr/local/cuda-13.0/compat/lib.real:/usr/local/cuda-13.0/compat

RUN install --directory --mode=0755 \
        /opt/him \
        /opt/him/bin \
        /opt/him/dependency-authority \
        /opt/him/validation \
        /workspace

COPY pyproject.toml uv.lock /opt/him/dependency-authority/
COPY validation/a100_validation_v1.py /opt/him/validation/a100_validation_v1.py
COPY runtime-image-definition.json /opt/him/runtime-image-definition.json
COPY start.sh /opt/him/bin/start.sh
COPY validation/him_runtime_validation_v1.py /opt/him/validation/him_runtime_validation_v1.py
COPY build-gate/cuda-protocol-parse-gate.py /tmp/cuda-protocol-parse-gate.py

# Python and all numerical dependencies are realized from exact version and
# lock authority at image build time; no manual torch installation is used.
RUN uv python install --install-dir "${UV_PYTHON_INSTALL_DIR}" "${PYTHON_VERSION}" \
    && test -x /opt/him/python/cpython-3.13.14-linux-x86_64-gnu/bin/python3.13 \
    && install --directory --mode=0755 /opt/him/python/bin \
    && if [ -e /opt/him/python/bin/python3.13 ] || [ -L /opt/him/python/bin/python3.13 ]; then \
        test -L /opt/him/python/bin/python3.13 \
        && test "$(readlink /opt/him/python/bin/python3.13)" = "../cpython-3.13.14-linux-x86_64-gnu/bin/python3.13"; \
    else \
        ln -s ../cpython-3.13.14-linux-x86_64-gnu/bin/python3.13 /opt/him/python/bin/python3.13; \
    fi \
    && test -x /opt/him/python/bin/python3.13 \
    && /opt/him/python/bin/python3.13 --version \
    && test ! -e /opt/him/runtime \
    && uv sync --frozen --no-dev \
        --project /opt/him/dependency-authority \
        --python /opt/him/python/bin/python3.13 \
    && test -x /opt/him/runtime/bin/python \
    && test -f /opt/him/runtime/lib/python3.13/site-packages/torch/__init__.py

COPY runtime-identity.json /opt/him/runtime/runtime-identity.json
COPY training/him/runtime/a100/final-training-authority-packet-v1 /opt/him/training/him/runtime/a100/final-training-authority-packet-v1
COPY training/him/runtime/a100/final-training-runtime-authority.v2.json /opt/him/training/him/runtime/a100/final-training-runtime-authority.v2.json
COPY training/him/runtime/a100/final-evaluation-authority-v1 /opt/him/training/him/runtime/a100/final-evaluation-authority-v1

RUN /opt/him/runtime/bin/python \
        /opt/him/validation/him_runtime_validation_v1.py \
        --mode build \
        --lineage-output /opt/him/validation/build-lineage.json

# Copy the explicit HIM runtime source closure unchanged into the one
# canonical interpreter site-packages path.
COPY trainer/him_trainer /opt/him/runtime/lib/python3.13/site-packages/him_trainer

RUN /opt/him/runtime/bin/python \
        -c 'import hashlib, importlib.util, json, pathlib; import him_trainer; import him_trainer.__main__ as entrypoint; import him_trainer.point13_model_forward_v1 as point13; import tokenizers, sentencepiece, torch; assert callable(entrypoint.run); assert point13.__file__ == "/opt/him/runtime/lib/python3.13/site-packages/him_trainer/point13_model_forward_v1.py"; assert hasattr(point13, "load_pinned_him_multi_head_model_from_root_v1"); assert hasattr(point13, "build_him_multi_head_model_v1"); assert hasattr(point13, "HimMultiHeadModelV1"); assert hasattr(point13, "HimMultiHeadForwardOutputV1"); assert torch.__version__ == "2.14.0+cu130"; assert torch.version.cuda == "13.0"; assert tokenizers.__version__ == "0.23.1"; assert sentencepiece.__version__ == "0.2.2"; assert importlib.util.find_spec("transformers") is None; identity=json.loads(pathlib.Path("/opt/him/runtime/runtime-identity.json").read_text()); point13_digest=hashlib.sha256(pathlib.Path(point13.__file__).read_bytes()).hexdigest(); point13_entry=next(item for item in identity["sourceTreeFileDigests"] if item["sourcePath"].endswith("/point13_model_forward_v1.py")); assert point13_digest == point13_entry["sha256"]' \
      && /opt/him/runtime/bin/python -c 'import importlib, json, pathlib; root=pathlib.Path("/opt/him/runtime/lib/python3.13/site-packages/him_trainer"); py_files=list(root.rglob("*.py")); assert len(py_files) == 39; identity=json.loads(pathlib.Path("/opt/him/runtime/runtime-identity.json").read_text()); assert len(identity["sourceTreeFileDigests"]) == 40; nested=root / "evaluation_authorities" / "productive_v2.py"; assert nested.is_file(); assert not (root / "productive_v2.py").exists(); modules=["him_trainer." + p.relative_to(root).with_suffix("").as_posix().replace("/", ".") for p in py_files if p.name != "__init__.py"]; [importlib.import_module(m) for m in modules]'

# The P2 V2 binding is a separate active entrypoint.  Import every authority
# that it consumes in the image so a locally-present module cannot be omitted
# from the immutable runtime closure.
RUN /opt/him/runtime/bin/python \
        -c 'import him_trainer.evaluation_authorities.productive_v2, him_trainer.batch_size_authority_v2, him_trainer.blind_expanded_validation_v2, him_trainer.corpus_assembly_v2, him_trainer.corpus_coverage_v2, him_trainer.evidence_projection_v2, him_trainer.final_evaluation_authority_v1, him_trainer.final_training_runtime_closure_v1, him_trainer.input_representation_v2, him_trainer.partition_leakage_v2, him_trainer.partition_v2, him_trainer.post_review_eligibility_v2, him_trainer.productive_training_p2_v2, him_trainer.sequence_length_authority_v2, him_trainer.training_input_authority_v2, him_trainer.training_readiness_authority_v2, him_trainer.training_runtime_authority_v2'

RUN /opt/him/runtime/bin/python /tmp/cuda-protocol-parse-gate.py \
    && rm -f /tmp/cuda-protocol-parse-gate.py

RUN chmod 0755 /opt/him/bin/start.sh \
    && chmod 0644 /opt/him/validation/a100_validation_v1.py \
    && chmod 0644 /opt/him/validation/him_runtime_validation_v1.py \
    && chmod 0644 /opt/him/validation/build-lineage.json \
    && chmod 0644 /opt/him/runtime-image-definition.json \
    && chmod 0644 /opt/him/runtime/runtime-identity.json \
    && chmod 0644 /opt/him/training/him/runtime/a100/final-training-runtime-authority.v2.json \
    && chmod 0644 /opt/him/training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json \
    && find /opt/him/training/him/runtime/a100/final-training-authority-packet-v1 -type f -name '*.json' -exec chmod 0644 {} +

ENTRYPOINT ["/opt/him/bin/start.sh"]
