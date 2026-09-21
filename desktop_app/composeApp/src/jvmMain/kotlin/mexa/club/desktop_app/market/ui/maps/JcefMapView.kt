package mexa.club.desktop_app.market.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.awt.SwingPanel
import kotlinx.coroutines.delay
import org.cef.browser.CefBrowser
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefFrame
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.callback.CefQueryCallback
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Google Maps JavaScript API kaliti (desktop admin xarita uchun).
 * Manbalar: GOOGLE_MAPS_API_KEY env → loyiha ildizidagi .env (git'ga tushmaydi).
 */
internal object GoogleMapsConfig {
    val apiKey: String by lazy {
        System.getenv("GOOGLE_MAPS_API_KEY")?.takeIf { it.isNotBlank() }
            ?: findDotEnvKey()
            ?: ""
    }

    private fun findDotEnvKey(): String? {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        repeat(4) {
            val env = File(dir, ".env")
            if (env.isFile) {
                env.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val t = line.trim()
                        if (t.startsWith("GOOGLE_MAPS_API_KEY=")) {
                            val v = t.substringAfter("=").trim().trim('"', '\'')
                            if (v.isNotEmpty()) return v
                        }
                    }
                }
            }
            dir = dir?.parentFile
        }
        return null
    }
}

private val LEAFLET_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    /* Google Maps (Leaflet olib tashlandi) */
    * { margin: 0; padding: 0; }
    html, body, #map { width: 100%; height: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    .edit-panel {
      background: #fff; border-radius: 16px; padding: 20px; min-width: 240px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.10), 0 1px 4px rgba(0,0,0,0.06);
      user-select: none; max-height: 95vh; overflow-y: auto;
      border: 1px solid #E8E5F5;
    }
    .panel-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 2px solid #F0EFF5; display: flex; align-items: center; gap: 8px; letter-spacing: -0.01em; }
    .tool-row { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
    .tool-btn { display: flex; align-items: center; gap: 6px; flex: 1; min-width: 60px; padding: 8px 10px; border: 1px solid #E2DFEE; border-radius: 10px; background: #F8F7FC; cursor: pointer; font-size: 12px; color: #1B1B23; transition: all 0.15s; justify-content: center; white-space: nowrap; }
    .tool-btn:hover { background: #EEEDF8; border-color: #D0CDE5; }
    .tool-btn.active { background: #4648D4; color: #fff; border-color: #4648D4; box-shadow: 0 2px 8px rgba(70,72,212,0.25); }
    .tool-btn.disabled { opacity: 0.4; cursor: not-allowed; }
    .tool-btn .icon { font-size: 14px; }
    .colors-section { margin: 10px 0; padding-top: 10px; border-top: 1px solid #F0EFF5; display: none; }
    .colors-section.visible { display: block; }
    .section-label { font-size: 10px; color: #8A85A0; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.8px; font-weight: 700; }
    .color-grid { display: flex; flex-wrap: wrap; gap: 5px; }
    .color-swatch { width: 24px; height: 24px; border-radius: 8px; cursor: pointer; border: 2px solid transparent; transition: all 0.12s; }
    .color-swatch:hover { transform: scale(1.18); border-color: #C7C4D7; }
    .color-swatch.active { border-color: #4648D4; box-shadow: 0 0 0 1px #fff, 0 0 0 3px #4648D4; }

    .wh-selector { margin-bottom: 14px; }
    .wh-selector select { width: 100%; padding: 9px 10px; border: 1px solid #E2DFEE; border-radius: 10px; font-size: 13px; background: #F8F7FC; outline: none; color: #1B1B23; box-sizing: border-box; cursor: pointer; }
    .wh-selector select:focus { border-color: #4648D4; box-shadow: 0 0 0 3px rgba(70,72,212,0.12); }
    .save-btn { width: 100%; padding: 10px; background: #4648D4; color: #fff; border: none; border-radius: 10px; cursor: pointer; font-weight: 600; font-size: 13px; margin-top: 8px; transition: background 0.15s, transform 0.1s; display: flex; align-items: center; justify-content: center; gap: 6px; }
    .save-btn:hover { background: #6063EE; }
    .save-btn:active { transform: scale(0.97); }
    .status-bar { font-size: 11px; color: #8A85A0; margin-top: 12px; padding-top: 10px; border-top: 1px solid #F0EFF5; text-align: center; line-height: 1.4; }
    .location-btn { border-color: #10B981; color: #10B981; }
    .location-btn:hover { background: #ECFDF5; border-color: #10B981; }
    .location-btn.active { background: #10B981; color: #fff; border-color: #10B981; box-shadow: 0 2px 8px rgba(16,185,129,0.25); }
    .leaflet-draw-toolbar { display: none !important; }
    #mapPanel { position: absolute; top: 12px; right: 12px; z-index: 5; width: 280px; }
    .map-error { position: absolute; top: 12px; left: 12px; right: 304px; background: #FEF2F2; border: 1px solid #FECACA; color: #991B1B; border-radius: 12px; padding: 12px 16px; font-size: 13px; z-index: 6; }
    .toast {
      position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
      background: rgba(27,27,35,0.92); color: #fff; padding: 10px 18px;
      border-radius: 12px; font-size: 13px; font-weight: 600; z-index: 1000;
      opacity: 0; pointer-events: none; transition: opacity 0.2s ease, transform 0.2s ease;
      box-shadow: 0 4px 16px rgba(0,0,0,0.25); white-space: nowrap;
    }
    .toast.show { opacity: 1; transform: translateX(-50%) translateY(-6px); }
  </style>
</head>
<body>
<div id="map"></div>
<div class="toast" id="toast"></div>
<script src="https://maps.googleapis.com/maps/api/js?key=__GOOGLE_MAPS_API_KEY__&loading=async&callback=initMap" async defer></script>
<script>
  var map, drawingManager;
  // Google Maps shimlari — Leaflet LayerGroup API'siga mos (qolgan kod o'zgarmaydi).
  function makeLayerGroup() {
    return { _items: [],
      addLayer: function(l) { l.setMap(map); if (this._items.indexOf(l) < 0) this._items.push(l); },
      removeLayer: function(l) { l.setMap(null); var i = this._items.indexOf(l); if (i >= 0) this._items.splice(i, 1); },
      clearLayers: function() { this._items.forEach(function(l) { l.setMap(null); }); this._items = []; },
      eachLayer: function(fn) { this._items.slice().forEach(fn); },
      getBounds: function() { var b = new google.maps.LatLngBounds(); this._items.forEach(function(l) { l.getPath().forEach(function(p) { b.extend(p); }); }); return b; }
    };
  }
  var polygonLayer = null, markerLayer = null, zoneMeta = {};
  var tempIdCounter = 0, selectedPolygon = null, currentMode = null, deletedZoneIds = [];
  var drawing = false, drawerBusy = false, setLocationClickHandle = null, setLocationClickHandler = null;
  var currentWarehouseId = null, warehouseNames = {}, warehouseMarker = null, savedWhLat = null, savedWhLng = null;
  var mapReady = false, initRetryCount = 0;
  var pendingZonesJson = null, pendingWhListJson = null;
  var COLORS = ['#4648D4','#EF4444','#10B981','#F59E0B','#8B5CF6','#EC4899','#14B8A6','#F97316','#06B6D4','#84CC16'];

  var toastTimer = null;
  function showToast(msg) {
    var el = document.getElementById('toast');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function() { el.classList.remove('show'); }, 2500);
  }

  function initMap() {
    if (typeof google === 'undefined' || typeof google.maps === 'undefined') {
      initRetryCount++;
      if (initRetryCount > 60) {
        console.error("[initMap] Google Maps yuklanmadi (kalit/internetni tekshiring)");
        showMapError("Google Maps yuklanmadi — API kalit va internetni tekshiring.");
        return;
      }
      setTimeout(initMap, 1000);
      return;
    }
    if (mapReady) return;
    try {
      map = new google.maps.Map(document.getElementById('map'), {
        center: { lat: 41.377491, lng: 64.585262 },
        zoom: 6,
        streetViewControl: false,
        fullscreenControl: false,
        // JCEF (embedded brauzer) da WebGL vektor titrash beradi — raster rejim barqaror.
        renderingType: (google.maps.RenderingType && google.maps.RenderingType.RASTER) || undefined,
        mapTypeControlOptions: { position: google.maps.ControlPosition.TOP_LEFT }
      });
      polygonLayer = makeLayerGroup();
      markerLayer = makeLayerGroup();
      // Maxsus chizish rejimi (Google DrawingManager v3.65+ da o'chirilgan —
      // bosish-bilan-nuqtа qo'shish, double-click bilan yakunlash).

      map.addListener('click', function() { deselectPolygon(); });

      addEditPanel();
      mapReady = true;
      console.log("[initMap] map ready");
      if (pendingZonesJson) { var z = pendingZonesJson; pendingZonesJson = null; applyZones(z); }
      if (pendingWhListJson) { var w = pendingWhListJson; pendingWhListJson = null; applyWarehouseList(w); }
      notifyPageReady();
    } catch (ex) {
      console.error("[initMap] error:", ex);
      initRetryCount++;
      if (initRetryCount <= 60) setTimeout(initMap, 1000);
    }
  }

  function showMapError(msg) {
    if (document.getElementById('mapError')) return;
    var d = document.createElement('div');
    d.id = 'mapError';
    d.className = 'map-error';
    d.textContent = msg;
    document.body.appendChild(d);
  }

  // Chizish tugagach (DrawingManager overlaycomplete) — Leaflet draw:created o'rniga.
  function onDrawCreated(overlay) {
    try {
      if (drawerBusy) { overlay.setMap(null); return; }
      drawerBusy = true;
      overlay.zoneId = 'new_' + (++tempIdCounter);
      applyPolyStyle(overlay, { color: '#4648D4', weight: 3, opacity: 1.0, fillOpacity: 0.15 });
      attachPolyClick(overlay);
      polygonLayer.addLayer(overlay);
      updateStatus("Yangi hudud chizildi");
      currentMode = null;
      document.querySelectorAll('.tool-btn').forEach(function(b) { b.classList.remove('active'); });
    } catch(ex) { console.error("[onDrawCreated] error:", ex); }
    setTimeout(function() { drawerBusy = false; }, 100);
  }

  function applyPolyStyle(poly, s) {
    var opt = {};
    if (s.color !== undefined) { opt.strokeColor = s.color; opt.fillColor = s.color; }
    if (s.weight !== undefined) opt.strokeWeight = s.weight;
    if (s.opacity !== undefined) opt.strokeOpacity = s.opacity;
    if (s.fillOpacity !== undefined) opt.fillOpacity = s.fillOpacity;
    poly.setOptions(opt);
  }

  // Google API yuklanmasa (kalit xato/yo'q) — 10s dan keyin ogohlantirish.
  setTimeout(function() {
    if (!mapReady && (typeof google === 'undefined' || typeof google.maps === 'undefined')) {
      showMapError("Google Maps yuklanmadi — API kalit va internetni tekshiring.");
    }
  }, 10000);

  function notifyPageReady() {
    if (typeof window.cefQuery === 'undefined') {
      setTimeout(notifyPageReady, 200);
      return;
    }
    window.cefQuery({ request: 'pageReady', onSuccess: function(){}, onFailure: function(){} });
  }

  function addEditPanel() {
    if (document.getElementById('mapPanel')) return;
    (function() {
      var panel = document.createElement('div');
      panel.id = 'mapPanel';
      var div = document.createElement('div');
      div.className = 'edit-panel';
      var swatches = '';
      COLORS.forEach(function(c) { swatches += '<span class="color-swatch" style="background:' + c + '" data-color="' + c + '" onclick="pickColor(\'' + c + '\')"></span>'; });
      div.innerHTML =
        '<div class="panel-title">\uD83D\uDDFA\uFE0F Xarita</div>' +
        '<div class="wh-selector">' +
          '<select id="whSelect" onchange="onWhSelectChange()">' +
            '<option value="">Omborxona tanlang...</option>' +
          '</select>' +
        '</div>' +
        '<div class="tool-row">' +
          '<button class="tool-btn" id="drawBtn" onclick="setMode(\'draw\')"><span class="icon">\u2795</span>Chizish</button>' +
          '<button class="tool-btn" id="editBtn" onclick="setMode(\'edit\')"><span class="icon">\u270F\uFE0F</span>Tahrir</button>' +
          '<button class="tool-btn" id="delBtn" onclick="setMode(\'delete\')"><span class="icon">\uD83D\uDDD1\uFE0F</span>O\u2018chir</button>' +
        '</div>' +
        '<button class="tool-btn location-btn" id="locBtn" onclick="setMode(\'setLocation\')"><span class="icon">\uD83D\uDCCD</span> Joylashni belgilash</button>' +
        '<div class="colors-section" id="colorsSection">' +
          '<div class="section-label">Rang tanlash</div>' +
          '<div class="color-grid">' + swatches + '</div>' +
        '</div>' +
        '<button class="save-btn" onclick="saveZones()"><span>\uD83D\uDCBE</span> Saqlash</button>' +
        '<div class="status-bar" id="statusBar">Poligon chizish uchun \u2795 tugmasini bosing</div>';
      panel.appendChild(div);
      document.body.appendChild(panel);
    })();
  }

  function updateDrawBtnDisabled() {
    var ids = ['drawBtn', 'editBtn', 'delBtn', 'locBtn'];
    ids.forEach(function(id) {
      var btn = document.getElementById(id);
      if (!btn) return;
      if (!currentWarehouseId) {
        btn.classList.add('disabled');
        btn.title = 'Avval omborxona tanlang';
      } else {
        btn.classList.remove('disabled');
        btn.title = '';
      }
    });
    var save = document.querySelector('.save-btn');
    if (save) {
      if (!currentWarehouseId) { save.classList.add('disabled'); save.title = 'Avval omborxona tanlang'; }
      else { save.classList.remove('disabled'); save.title = ''; }
    }
  }

  // Bir xil payload qayta kelsa — xaritani tozalab-qayta chizmaymiz (miltillash sababi).
  var lastZonesJson = null, lastWhListJson = null;

  window.setWarehouseList = function(listJson) {
    if (!mapReady) {
      pendingWhListJson = listJson;
      initMap();
      return;
    }
    if (listJson === lastWhListJson) return;
    lastWhListJson = listJson;
    applyWarehouseList(listJson);
  };

  function applyWarehouseList(listJson) {
    if (!mapReady) { pendingWhListJson = listJson; return; }
    var list = JSON.parse(listJson);
    warehouseNames = {};
    var sel = document.getElementById('whSelect');
    if (!sel) return;
    var html = '<option value="">Omborxona tanlang...</option>';
    list.forEach(function(w) {
      warehouseNames[w.id] = w.name;
      html += '<option value="' + w.id + '">' + w.name + '</option>';
    });
    sel.innerHTML = html;
    if (currentWarehouseId) { sel.value = currentWarehouseId; }
    updateDrawBtnDisabled();
  };

  function clearZones() {
    disableCurrentMode();
    destroyDrawer();
    document.querySelectorAll('.tool-btn').forEach(function(b) { b.classList.remove('active'); });
    selectedPolygon = null; deselectPolygon();
    polygonLayer.clearLayers();
    if (markerLayer) markerLayer.clearLayers();
    zoneMeta = {}; deletedZoneIds = [];
  }

  window.setCurrentWarehouseId = function(id, lat, lng) {
    currentWarehouseId = id;
    savedWhLat = (lat !== undefined) ? lat : null;
    savedWhLng = (lng !== undefined) ? lng : null;
    placeWarehouseMarker();
    var sel = document.getElementById('whSelect');
    if (sel) sel.value = id || '';
    updateDrawBtnDisabled();
  };

  function onWhSelectChange() {
    var sel = document.getElementById('whSelect');
    var id = sel ? sel.value : '';
    currentWarehouseId = id || null;
    savedWhLat = null; savedWhLng = null;
    updateDrawBtnDisabled();
    window.cefQuery({
      request: 'selectWarehouse:' + (id || ''),
      onSuccess: function(){},
      onFailure: function(){}
    });
  }

  function setMode(mode) {
    try {
      if (!currentWarehouseId) {
        disableCurrentMode();
        updateStatus("Avval omborxona tanlang");
        showToast("Avval omborxonani tanlang");
        updateDrawBtnDisabled();
        return;
      }
      if (currentMode === mode) { setMode(null); return; }
      disableCurrentMode();
      if (mode === 'draw' && !currentWarehouseId) {
        updateStatus("Avval omborxonani tanlang!");
        updateDrawBtnDisabled();
        return;
      }
      currentMode = mode;
      document.querySelectorAll('.tool-btn').forEach(function(b) { b.classList.remove('active'); });
      if (mode === 'draw') {
        document.getElementById('drawBtn').classList.add('active');
        destroyDrawer();
        startDrawSession();
        updateStatus("Xaritada nuqtalarni bosing — yakunlash uchun double-click");
      } else if (mode === 'edit') {
        document.getElementById('editBtn').classList.add('active');
        if (selectedPolygon) {
          try { enableEditing(selectedPolygon); } catch(ex) {}
        } else {
          polygonLayer.eachLayer(function(l) { try { enableEditing(l); } catch(ex) {} });
        }
        if (warehouseMarker) {
          try { warehouseMarker.setDraggable(true); } catch(ex) {}
        }
        updateStatus("Poligon uchlari ko\u2018rinadi \u2014 sudrab tahrirlang");
      } else if (mode === 'setLocation') {
        if (warehouseMarker) {
          showToast("Mark allaqachon qo'yilgan");
          updateStatus("Mark allaqachon qo'yilgan");
          currentMode = null;
          return;
        }
        if (!currentWarehouseId) { updateStatus("Avval omborxonani tanlang!"); currentMode = null; return; }
        document.getElementById('locBtn').classList.add('active');
        updateStatus("Xaritada mark qo'yish uchun bosing");
        setLocationClickHandler = function(e) {
          var lat = e.latLng.lat().toFixed(6), lng = e.latLng.lng().toFixed(6);
          savedWhLat = parseFloat(lat); savedWhLng = parseFloat(lng);
          placeWarehouseMarker();
          saveWhPosition(lat, lng);
          currentMode = null;
          document.getElementById('locBtn').classList.remove('active');
          if (setLocationClickHandle) { google.maps.event.removeListener(setLocationClickHandle); setLocationClickHandle = null; }
          setLocationClickHandler = null;
          updateStatus("Mark qo'yildi");
          showToast("Mark qo'yildi");
        };
        setLocationClickHandle = map.addListener('click', setLocationClickHandler);
      } else if (mode === 'delete') {
        document.getElementById('delBtn').classList.add('active');
        polygonLayer.eachLayer(function(l) { l._delListener = l.addListener('click', onDeleteClick); });
        if (warehouseMarker) {
          warehouseMarker._delListener = warehouseMarker.addListener('click', onMarkerDeleteClick);
        }
        updateStatus("O\u2018chirish uchun hudud yoki markni bosing");
      }
    } catch(ex) { console.error("[setMode] error:", ex); }
  }

  function disableCurrentMode() {
    try {
      if (currentMode === 'draw') { destroyDrawer(); }
      if (currentMode === 'edit') {
        polygonLayer.eachLayer(function(l) { try { disableEditing(l); } catch(ex) {} });
        if (warehouseMarker) {
          try { warehouseMarker.setDraggable(false); } catch(ex) {}
        }
      }
      if (currentMode === 'setLocation') {
        if (setLocationClickHandle) { google.maps.event.removeListener(setLocationClickHandle); setLocationClickHandle = null; }
        setLocationClickHandler = null;
      }
      if (currentMode === 'delete') {
        polygonLayer.eachLayer(function(l) { if (l._delListener) { google.maps.event.removeListener(l._delListener); l._delListener = null; } });
        if (warehouseMarker && warehouseMarker._delListener) { google.maps.event.removeListener(warehouseMarker._delListener); warehouseMarker._delListener = null; }
      }
    } catch(ex) { console.error("[disableCurrentMode] error:", ex); }
    currentMode = null;
  }

  var drawer = null;

  // ── Maxsus poligon chizish (DrawingManager o'rniga) ──────────────────────
  // Bir marta bosish = nuqta qo'shish, double-click / Chizish tugmasi = yakunlash.
  var drawPoints = [], drawPreview = null, drawVertexMarkers = [];
  var drawClickHandle = null, drawDblClickHandle = null;

  function startDrawSession() {
    cancelDrawSession();
    map.setOptions({ disableDoubleClickZoom: true, draggableCursor: 'crosshair' });
    drawClickHandle = map.addListener('click', function(e) {
      var p = { lat: e.latLng.lat(), lng: e.latLng.lng() };
      drawPoints.push(p);
      drawVertexMarkers.push(new google.maps.Marker({
        position: p, map: map,
        icon: { path: google.maps.SymbolPath.CIRCLE, scale: 5, fillColor: '#4648D4', fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2 }
      }));
      redrawDrawPreview();
    });
    drawDblClickHandle = map.addListener('dblclick', function() { finishDrawSession(); });
  }

  function redrawDrawPreview() {
    if (drawPreview) { drawPreview.setMap(null); drawPreview = null; }
    if (drawPoints.length < 2) return;
    drawPreview = new google.maps.Polyline({
      path: drawPoints, map: map,
      strokeColor: '#4648D4', strokeWeight: 3, strokeOpacity: 0.9
    });
  }

  function finishDrawSession() {
    var pts = drawPoints.slice();
    cancelDrawSession();
    map.setOptions({ disableDoubleClickZoom: false, draggableCursor: null });
    if (pts.length < 3) {
      if (pts.length > 0) updateStatus("Kamida 3 ta nuqta kerak — bekor qilindi");
      return;
    }
    var poly = new google.maps.Polygon({
      paths: pts,
      strokeColor: '#4648D4', strokeWeight: 3, strokeOpacity: 1.0,
      fillColor: '#4648D4', fillOpacity: 0.15,
      map: map
    });
    onDrawCreated(poly);
  }

  function cancelDrawSession() {
    if (drawClickHandle) { google.maps.event.removeListener(drawClickHandle); drawClickHandle = null; }
    if (drawDblClickHandle) { google.maps.event.removeListener(drawDblClickHandle); drawDblClickHandle = null; }
    if (drawPreview) { drawPreview.setMap(null); drawPreview = null; }
    drawVertexMarkers.forEach(function(m) { m.setMap(null); });
    drawVertexMarkers = [];
    drawPoints = [];
    try { map.setOptions({ disableDoubleClickZoom: false, draggableCursor: null }); } catch (ex) {}
  }

  function destroyDrawer() {
    try {
      // Faol chizish bo'lsa — yakunlashga urinish (nuqta yetarli bo'lsa saqlanadi).
      if (currentMode === 'draw' && drawPoints.length >= 3) { finishDrawSession(); }
      else { cancelDrawSession(); }
      drawer = null;
    } catch(ex) { console.error("[destroyDrawer] error:", ex); }
  }

  function enableEditing(layer) {
    try { layer.setEditable(true); } catch(ex) {}
  }

  function disableEditing(layer) {
    try { layer.setEditable(false); } catch(ex) {}
  }

  function onDeleteClick(e) {
    var layer = this;
    var id = layer.zoneId;
    if (id && !id.startsWith('new_')) {
      if (deletedZoneIds.indexOf(id) < 0) deletedZoneIds.push(id);
    }
    polygonLayer.removeLayer(layer);
    updateStatus("Hudud o\u2018chirildi");
  }

  function onMarkerDeleteClick(e) {
    if (warehouseMarker) {
      warehouseMarker.setMap(null);
      warehouseMarker = null;
      savedWhLat = null; savedWhLng = null;
      window.cefQuery({
        request: 'warehousePositionDeleted',
        onSuccess: function(){},
        onFailure: function(err, msg){}
      });
      updateStatus("Mark o\u2018chirildi");
      showToast("Mark o\u2018chirildi");
    }
  }

  function attachPolyClick(layer) {
    layer.addListener('click', function(e) {
      if (currentMode === 'setLocation') return;
      if (currentMode === 'delete') return;
      selectPolygon(this);
      var id = this.zoneId;
      if (id && !id.startsWith('new_'))
        window.cefQuery({ request: 'zoneClick:' + id, onSuccess: function(){}, onFailure: function(){} });
    });
  }

  function selectPolygon(poly) {
    deselectPolygon();
    selectedPolygon = poly;
    applyPolyStyle(poly, { weight: 5, opacity: 1.0, fillOpacity: 0.3 });
    document.getElementById('colorsSection').classList.add('visible');
    updateStatus("Tanlandi \u2014 rangni o\u2018zgartiring yoki tahrirlang");
  }

  function deselectPolygon() {
    if (selectedPolygon) {
      applyPolyStyle(selectedPolygon, { weight: 3, opacity: 1.0, fillOpacity: 0.15 });
      selectedPolygon = null;
    }
    var cs = document.getElementById('colorsSection');
    if (cs) cs.classList.remove('visible');
  }

  function pickColor(color) {
    if (!selectedPolygon) { updateStatus("Avval hududni tanlang"); return; }
    applyPolyStyle(selectedPolygon, { color: color });
    var id = selectedPolygon.zoneId;
    if (zoneMeta[id]) zoneMeta[id].color = color;
    else zoneMeta[id] = { color: color };
    updateStatus("Rang o\u2018zgartirildi");
  }

  function updateStatus(msg) {
    var el = document.getElementById('statusBar');
    if (el) el.textContent = msg;
  }

  function saveWhPosition(lat, lng) {
    window.cefQuery({
      request: 'warehouseMoved:' + currentWarehouseId + ':' + lat + ':' + lng,
      onSuccess: function(m) {},
      onFailure: function() { updateStatus("Xatolik: joy saqlanmadi"); }
    });
  }

  function placeWarehouseMarker() {
    if (warehouseMarker) { warehouseMarker.setMap(null); warehouseMarker = null; }
    if (!currentWarehouseId) return;
    var lat, lng;
    if (savedWhLat != null && savedWhLng != null) {
      lat = savedWhLat; lng = savedWhLng;
    } else {
      var zones = [];
      polygonLayer.eachLayer(function(l) {
        var z = zoneMeta[l.zoneId];
        if (z && z.centerLat && z.centerLng) zones.push(z);
      });
      if (zones.length === 0) return;
      var latSum = 0, lngSum = 0, count = 0;
      zones.forEach(function(z) { latSum += z.centerLat; lngSum += z.centerLng; count++; });
      if (count === 0) return;
      lat = latSum / count; lng = lngSum / count;
    }
    var whName = warehouseNames[currentWarehouseId] || 'Omborxona';
    warehouseMarker = new google.maps.Marker({
      position: { lat: lat, lng: lng },
      map: map,
      draggable: false,
      title: whName,
      label: { text: 'W', color: 'white', fontWeight: 'bold', fontSize: '13px' }
    });
    warehouseMarker.addListener('dragend', function() {
      var pos = warehouseMarker.getPosition();
      var lat = pos.lat().toFixed(6), lng = pos.lng().toFixed(6);
      savedWhLat = pos.lat(); savedWhLng = pos.lng();
      saveWhPosition(lat, lng);
      updateStatus("Omborxona joyi yangilandi");
    });
  }

  window.setZones = function(zonesJson) {
    console.log("[setZones] called, mapReady=" + mapReady + ", len=" + zonesJson.length);
    if (!mapReady) {
      pendingZonesJson = zonesJson;
      initMap();
      console.log("[setZones] map not ready, queued as pending");
      return;
    }
    if (zonesJson === lastZonesJson) { console.log("[setZones] identical payload, skipped"); return; }
    lastZonesJson = zonesJson;
    applyZones(zonesJson);
  };

  function applyZones(zonesJson) {
    if (!mapReady) { pendingZonesJson = zonesJson; return; }
    clearZones();
    var zones = JSON.parse(zonesJson);
    console.log("[applyZones] parsed", zones.length, "zones");
    zones.forEach(function(z) {
      try {
        var coords = z.polygonCoords.map(function(c) { return { lat: c[1], lng: c[0] }; });
        if (coords.length < 3) return;
        var poly = new google.maps.Polygon({
          paths: coords,
          strokeColor: z.color, strokeWeight: 3, strokeOpacity: 1.0,
          fillColor: z.color, fillOpacity: 0.15,
          map: map
        });
        poly.zoneId = z.id;
        attachPolyClick(poly);
        polygonLayer.addLayer(poly);
        zoneMeta[z.id] = z;
      } catch(e) { console.error("[applyZones] polygon error:", e, z); }
    });
    if (zones.length > 0) {
      var bounds = polygonLayer.getBounds();
      if (!bounds.isEmpty()) map.fitBounds(bounds, 40);
    }
    placeWarehouseMarker();
    updateStatus("" + zones.length + " ta hudud yuklandi");
  }

  function saveZones() {
    try {
    if (!currentWarehouseId) {
      updateStatus("Avval omborxona tanlang");
      showToast("Avval omborxonani tanlang");
      updateDrawBtnDisabled();
      return;
    }
    if (!confirm("Hududlarni saqlashni tasdiqlaysizmi?")) { console.log("[saveZones] cancelled"); return; }
    console.log("[saveZones] confirmed, collecting data...");
    disableCurrentMode();
    destroyDrawer();
    document.querySelectorAll('.tool-btn').forEach(function(b) { b.classList.remove('active'); });
    deselectPolygon();
    var zonesData = [];
    var pending = 0, errors = 0;
    polygonLayer.eachLayer(function(layer) {
      var path = layer.getPath();
      if (!path || path.getLength() === 0) return;
      var coords = path.getArray().map(function(ll) {
        return [parseFloat(ll.lat().toFixed(6)), parseFloat(ll.lng().toFixed(6))];
      });
      var id = layer.zoneId;
      var meta = zoneMeta[id] || {};
      var centerLat = meta.centerLat || coords.reduce(function(s,c){return s+c[0];},0)/coords.length;
      var centerLng = meta.centerLng || coords.reduce(function(s,c){return s+c[1];},0)/coords.length;
      var isNew = id && id.startsWith('new_');
      var body = {
        name: meta.name || 'New Zone',
        region: meta.region || "",
        district: meta.district || "",
        polygon: JSON.stringify(coords),
        centerLat: centerLat.toString(),
        centerLng: centerLng.toString(),
        fee: 0,
        estimatedDays: 1,
        color: meta.color || '#6366F1',
        active: meta.active !== undefined ? meta.active : true,
      };
      if (currentWarehouseId) body.warehouseId = currentWarehouseId;
      zonesData.push({
        id: isNew ? null : id,
        body: JSON.stringify(body)
      });
    });
    var total = zonesData.length + deletedZoneIds.length;
    if (total === 0) { updateStatus("Saqlash uchun hech narsa yo'q"); return; }
    pending = total;
    zonesData.forEach(function(z) {
      var method = z.id ? "PUT" : "POST";
      var url = z.id ? "PUT:" + z.id : "POST:null";
      window.cefQuery({
        request: 'saveZone:' + url + ':' + z.body,
              onSuccess: function(msg) {
            pending--;
            if (pending === 0) { onSaveDone(total, errors); }
          },
        onFailure: function(err, msg) {
          pending--; errors++;
          if (pending === 0) { onSaveDone(total, errors); }
        }
      });
    });
    deletedZoneIds.forEach(function(id) {
      window.cefQuery({
        request: 'saveZone:DELETE:' + id + ':{}',
        onSuccess: function(msg) {
          pending--;
          if (pending === 0) { onSaveDone(total, errors); }
        },
        onFailure: function(err, msg) {
          pending--; errors++;
          if (pending === 0) { onSaveDone(total, errors); }
        }
      });
    });
    function onSaveDone(total, errors) {
      deletedZoneIds = [];
      if (errors === 0) {
        updateStatus("Saqlandi (" + total + " ta operatsiya)");
      } else {
        updateStatus("Xatolik: " + errors + " ta operatsiya bajarilmadi");
      }
    }
    updateStatus("Saqlanmoqda (" + pending + " ta)...");
    } catch(ex) { console.error("[saveZones] error:", ex); updateStatus("Xatolik yuz berdi"); }
  }

  initMap();
</script>
</body>
</html>
"""

@Composable
fun JcefMapView(
    zones: List<MapZoneData>,
    warehouseList: List<WarehouseListData>,
    selectedWarehouseId: String?,
    warehousePositionLat: Double?,
    warehousePositionLng: Double?,
    selectedZoneId: String?,
    onZoneClick: (String) -> Unit,
    onSaveZone: (method: String, id: String, body: String) -> Unit,
    onWarehouseSelected: (warehouseId: String?) -> Unit,
    onWarehouseMoved: (warehouseId: String, lat: Double, lng: Double) -> Unit,
    onWarehousePositionDeleted: (warehouseId: String) -> Unit,
    onReloadZones: () -> Unit,
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val latestOnClick by rememberUpdatedState(onZoneClick)
    val latestOnSaveZone by rememberUpdatedState(onSaveZone)
    val latestReloadZones by rememberUpdatedState(onReloadZones)
    val latestOnWarehouseMoved by rememberUpdatedState(onWarehouseMoved)
    val latestOnWarehousePositionDeleted by rememberUpdatedState(onWarehousePositionDeleted)
    val latestZones by rememberUpdatedState(zones)
    val latestWarehouseList by rememberUpdatedState(warehouseList)
    val latestSelectedWarehouseId by rememberUpdatedState(selectedWarehouseId)
    val latestWarehousePositionLat by rememberUpdatedState(warehousePositionLat)
    val latestWarehousePositionLng by rememberUpdatedState(warehousePositionLng)
    val latestOnWarehouseSelected by rememberUpdatedState(onWarehouseSelected)
    val latestOnReady by rememberUpdatedState(onReady)

    val pageReady = remember { AtomicBoolean(false) }
    var attachedRouter by remember { mutableStateOf<CefMessageRouter?>(null) }
    var browser by remember { mutableStateOf<CefBrowser?>(null) }
    var browserFailed by remember { mutableStateOf(false) }
    var browserAttached by remember { mutableStateOf(false) }

    fun createBrowserWithRouter(): CefBrowser? {
        return try {
            val apiKey = GoogleMapsConfig.apiKey
            if (apiKey.isBlank()) {
                println("[Map] WARN: GOOGLE_MAPS_API_KEY topilmadi (.env ga qo'shing)")
            }
            val html = LEAFLET_HTML.replace("__GOOGLE_MAPS_API_KEY__", apiKey)
            val encoded = Base64.getEncoder().encodeToString(html.toByteArray(Charsets.UTF_8))
            val dataUrl = "data:text/html;base64,$encoded"
            println("[Map] HTML length=${html.length}, DataURL length=${dataUrl.length}")
            val b = JcefManager.createBrowser(dataUrl)
            println("[Map] Browser created: ${b != null}")
            if (b != null) {

                val handler = object : CefMessageRouterHandlerAdapter() {
                    override fun onQuery(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        queryId: Long,
                        request: String?,
                        persistent: Boolean,
                        callback: CefQueryCallback?,
                    ): Boolean {
                        val req = request ?: return false
                        if (req == "pageReady") {
                            pageReady.set(true)
                            SwingUtilities.invokeLater { latestOnReady() }
                            val json = zonesToJson(latestZones)
                            val ljson = warehouseListToJson(latestWarehouseList)
                            browser?.executeJavaScript("window.setZones('$json')", "about:blank", 0)
                            browser?.executeJavaScript("window.setWarehouseList('$ljson')", "about:blank", 0)
                            if (latestSelectedWarehouseId != null) {
                                val lat = latestWarehousePositionLat
                                val lng = latestWarehousePositionLng
                                if (lat != null && lng != null) {
                                    browser?.executeJavaScript("window.setCurrentWarehouseId('${latestSelectedWarehouseId}', $lat, $lng)", "about:blank", 0)
                                } else {
                                    browser?.executeJavaScript("window.setCurrentWarehouseId('${latestSelectedWarehouseId}')", "about:blank", 0)
                                }
                            }
                            callback?.success("ok")
                            return true
                        }
                        if (req.startsWith("zoneClick:")) {
                            val id = req.removePrefix("zoneClick:")
                            SwingUtilities.invokeLater { latestOnClick(id) }
                            callback?.success("ok")
                            return true
                        }
                        if (req.startsWith("saveZone:")) {
                            val payload = req.removePrefix("saveZone:")
                            val methodEnd = payload.indexOf(':')
                            if (methodEnd < 0) return false
                            val method = payload.substring(0, methodEnd)
                            val rest = payload.substring(methodEnd + 1)
                            val idEnd = rest.indexOf(':')
                            if (idEnd < 0) return false
                            val id = rest.substring(0, idEnd)
                            val body = rest.substring(idEnd + 1)
                            SwingUtilities.invokeLater {
                                latestOnSaveZone(method, id, body)
                            }
                            callback?.success("ok")
                            return true
                        }
                        if (req.startsWith("warehouseMoved:")) {
                            val p = req.removePrefix("warehouseMoved:")
                            val parts = p.split(':')
                            if (parts.size >= 3) {
                                val whId = parts[0]
                                val lat = parts[1].toDoubleOrNull() ?: return false
                                val lng = parts[2].toDoubleOrNull() ?: return false
                                SwingUtilities.invokeLater { latestOnWarehouseMoved(whId, lat, lng) }
                            }
                            callback?.success("ok")
                            return true
                        }
                        if (req == "warehousePositionDeleted") {
                            val whId = latestSelectedWarehouseId
                            if (whId != null) {
                                SwingUtilities.invokeLater {
                                    latestOnWarehousePositionDeleted(whId)
                                }
                            }
                            callback?.success("ok")
                            return true
                        }
                        if (req.startsWith("selectWarehouse:")) {
                            val id = req.removePrefix("selectWarehouse:")
                            val whId = if (id.isEmpty()) null else id
                            SwingUtilities.invokeLater { latestOnWarehouseSelected(whId) }
                            callback?.success("ok")
                            return true
                        }
                        if (req == "reloadAfterSave") {
                            SwingUtilities.invokeLater { latestReloadZones() }
                            callback?.success("ok")
                            return true
                        }
                        return false
                    }
                }
                val router = CefMessageRouter.create(handler)
                b.getClient()?.addMessageRouter(router)
                attachedRouter = router
                println("[Map] Router added to client")
            }
            b
        } catch (e: Exception) {
            println("[Map] createBrowserWithRouter FAILED: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    var panelReadyTick by remember { mutableStateOf(0) }
    val hostPanel = remember {
        JPanel(BorderLayout()).apply {
            background = java.awt.Color.WHITE
            // Qat'iy o'lcham BERMASLIGI kerak — aks holda brauzer konteynerdan
            // kichik chizilib atrofi qora qoladi. BorderLayout.CENTER to'ldiradi.
            addComponentListener(object : java.awt.event.ComponentAdapter() {
                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    val comp = try { browser?.uiComponent } catch (_: Exception) { null }
                    println("[Map] resized: panel=${width}x${height} browserComp=${comp?.width}x${comp?.height}")
                    panelReadyTick++
                    revalidate()
                    repaint()
                }
            })
        }
    }

    // Brauzer faqat panel o'lchami tayyor bo'lganda yaratiladi — 0x0 holatda
    // yaratilgan native view keyin o'lcham olmagani uchun qora chekkalar qolardi.
    LaunchedEffect(panelReadyTick) {
        if (browser != null || browserFailed) return@LaunchedEffect
        if (hostPanel.width <= 0 || hostPanel.height <= 0) {
            println("[Map] Panel size not ready yet, waiting for layout")
            return@LaunchedEffect
        }
        println("[Map] LaunchedEffect starting, calling JcefManager.init()")
        JcefManager.init()
        println("[Map] JcefManager.init() done")
        var attempts = 0
        while (browser == null && attempts < 5) {
            attempts++
            println("[Map] Creating browser, attempt $attempts (panel=${hostPanel.width}x${hostPanel.height})")
            browser = createBrowserWithRouter()
            if (browser == null) {
                println("[Map] Browser is null, retrying in 2s")
                delay(2000)
            }
        }
        if (browser != null) {
            println("[Map] Browser created, getting UI component")
            val comp = browser!!.getUIComponent()
            println("[Map] UI component class: ${comp.javaClass.name}, size: ${comp.size}")
            SwingUtilities.invokeLater {
                hostPanel.removeAll()
                hostPanel.add(comp, BorderLayout.CENTER)
                hostPanel.doLayout()
                println("[Map] Component added to hostPanel, count=${hostPanel.componentCount}, panel=${hostPanel.size}, comp=${comp.size}")
                hostPanel.revalidate()
                hostPanel.repaint()
                browserAttached = true
            }
            latestOnReady()
        } else {
            println("[Map] Browser creation FAILED after $attempts attempts")
            browserFailed = true
            SwingUtilities.invokeLater { latestOnReady() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SwingUtilities.invokeLater {
                val routerToRemove = attachedRouter
                if (routerToRemove != null) {
                    browser?.getClient()?.removeMessageRouter(routerToRemove)
                }
                browser?.close(true)
            }
        }
    }

    LaunchedEffect(zones) {
        if (!pageReady.get()) return@LaunchedEffect
        browser?.let { b ->
            val json = zonesToJson(latestZones)
            SwingUtilities.invokeLater {
                try {
                    b.executeJavaScript("window.setZones('$json')", "about:blank", 0)
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(warehouseList) {
        if (!pageReady.get()) return@LaunchedEffect
        val ljson = warehouseListToJson(latestWarehouseList)
        val whId = latestSelectedWarehouseId
        val lat = latestWarehousePositionLat
        val lng = latestWarehousePositionLng
        browser?.let { b ->
            SwingUtilities.invokeLater {
                try {
                    b.executeJavaScript("window.setWarehouseList('$ljson')", "about:blank", 0)
                    if (whId != null) {
                        if (lat != null && lng != null) {
                            b.executeJavaScript("window.setCurrentWarehouseId('$whId', $lat, $lng)", "about:blank", 0)
                        } else {
                            b.executeJavaScript("window.setCurrentWarehouseId('$whId')", "about:blank", 0)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(selectedWarehouseId, warehousePositionLat, warehousePositionLng) {
        if (!pageReady.get()) return@LaunchedEffect
        val whId = latestSelectedWarehouseId
        val lat = latestWarehousePositionLat
        val lng = latestWarehousePositionLng
        browser?.let { b ->
            SwingUtilities.invokeLater {
                try {
                    if (whId != null && lat != null && lng != null) {
                        b.executeJavaScript("window.setCurrentWarehouseId('$whId', $lat, $lng)", "about:blank", 0)
                    } else {
                        b.executeJavaScript("window.setCurrentWarehouseId('${whId ?: ""}')", "about:blank", 0)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = { hostPanel },
            update = { panel ->
                panel.revalidate()
                panel.repaint()
            },
        )
        if (browserFailed) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF8F7FC)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Xarita yuklanmadi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B23))
                    Text("JCEF (brauzer) komponenti ishga tushmadi", fontSize = 12.sp, color = Color(0xFF8A85A0))
                }
            }
        }
    }
}
