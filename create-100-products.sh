#!/bin/bash
# 100 dona mahsulot qo'shish
BASE_URL="http://localhost:8080"

LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" -H "Content-Type: application/json" -d '{"usernameOrEmail":"superadmin","password":"super123"}')
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

if [ -z "$TOKEN" ]; then echo "Token olinmadi!"; exit 1; fi
echo "Token olindi ✓"

CAT_ID="1b31e706-1a2b-4c8b-bf02-175551b0bebc"
BRAND_ID="a6b23c97-00fa-4ecc-89bd-453ebf879e96"

NAMES=(
  "Smartfon Galaxy S24 Ultra" "iPhone 15 Pro Max" "Xiaomi 14 Ultra" "Pixel 8 Pro" "OnePlus 12R"
  "MacBook Pro 14" "Dell XPS 15" "ThinkPad X1 Carbon" "HP Spectre x360" "ASUS ROG Strix"
  "iPad Air M2" "Samsung Tab S9" "Xiaomi Pad 6 Pro" "Lenovo Tab P12" "Huawei MatePad Pro"
  "AirPods Pro 2" "Sony WH-1000XM5" "Samsung Buds2 Pro" "JBL Tune 770NC" "QuietComfort Ultra"
  "Apple Watch Ultra 2" "Galaxy Watch 6" "Mi Band 8" "Garmin Venu 3" "Fitbit Charge 6"
  "Samsung QLED 55" "LG OLED C3" "Sony Bravia XR" "TCL C845" "Hisense U8K"
  "Samsung Blender" "Kenwood Mikser" "Tefal Qozon" "Redmond Multivarka" "Moulinex Dasturxon"
  "LG TurboWash" "Samsung EcoBubble" "Bosch Serie 6" "Indesit EWE" "Candy RapidO"
  "Samsung NoFrost" "LG LineCooling" "Bosch VitaFresh" "Indesit Caa" "ATLANT CM"
  "Daikin FTXM" "Samsung AR95" "LG ArtCool" "Hisense AS" "Midea MS"
  "Zara Blazer" "H&M Sweatshirt" "Nike Windrunner" "Adidas Ultraboost" "Puma Tesla"
  "Nike Air Max 97" "Adidas Samba OG" "Puma RS-X" "New Balance 550" "Reebok Classic"
  "Casio G-Shock" "Seiko Presage" "Citizen Eco-Drive" "Orient Bambino" "Timex Weekender"
  "iPhone 15 Chexol" "Samsung S24 Chexol" "USB-C Zaryadka" "MagSafe Charger" "Anker Powerbank"
  "Logitech MX Keys" "Razer DeathAdder" "Dell UltraSharp 27" "LG UltraWide 34" "HP LaserJet Pro"
  "TP-Link Archer AX73" "ASUS RT-AX86U" "MikroTik hAP ac3" "Cisco SG110" "HDMI Cable 2m"
  "Samsonite Lite" "AT Samsonite" "Parker Jotter" "A4 Daftar" "BIC Ruchka"
  "Nivea Soft Cream" "Chanel No 5" "Head Shoulders" "Colgate Total" "Dove Beauty Bar"
  "Homo milk 1L" "Non Lavash" "Pomidor 1kg" "Banan 1kg" "Olma 1kg"
  "Snickers 50g" "Pepsi 1.5L" "Greenfield Tea" "Nescafe Classic" "Domino Sugar"
  "Barilla Spaghetti" "Tilda Basmati" "President Cheese" "Lenta Butter" "Schogetten"
  "Samsung S25" "Xiaomi Redmi Note 13" "Realme GT 6" "Nothing Phone 3" "iQOO 12"
)

SUCCESS=0; FAIL=0

for i in $(seq 1 100); do
  IDX=$((i - 1))
  NAME="${NAMES[$IDX]}"
  BARCODE="2026$(printf '%07d' $i)"
  WEIGHT_INT=$((1 + i % 20))
  LENGTH_INT=$((10 + i % 30))
  WIDTH_INT=$((5 + i % 15))
  HEIGHT_INT=$((2 + i % 10))
  MIN_STOCK=$((5 + i % 10))
  LEAD_DAYS=$((i % 7))
  IS_FRAGILE=$([ $((i % 5)) -eq 0 ] && echo "true" || echo "false")
  IS_FEATURED=$([ $((i % 10)) -eq 0 ] && echo "true" || echo "false")

  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/products" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$NAME\",\"barcode\":\"$BARCODE\",\"categoryId\":\"$CAT_ID\",\"brandId\":\"$BRAND_ID\",\"unit\":\"pcs\",\"leadTimeDays\":$LEAD_DAYS,\"minStock\":$MIN_STOCK,\"weight\":${WEIGHT_INT}.0,\"length\":${LENGTH_INT}.0,\"width\":${WIDTH_INT}.0,\"height\":${HEIGHT_INT}.0,\"packageType\":\"box\",\"fragile\":$IS_FRAGILE,\"active\":true,\"status\":\"PUBLISHED\",\"featured\":$IS_FEATURED,\"shortDescription\":\"$NAME - yuqori sifat\",\"tags\":[\"mahsulot\",\"yangi\"]}")

  HTTP_CODE=$(echo "$RESP" | tail -n1)
  if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    SUCCESS=$((SUCCESS + 1))
    echo "[$i/100] ✓ $NAME"
  else
    FAIL=$((FAIL + 1))
    echo "[$i/100] ✗ $NAME (HTTP $HTTP_CODE)"
  fi
done

echo ""
echo "==============================="
echo "  Muvaffaqiyatli: $SUCCESS"
echo "  Xatolik: $FAIL"
echo "==============================="
