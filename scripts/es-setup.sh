#!/usr/bin/env bash
set -e

ES_URL="${1:-http://localhost:9200}"
ES_USER="${2:-elastic}"
ES_PASS="${3:-changeme}"

curl -u "$ES_USER:$ES_PASS" -X PUT "$ES_URL/products" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "id":        { "type": "keyword" },
      "name":      { "type": "search_as_you_type" },
      "description": { "type": "text" },
      "category":  { "type": "keyword" },
      "price":     { "type": "double" },
      "image":     { "type": "keyword" }
    }
  }
}'
echo
echo "Índice 'products' creado con mapping."
