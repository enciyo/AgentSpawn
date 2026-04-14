#!/bin/sh
set -e
mkdir -p /app/web-static
# Önce bilinen path'leri dene
if [ -d /app/composeApp/build/dist/js/productionExecutable ]; then
  cp -r /app/composeApp/build/dist/js/productionExecutable/. /app/web-static/
elif [ -d /app/composeApp/build/distributions ]; then
  cp -r /app/composeApp/build/distributions/. /app/web-static/
else
  find /app/composeApp/build -name "composeApp.js" -type f 2>/dev/null | head -1 | while read f; do
    cp -r "$(dirname "$f")"/. /app/web-static/
  done
fi
[ -f /app/web-static/composeApp.js ] || [ -f /app/web-static/index.html ] || {
  echo "Web build output not found. Listing build:"
  ls -laR /app/composeApp/build 2>/dev/null | head -100
  exit 1
}
