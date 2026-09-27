# imagePullSecrets — how the prod overlay pulls from ghcr.io
#
# ── WHY THIS IS A DOCUMENT AND NOT A MANIFEST ───────────────────────────────
# It is tempting to add
#
#     imagePullSecrets:
#       - name: ghcr-pull
#
# to all 11 Deployments and a matching Secret to `resources:`. Do not, yet, and
# the reason is worth stating precisely, because the failure is silent and total:
#
#   * A `secretRef` in `imagePullSecrets` that does not resolve does not produce
#     a warning. The pod is created, the kubelet tries to pull, and the pod sits
#     in `ErrImagePull` -> `ImagePullBackOff` forever.
#   * All 11 services would do that at once.
#   * `kubectl apply --dry-run=client` would report success, and so would
#     `kubectl kustomize`. Neither checks that the Secret exists.
#
# ghcr.io does serve PUBLIC packages anonymously, so a public image needs no
# pull secret at all. This placeholder exists for the two cases where one IS
# required:
#   * the packages are private (a paid org, or a registry you control)
#   * you move to a registry with mandatory authentication
#
# ── STEP 1: create the Secret ──────────────────────────────────────────────
# For a GitHub PAT with `read:packages`:
#
#     kubectl -n csph-gpl create secret docker-registry ghcr-pull \
#       --docker-server=ghcr.io \
#       --docker-username=csp-hq \
#       --docker-password='<PAT with read:packages>' \
#       --docker-email='platform@csp.cm'
#
# Do NOT put that in git, and do NOT reuse the dev `gpl-app-credentials` Secret:
# the two Secret objects have two different audiences by design (see the header
# of k8s/base/secrets.yaml). Add a third audience, not a second purpose for an
# existing one.
#
# ── STEP 2: reference it from the Deployments ───────────────────────────────
# Add to kustomization.yaml, above `patches:`:
#
#     secretGenerator:
#       - name: ghcr-pull
#         literals:
#           - .dockerconfigjson=<base64 of the JSON below>
#         options:
#           labels:
#             app.kubernetes.io/component: registry-credential
#         # The generator appends a content hash to the name, which is what
#         # forces a rollout when the credential rotates. Note the `disableNameSuffixHash`
#         # alternative below before choosing.
#
#     generatorOptions:
#       disableNameSuffixHash: true   # only if you reference the name literally
#
# and then add the reference to all 11 Deployments, either by extending
# patches/jvm-workloads-prod.yaml with
#
#     spec:
#       template:
#         spec:
#           imagePullSecrets:
#             - name: ghcr-pull
#
# or — cleaner, and the option that survives credential rotation — with a small
# dedicated patch file per workload, or a `patches:` entry using a `target:`
# selector:
#
#     patches:
#       - target:
#           kind: Deployment
#           labelSelector: app.kubernetes.io/part-of=csph-gpl-platform
#         patch: |-
#           - op: add
#             path: /spec/template/spec/imagePullSecrets
#             value:
#               - name: ghcr-pull
#
# ── THE CREDENTIAL CONTENT ──────────────────────────────────────────────────
#
#     {
#       "auths": {
#         "ghcr.io": {
#           "username": "csp-hq",
#           "password": "<PAT>",
#           "auth": "<base64 of username:password>"
#         }
#       }
#     }
#
# then base64 the WHOLE thing for `literals`, or store it as a file and use
# `files:` instead of `literals:`.
#
# ── THE RIGHT LONG-TERM ANSWER ─────────────────────────────────────────────
# A docker-registry Secret is a credential in a ConfigMap-shaped object that
# every pod can read and that lands in etcd, in a backup, and in any
# `kubectl get secret -o yaml` an operator ever pastes into a ticket. Before
# real production, replace it with one of:
#
#   * External Secrets Operator  — syncs from Vault / AWS Secrets Manager /
#     GCP Secret Manager, and refreshes on a schedule
#   * SOPS + Flux/Argo CD        — the Secret is encrypted at rest in git and
#     decrypted only by the operator
#   * SealedSecrets              — a controller that only the cluster's private
#     key can unseal
#
# All three keep the `imagePullSecrets: [{name: ...}]` reference in this
# overlay unchanged, which is the point: the reference is the interface, and
# only the contents of the object should ever change.
#
# ── VERIFY, DO NOT ASSUME ───────────────────────────────────────────────────
#
#     kubectl -n csph-gpl get secret ghcr-pull
#     kubectl -n csph-gpl rollout restart deploy/api-gateway
#     kubectl -n csph-gpl rollout status  deploy/api-gateway
#     kubectl -n csph-gpl get events --field-selector reason=Failed | tail
#
# And the negative test, which is the one that catches a bad credential:
#
#     kubectl -n csph-gpl scale deploy/api-gateway --replicas=0
#     kubectl -n csph-gpl set image deploy/api-gateway api-gateway=ghcr.io/csp-hq/does-not-exist:v1.0.0
#     kubectl -n csph-gpl rollout status deploy/api-gateway --timeout=60s   # MUST fail
#     kubectl -n csph-gpl set image deploy/api-gateway api-gateway=ghcr.io/csp-hq/gpl-api-gateway:v1.0.0
#     kubectl -n csph-gpl rollout status deploy/api-gateway --timeout=180s  # MUST succeed
#
# If the "must fail" case succeeds, your image reference is wrong. If the
# "must succeed" case fails with ImagePullBackOff, your credential is wrong.
# Those are different problems and this is how you tell them apart in ten
# seconds instead of an hour.
