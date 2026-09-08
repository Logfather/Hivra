#!/usr/bin/env bash
set -Eeuo pipefail

readonly HIM_ROOT=/opt/him
readonly HIM_BIN_ROOT=/opt/him/bin
readonly HIM_VALIDATION_ROOT=/opt/him/validation
readonly HIM_RUNTIME_ROOT=/opt/him/runtime
readonly HIM_RUNTIME_PYTHON=/opt/him/runtime/bin/python
readonly HIM_RUNTIME_VALIDATOR=/opt/him/validation/him_runtime_validation_v1.py
readonly HIM_WORKSPACE_ROOT=/workspace
readonly SSH_CONFIG_DROP_IN=/etc/ssh/sshd_config.d/99-him-runtime.conf
readonly AUTHORIZED_KEYS=/root/.ssh/authorized_keys

log() {
    printf 'him-runtime: %s\n' "$1"
}

fail_closed() {
    printf 'him-runtime: bootstrap failed closed: %s\n' "$1" >&2
    exit 78
}

[[ "$(uname -s)" == "Linux" ]] || fail_closed "Linux host required"
case "$(uname -m)" in
    x86_64|amd64) ;;
    *) fail_closed "x86_64 host required" ;;
esac

[[ -d "$HIM_ROOT" ]] || fail_closed "missing immutable runtime root"
[[ -d "$HIM_BIN_ROOT" ]] || fail_closed "missing runtime bin root"
[[ -d "$HIM_VALIDATION_ROOT" ]] || fail_closed "missing validation root"
[[ -d "$HIM_RUNTIME_ROOT" ]] || fail_closed "missing Python runtime root"
[[ -x "$HIM_RUNTIME_PYTHON" ]] || fail_closed "authoritative Python runtime is unavailable"
[[ -f "$HIM_RUNTIME_VALIDATOR" ]] || fail_closed "runtime validator is unavailable"
[[ -d "$HIM_WORKSPACE_ROOT" ]] || fail_closed "missing mutable workspace root"
[[ -x /usr/sbin/sshd ]] || fail_closed "openssh-server is unavailable"
[[ -x /usr/bin/ssh-keygen ]] || fail_closed "ssh-keygen is unavailable"

readonly PUBLIC_KEY_VALUE="${PUBLIC_KEY:-}"
[[ -n "$PUBLIC_KEY_VALUE" ]] || fail_closed "provider public key is missing"
[[ "$PUBLIC_KEY_VALUE" != *$'\n'* ]] || fail_closed "provider public key must be one line"

"$HIM_RUNTIME_PYTHON" "$HIM_RUNTIME_VALIDATOR" --mode build \
    || fail_closed "Python/PyTorch runtime validation failed"

install --directory --mode=0700 /root/.ssh /run/sshd /etc/ssh/sshd_config.d
printf '%s\n' "$PUBLIC_KEY_VALUE" > "$AUTHORIZED_KEYS"
chmod 0600 "$AUTHORIZED_KEYS"
chown root:root /root/.ssh "$AUTHORIZED_KEYS"

ssh-keygen -lf "$AUTHORIZED_KEYS" >/dev/null 2>&1 \
    || fail_closed "provider public key is malformed"

# Host private keys are generated per container and never baked into the image.
ssh-keygen -A >/dev/null 2>&1 || fail_closed "SSH host-key generation failed"

cat > "$SSH_CONFIG_DROP_IN" <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
PermitEmptyPasswords no
PubkeyAuthentication yes
PermitRootLogin prohibit-password
AuthorizedKeysFile .ssh/authorized_keys
AllowTcpForwarding no
X11Forwarding no
PermitTunnel no
GatewayPorts no
EOF
chmod 0644 "$SSH_CONFIG_DROP_IN"

/usr/sbin/sshd -t -f /etc/ssh/sshd_config \
    || fail_closed "SSH configuration validation failed"

log "SSH public-key bootstrap complete"
log "validation tooling available at /opt/him/validation"
log "training and model download autostart disabled"

# Keep the image alive as the foreground lifecycle process; no external
# keepalive override is required.
exec /usr/sbin/sshd -D -e
