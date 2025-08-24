import json, os, requests

ES_URL = os.getenv("ES_URL", "http://localhost:9200")
ES_USER = os.getenv("ES_USER", "elastic")
ES_PASS = os.getenv("ES_PASS", "changeme")
PRODUCTS_JSON = os.getenv("PRODUCTS_JSON", "./FRONTEND/public/data/products.json")

with open(PRODUCTS_JSON, "r", encoding="utf-8") as f:
    data = json.load(f)

lines = []
for p in data:
    idx = {"index": {"_index": "products", "_id": str(p["id"])}}
    lines.append(json.dumps(idx))
    # Ajusta tipos/transformaciones si es necesario
    body = {
        "id": str(p["id"]),
        "name": p.get("name", ""),
        "description": p.get("description", ""),
        "category": p.get("category", ""),
        "price": float(p.get("price", 0)),
        "image": p.get("image", "")
    }
    lines.append(json.dumps(body))

bulk_body = "\n".join(lines) + "\n"
r = requests.post(f"{ES_URL}/_bulk", data=bulk_body,
                  headers={"Content-Type": "application/x-ndjson"},
                  auth=(ES_USER, ES_PASS))
r.raise_for_status()
print("Bulk indexing OK:", r.json())
