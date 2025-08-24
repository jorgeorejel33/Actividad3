#!/usr/bin/env python3
import os, sys, json, base64, urllib.request, urllib.error

def env(name, default=None):
    v = os.environ.get(name, default)
    if v is None or str(v).strip() == "":
        return None
    return v

def make_basic_auth(user, password):
    token = f"{user}:{password}".encode("utf-8")
    return "Basic " + base64.b64encode(token).decode("ascii")

def main():
    PRODUCTS_JSON = "products.json"
    ES_URL = env("ES_URL", "http://localhost:9200")
    ES_USER = env("ES_USER", "elastic")
    ES_PASS = env("ES_PASS", "changeme")
    INDEX = env("ES_INDEX", "products")

    if not PRODUCTS_JSON:
        print("ERROR: defina la variable de entorno PRODUCTS_JSON con la ruta al archivo products.json", file=sys.stderr)
        sys.exit(1)

    if not os.path.isfile(PRODUCTS_JSON):
        print(f"ERROR: no existe el archivo: {PRODUCTS_JSON}", file=sys.stderr)
        sys.exit(1)

    # Normalizar URL base
    ES_URL = ES_URL.rstrip("/")

    # Leer arreglo JSON
    with open(PRODUCTS_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
        if not isinstance(data, list):
            print("ERROR: el archivo JSON debe contener una lista de productos", file=sys.stderr)
            sys.exit(1)

    # Construir NDJSON para la Bulk API
    # Cada documento lleva _index=products y _id = product['id']
    lines = []
    for prod in data:
        _id = str(prod.get("id"))
        lines.append(json.dumps({ "index": { "_index": INDEX, "_id": _id }}, ensure_ascii=False))
        lines.append(json.dumps(prod, ensure_ascii=False))
    payload = ("\n".join(lines) + "\n").encode("utf-8")

    # Preparar petición bulk
    bulk_url = f"{ES_URL}/_bulk"
    req = urllib.request.Request(bulk_url, data=payload, method="POST")
    req.add_header("Content-Type", "application/x-ndjson")
    req.add_header("Authorization", make_basic_auth(ES_USER, ES_PASS))

    try:
        with urllib.request.urlopen(req) as resp:
            body = resp.read().decode("utf-8")
        # Validación mínima: que no haya "errors": true
        obj = json.loads(body)
        if obj.get("errors"):
            print("ADVERTENCIA: la respuesta de Bulk reporta errors=true. Revise detalles:", file=sys.stderr)
            print(body)
        else:
            print("Bulk indexing completado sin errores.")
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code} al llamar Bulk API:\n{e.read().decode('utf-8')}", file=sys.stderr)
        sys.exit(2)
    except urllib.error.URLError as e:
        print(f"Error de red al llamar Bulk API: {e}", file=sys.stderr)
        sys.exit(2)

    # Refresh del índice
    try:
        ref_url = f"{ES_URL}/{INDEX}/_refresh"
        req2 = urllib.request.Request(ref_url, method="POST")
        req2.add_header("Authorization", make_basic_auth(ES_USER, ES_PASS))
        urllib.request.urlopen(req2).read()
    except Exception as e:
        print(f"ADVERTENCIA: no se pudo refrescar el índice: {e}", file=sys.stderr)

    # Conteo
    try:
        cnt_url = f"{ES_URL}/{INDEX}/_count"
        req3 = urllib.request.Request(cnt_url, method="GET")
        req3.add_header("Authorization", make_basic_auth(ES_USER, ES_PASS))
        with urllib.request.urlopen(req3) as resp3:
            cnt = json.loads(resp3.read().decode("utf-8")).get("count")
            print(f"Documentos en '{INDEX}': {cnt}")
    except Exception as e:
        print(f"ADVERTENCIA: no se pudo obtener el conteo: {e}", file=sys.stderr)

if __name__ == "__main__":
    main()
