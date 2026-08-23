#!/usr/bin/env python3
# =====================================================================
#  test-falabella.py
#  Version Python del test de credenciales (si prefieres esta a la de
#  PowerShell). Firma verificada byte a byte contra HmacSignatureService.java:
#  mismo string-to-sign y misma firma HMAC-SHA256.
#
#  Uso:   python test-falabella.py
#  No necesita instalar nada (solo libreria estandar).
# =====================================================================

import datetime
import hashlib
import hmac
import json
import urllib.error
import urllib.parse
import urllib.request

USER_ID = "ximena.huala@usach.cl"
API_KEY = "d8885527dea9577e51efd73a85ba978b072d3b03"
SELLER_ID = "SC459DF"
BASE_URL = "https://sellercenter-api.falabella.com"
VERSION = "1.0"


def timestamp():
    """ISO 8601 con offset local, igual que OffsetDateTime.now() en el backend."""
    ts = datetime.datetime.now().astimezone().strftime("%Y-%m-%dT%H:%M:%S%z")
    return ts[:-2] + ":" + ts[-2:]


def string_to_sign(params):
    return "&".join(
        f"{urllib.parse.quote_plus(k)}={urllib.parse.quote_plus(v)}"
        for k, v in sorted(params.items())
    )


def llamar(action, extra=None, romper_firma=False):
    params = {
        "Action": action,
        "Format": "JSON",
        "Timestamp": timestamp(),
        "UserID": USER_ID,
        "Version": VERSION,
    }
    if extra:
        params.update(extra)

    firma = hmac.new(
        API_KEY.encode("utf-8"),
        string_to_sign(params).encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    if romper_firma:
        firma = firma[:-1] + ("b" if firma[-1] == "a" else "a")

    params["Signature"] = firma
    url = BASE_URL + "/?" + string_to_sign(params)
    req = urllib.request.Request(url, headers={"User-Agent": f"{SELLER_ID}/Java/21"})

    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return "SIN_RED", repr(e)


def mostrar(titulo, resultado):
    code, body = resultado
    print()
    print(f"=== {titulo}  ->  HTTP {code}")
    try:
        body = json.dumps(json.loads(body), indent=2, ensure_ascii=False)
    except Exception:
        pass
    print(body[:1500] + (" ...[truncado]" if len(body) > 1500 else ""))


if __name__ == "__main__":
    print(f"UserID  : {USER_ID}")
    print(f"SellerID: {SELLER_ID}")
    print(f"ApiKey  : {API_KEY[:6]}...{API_KEY[-4:]}  ({len(API_KEY)} chars)")

    # 1) Control negativo: firma rota a proposito -> debe dar E007.
    mostrar(
        "1. Firma ROTA a proposito (se espera E007)",
        llamar("GetMetrics", {"StatisticsType": "alltime"}, romper_firma=True),
    )
    mostrar(
        "2. GetMetrics (firma correcta)",
        llamar("GetMetrics", {"StatisticsType": "alltime"}),
    )
    mostrar(
        "3. GetOrders",
        llamar("GetOrders", {
            "CreatedAfter": "2026-01-01T00:00:00-04:00",
            "CreatedBefore": "2026-02-01T00:00:00-04:00",
            "Limit": "5",
            "Offset": "0",
        }),
    )
    mostrar("4. GetProducts", llamar("GetProducts", {"Limit": "2", "Offset": "0"}))

    print()
    print("-" * 62)
    print("COMO LEER EL RESULTADO:")
    print("  * 1 da E007 y 2/3/4 dan 200   -> credenciales OK.")
    print("  * 1 da E007 y 2/3/4 dan E009  -> la firma esta bien; el problema")
    print("    es de PERMISOS de la cuenta en Seller Center.")
    print("  * TODAS dan E007              -> la API Key no corresponde al UserID.")
    print("  * TODAS dan SIN_RED           -> firewall/proxy, no es la credencial.")
    print("-" * 62)
